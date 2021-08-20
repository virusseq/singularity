package org.cancogenvirusseq.singularity.components;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Disabled
@Slf4j
public class S3OperationsTest {

  /** Constants - Edit these for running manual tests */
  private static final URI ENDPOINT_URL = URI.create("http://localhost:9000");

  private static final Region REGION = Region.CA_CENTRAL_1;

  private static final String ACCESS_KEY = "minioadmin";
  private static final String SECRET_KEY = "minioadmin";
  private static final String BUCKET = "foo";
  private static final String DOWNLOAD_OBJECT_KEY = "test";
  private static final String UPLOAD_OBJECT_KEY = "test";

  private static S3AsyncClient client;
  private static S3Presigner presigner;

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
            .region(REGION)
            .credentialsProvider(creds)
            .serviceConfiguration(serviceConfiguration);

    client = s3AsyncClientBuilder.endpointOverride(ENDPOINT_URL).build();

    presigner =
        S3Presigner.builder()
            .region(REGION)
            .endpointOverride(ENDPOINT_URL)
            .credentialsProvider(creds)
            .serviceConfiguration(serviceConfiguration)
            .build();
  }

  @Test
  @SneakyThrows
  public void testGetObject() {
    val request = GetObjectRequest.builder().bucket(BUCKET).key(DOWNLOAD_OBJECT_KEY).build();
    log.info(client.getObject(request, Path.of("/tmp/foo")).get().toString());
  }

  @Test
  @SneakyThrows
  public void testListObjects() {
    val request = ListObjectsRequest.builder().bucket(BUCKET).build();
    log.info(client.listObjects(request).get().toString());
  }

  @Test
  @SneakyThrows
  public void testUploadObject() {
    val testFile = this.getClass().getResource("test.tar.gz");
    val testFileContentType = "application/x-gtar";
    val uploadFilePath = Paths.get(testFile.toURI());
    val uploadFileSize = Files.size(uploadFilePath);

    val putObjectRequest =
        PutObjectRequest.builder()
            .bucket(BUCKET)
            .key(UPLOAD_OBJECT_KEY)
            .contentLength(uploadFileSize)
            .contentType(testFileContentType)
            .build();

    val putObjectPresignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putObjectRequest)
            .build();

    val presignedPutObjectRequest = presigner.presignPutObject(putObjectPresignRequest);

    StepVerifier.create(
            HttpClient.create()
                .headers(
                    h -> {
                      h.set(HttpHeaderNames.CONTENT_LENGTH, uploadFileSize);
                      h.set(HttpHeaderNames.CONTENT_TYPE, testFileContentType);
                    })
                .put()
                .uri(presignedPutObjectRequest.url().toURI())
                .send(ByteBufFlux.fromPath(uploadFilePath))
                .response()
                .map(HttpClientResponse::status))
        .expectNext(HttpResponseStatus.OK)
        .verifyComplete();
  }
}
