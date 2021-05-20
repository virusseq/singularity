/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cancogenvirusseq.all.components;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.all.components.model.ScoreFileSpec;
import org.cancogenvirusseq.all.components.model.ScoreServerException;
import org.cancogenvirusseq.all.components.model.ServerErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import static java.lang.String.format;

@Slf4j
@Component
public class ScoreClient {
  private final WebClient scoreClient;
  private final RetryBackoffSpec clientsRetrySpec;

  private static final String RESOURCE_ID_HEADER = "X-Resource-ID";
  private static final String OAUTH_RESOURCE_ID = "songScoreOauth";

  @Autowired
  public ScoreClient(
      @Value("${scoreClient.scoreRootUrl}") String scoreRootUrl,
      @Value("${scoreClient.tokenUrl}") String tokenUrl,
      @Value("${scoreClient.clientId}") String clientId,
      @Value("${scoreClient.clientSecret}") String clientSecret,
      @Value("${scoreClient.retryMaxAttempts}") Integer retryMaxAttempts,
      @Value("${scoreClient.retryDelaySec}") Integer retryDelaySec) {

    this.scoreClient =
        WebClient.builder()
            .baseUrl(scoreRootUrl)
            .filter(createOauthFilter(OAUTH_RESOURCE_ID, tokenUrl, clientId, clientSecret))
            .defaultHeader(RESOURCE_ID_HEADER, OAUTH_RESOURCE_ID)
            .build();

    this.clientsRetrySpec =
        Retry.fixedDelay(retryMaxAttempts, Duration.ofSeconds(retryDelaySec))
            // Retry on non 5xx errors, 4xx is bad request no point retrying
            .filter(
                t ->
                    t instanceof ScoreServerException
                        && ((ScoreServerException) t).getStatus().is5xxServerError())
            .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> retrySignal.failure()));
  }

  public Flux<DataBuffer> downloadObject(String objectId) {
    return getFileLink(objectId).flatMapMany(this::downloadFromS3);
  }

  private Mono<String> getFileLink(String objectId) {
    return scoreClient
        .get()
        .uri(format("/download/%s?offset=0&length=-1&external=true", objectId))
        .exchangeToMono(ofMonoTypeOrHandleError(ScoreFileSpec.class))
        .map(HttpEntity::getBody)
        // we request length = -1 which returns one file part
        .map(spec -> spec.getParts().get(0).getUrl())
        .log("Download::getFileLink")
        .retryWhen(clientsRetrySpec);
  }

  private Flux<DataBuffer> downloadFromS3(String presignedUrl) {
    return WebClient.create(decodeUrl(presignedUrl))
        .get()
        .exchangeToFlux(ofFluxTypeOrHandleError(DataBuffer.class))
        .log("SongScoreClient::downloadFromS3")
        .retryWhen(clientsRetrySpec);
  }

  private static String decodeUrl(String str) {
    return URLDecoder.decode(str, StandardCharsets.UTF_8);
  }

  private static <V> Function<ClientResponse, Mono<ResponseEntity<V>>> ofMonoTypeOrHandleError(
      Class<V> classType) {
    return clientResponse -> {
      if (clientResponse.statusCode().is4xxClientError()) {
        return clientResponse
            .bodyToMono(ServerErrorResponse.class)
            .flatMap(
                res ->
                    Mono.error(
                        new ScoreServerException(clientResponse.statusCode(), res.getMessage())));
      } else if (clientResponse.statusCode().is5xxServerError()) {
        // 5xx errors return as octet-stream
        return clientResponse
            .bodyToMono(String.class)
            .flatMap(
                res -> {
                  log.error("SongScoreServer 5xx response: {}", res);
                  return Mono.error(
                      new ScoreServerException(
                          clientResponse.statusCode(), "SongScore - Internal Server Error"));
                });
      }
      return clientResponse.toEntity(classType);
    };
  }

  private static <V> Function<ClientResponse, Flux<V>> ofFluxTypeOrHandleError(Class<V> classType) {
    return clientResponse -> {
      val status = clientResponse.statusCode();
      if (clientResponse.statusCode().is4xxClientError()) {
        return clientResponse
            .bodyToMono(ServerErrorResponse.class)
            .flux()
            .flatMap(res -> Mono.error(new ScoreServerException(status, res.getMessage())));
      } else if (clientResponse.statusCode().is5xxServerError()) {
        // 5xx errors return as octet-stream
        return clientResponse
            .bodyToMono(String.class)
            .flux()
            .flatMap(
                res ->
                    Mono.error(
                        new ScoreServerException(
                            clientResponse.statusCode(), "SongScore - Internal Server Error")));
      }

      return clientResponse.bodyToFlux(classType);
    };
  }

  private ExchangeFilterFunction createOauthFilter(
      String regId, String tokenUrl, String clientId, String clientSecret) {
    // create client registration with Id for lookup by filter when needed
    val registration =
        ClientRegistration.withRegistrationId(regId)
            .tokenUri(tokenUrl)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    val repo = new InMemoryReactiveClientRegistrationRepository(registration);

    // create new client manager to isolate from server oauth2 manager
    // more info: https://github.com/spring-projects/spring-security/issues/7984
    val authorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(repo);
    val authorizedClientManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            repo, authorizedClientService);
    authorizedClientManager.setAuthorizedClientProvider(
        new ClientCredentialsReactiveOAuth2AuthorizedClientProvider());

    // create filter function
    val oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth.setDefaultClientRegistrationId(regId);
    return oauth;
  }
}
