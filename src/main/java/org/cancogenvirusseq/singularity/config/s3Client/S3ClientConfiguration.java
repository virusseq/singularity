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

package org.cancogenvirusseq.singularity.config.s3Client;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(S3ClientProperties.class)
public class S3ClientConfiguration {
  @Bean
  public S3AsyncClient s3client(
      S3ClientProperties s3props, AwsCredentialsProvider credentialsProvider) {

    SdkAsyncHttpClient httpClient =
        NettyNioAsyncHttpClient.builder().writeTimeout(Duration.ZERO).maxConcurrency(64).build();

    S3Configuration serviceConfiguration =
        S3Configuration.builder()
            .checksumValidationEnabled(false)
            .chunkedEncodingEnabled(true)
            .pathStyleAccessEnabled(true)
            .build();

    S3AsyncClientBuilder s3AsyncClientBuilder =
        S3AsyncClient.builder()
            .httpClient(httpClient)
            .region(s3props.getRegion())
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(serviceConfiguration)
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create())
                    .build());

    if (s3props.getEndpoint() != null) {
      s3AsyncClientBuilder = s3AsyncClientBuilder.endpointOverride(s3props.getEndpoint());
    }
    return s3AsyncClientBuilder.build();
  }

  @Bean
  public AwsCredentialsProvider awsCredentialsProvider(S3ClientProperties s3props) {

    if (StringUtils.isBlank(s3props.getAccessKeyId())) {
      // Return default provider
      return DefaultCredentialsProvider.create();
    } else {
      // Return custom credentials provider
      return () -> {
        AwsCredentials creds =
            AwsBasicCredentials.create(s3props.getAccessKeyId(), s3props.getSecretAccessKey());
        return creds;
      };
    }
  }
}
