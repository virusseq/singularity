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

package org.cancogenvirusseq.singularity.config.elasticsearch;

import static java.lang.String.format;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration;

@Configuration
@RequiredArgsConstructor
public class ReactiveElasticSearchClientConfig extends AbstractReactiveElasticsearchConfiguration {

  private final ElasticsearchProperties elasticsearchProperties;

  private static final Integer connectTimeout = 15_000;
  private static final Integer socketTimeout = 15_000;

  @Override
  @Bean
  public ReactiveElasticsearchClient reactiveElasticsearchClient() {
    return ReactiveRestClients.create(
        Optional.of(ClientConfiguration.builder())
            .map(
                configBuilder ->
                    configBuilder.connectedTo(
                        format(
                            "%s:%s",
                            elasticsearchProperties.getHost(), elasticsearchProperties.getPort())))
            .map(
                configBuilder ->
                    elasticsearchProperties.getUseHttps()
                        ? configBuilder.usingSsl()
                        : configBuilder)
            .map(
                configBuilder ->
                    elasticsearchProperties.getUseAuthentication()
                        ? configBuilder.withBasicAuth(
                            elasticsearchProperties.getUsername(),
                            elasticsearchProperties.getPassword())
                        : configBuilder)
            .get()
            .withConnectTimeout(connectTimeout)
            .withSocketTimeout(socketTimeout)
            .build());
  }
}
