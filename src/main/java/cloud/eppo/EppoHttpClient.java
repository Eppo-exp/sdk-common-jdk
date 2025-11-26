package cloud.eppo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EppoHttpClient {
  private static final Logger log = LoggerFactory.getLogger(EppoHttpClient.class);

  private final OkHttpClient client;

  private final String baseUrl;
  private final String apiKey;
  private final String sdkName;
  private final String sdkVersion;

  public EppoHttpClient(String baseUrl, String apiKey, String sdkName, String sdkVersion) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
    this.client = buildOkHttpClient();
  }

  private static OkHttpClient buildOkHttpClient() {
    OkHttpClient.Builder builder =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS);

    return builder.build();
  }

  public EppoHttpResponse get(String path) {
    try {
      // Wait and return the async get.
      return getAsync(path, null).get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Config fetch interrupted", e);
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<EppoHttpResponse> getAsync(String path) {
    return getAsync(path, null);
  }

  public CompletableFuture<EppoHttpResponse> getAsync(String path, @Nullable String ifNoneMatch) {
    CompletableFuture<EppoHttpResponse> future = new CompletableFuture<>();
    Request.Builder requestBuilder = buildRequestBuilder(path);

    // Add If-None-Match header if eTag provided
    if (ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
      requestBuilder.header("If-None-Match", ifNoneMatch);
    }

    Request request = requestBuilder.build();

    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                  int statusCode = response.code();
                  String eTag = response.header("ETag");

                  // Handle 304 Not Modified
                  if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    future.complete(new EppoHttpResponse(new byte[0], statusCode, eTag));
                    return;
                  }

                  // Handle 2xx success
                  if (response.isSuccessful() && response.body() != null) {
                    log.debug("Fetch successful");
                    try {
                      byte[] bytes = response.body().bytes();
                      future.complete(new EppoHttpResponse(bytes, statusCode, eTag));
                    } catch (IOException ex) {
                      future.completeExceptionally(
                          new RuntimeException(
                              "Failed to read response from URL " + request.url(), ex));
                    }
                  } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    future.completeExceptionally(new RuntimeException("Invalid API key"));
                  } else {
                    log.debug("Fetch failed with status code: {}", statusCode);
                    future.completeExceptionally(
                        new RuntimeException("Bad response from URL " + request.url()));
                  }
                } finally {
                  response.close();
                }
              }

              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error(
                    "Http request failure: {} {}",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()),
                    e);
                future.completeExceptionally(
                    new RuntimeException("Unable to fetch from URL " + request.url()));
              }
            });
    return future;
  }

  private Request.Builder buildRequestBuilder(String path) {
    HttpUrl httpUrl =
        HttpUrl.parse(baseUrl + path)
            .newBuilder()
            .addQueryParameter("apiKey", apiKey)
            .addQueryParameter("sdkName", sdkName)
            .addQueryParameter("sdkVersion", sdkVersion)
            .build();

    return new Request.Builder().url(httpUrl);
  }
}
