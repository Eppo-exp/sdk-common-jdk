package cloud.eppo;

import cloud.eppo.http.EppoHttpClient;
import cloud.eppo.http.EppoHttpException;
import cloud.eppo.http.EppoHttpRequest;
import cloud.eppo.http.EppoHttpResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OkHttp-based implementation of {@link EppoHttpClient}.
 *
 * <p>This implementation handles HTTP communication with Eppo's servers using OkHttp. It supports
 * ETag-based conditional requests for efficient caching.
 */
public class OkHttpEppoClient implements EppoHttpClient {
  private static final Logger log = LoggerFactory.getLogger(OkHttpEppoClient.class);

  private final OkHttpClient client;

  private final String baseUrl;
  private final String apiKey;
  private final String sdkName;
  private final String sdkVersion;

  public OkHttpEppoClient(String baseUrl, String apiKey, String sdkName, String sdkVersion) {
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

  @Override
  public CompletableFuture<EppoHttpResponse> get(EppoHttpRequest request) {
    CompletableFuture<EppoHttpResponse> future = new CompletableFuture<>();
    Request okHttpRequest = buildOkHttpRequest(request);
    client
        .newCall(okHttpRequest)
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                  int statusCode = response.code();
                  String etag = response.header("ETag");

                  // Handle 304 Not Modified
                  if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    log.debug("Received 304 Not Modified");
                    future.complete(EppoHttpResponse.notModified(etag));
                    return;
                  }

                  if (response.isSuccessful() && response.body() != null) {
                    log.debug("Fetch successful");
                    byte[] body = response.body().bytes();
                    future.complete(EppoHttpResponse.success(statusCode, etag, body));
                  } else {
                    if (statusCode == HttpURLConnection.HTTP_FORBIDDEN
                        || statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                      log.error("Invalid API key");
                      future.completeExceptionally(EppoHttpException.unauthorized());
                    } else {
                      log.debug("Fetch failed with status code: {}", statusCode);
                      byte[] errorBody = response.body() != null ? response.body().bytes() : null;
                      future.complete(EppoHttpResponse.error(statusCode, errorBody));
                    }
                  }
                } catch (IOException ex) {
                  future.completeExceptionally(
                      EppoHttpException.networkError(
                          "Failed to read response from URL " + redactApiKey(okHttpRequest.url()),
                          ex));
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
                    EppoHttpException.networkError(
                        "Unable to fetch from URL " + redactApiKey(okHttpRequest.url()), e));
              }
            });
    return future;
  }

  private Request buildOkHttpRequest(EppoHttpRequest request) {
    // Build URL with path and query parameters
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + request.getUrl()).newBuilder();

    // Add SDK-specific query parameters
    urlBuilder.addQueryParameter("apiKey", apiKey);
    urlBuilder.addQueryParameter("sdkName", sdkName);
    urlBuilder.addQueryParameter("sdkVersion", sdkVersion);

    // Add custom query parameters from the request
    for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
      urlBuilder.addQueryParameter(param.getKey(), param.getValue());
    }

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());

    // Add If-None-Match header for conditional requests
    if (request.getIfNoneMatchEtag() != null) {
      requestBuilder.header("If-None-Match", request.getIfNoneMatchEtag());
    }

    return requestBuilder.build();
  }

  private String redactApiKey(HttpUrl url) {
    return url.toString().replaceAll("apiKey=[^&]*", "apiKey=<redacted>");
  }
}
