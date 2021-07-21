package org.cancogenvirusseq.singularity.components;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

@Disabled
@Slf4j
public class S3DownloadTest {

  /** Constants - Edit these for running manual tests */
  private static final String ENDPOINT_URL = "http://localhost:9000";

  private static final String ACCESS_KEY = "admin";
  private static final String SECRET_KEY = "password";
  private static final String BUCKET = "foo";
  private static final String OBJECT_KEY = "test";

  private static S3AsyncClient client;

  @BeforeAll
  public static void setupAwsClient() {
    SdkAsyncHttpClient httpClient =
        NettyNioAsyncHttpClient.builder().writeTimeout(Duration.ZERO).maxConcurrency(64).build();

    S3Configuration serviceConfiguration =
        S3Configuration.builder()
            .checksumValidationEnabled(false)
            .pathStyleAccessEnabled(true)
            .build();

    AwsCredentialsProvider creds = () -> AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);

    S3AsyncClientBuilder s3AsyncClientBuilder =
        S3AsyncClient.builder()
            .httpClient(httpClient)
            .region(Region.CA_CENTRAL_1)
            .credentialsProvider(creds)
            .serviceConfiguration(serviceConfiguration)
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create())
                    .build());

    client = s3AsyncClientBuilder.endpointOverride(URI.create(ENDPOINT_URL)).build();
  }

  @Test
  @SneakyThrows
  public void testGetObject() {
    val request = GetObjectRequest.builder().bucket(BUCKET).key(OBJECT_KEY).build();
    log.info(client.getObject(request, Path.of("/tmp/foo")).get().toString());
  }

  @Test
  @SneakyThrows
  public void testListObjects() {
    val request = ListObjectsRequest.builder().bucket(BUCKET).build();
    log.info(client.listObjects(request).get().toString());
  }
}
