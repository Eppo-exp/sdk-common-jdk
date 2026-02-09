package cloud.eppo;

import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link EppoConfigurationClient} using OkHttp 5.
 *
 * <p>This client handles HTTP communication with Eppo's configuration servers, supporting
 * conditional requests via If-None-Match headers for efficient caching.
 */
public class OkHttpEppoClient implements EppoConfigurationClient {
  private static final Logger log = LoggerFactory.getLogger(OkHttpEppoClient.class);
  private static final String ETAG_HEADER = "ETag";
  private static final String IF_NONE_MATCH_HEADER = "If-None-Match";

  private final OkHttpClient client;

  /** Creates a new OkHttp client with default timeouts (10 seconds for connect and read). */
  public OkHttpEppoClient() {
    this(buildDefaultClient());
  }

  /**
   * Creates a new OkHttp client with a custom OkHttpClient instance.
   *
   * <p>Use this constructor when you need custom timeouts, interceptors, or other OkHttp
   * configurations.
   *
   * @param client the OkHttpClient instance to use
   */
  public OkHttpEppoClient(OkHttpClient client) {
    this.client = client;
  }

  private static OkHttpClient buildDefaultClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
  }

  @Override
  public CompletableFuture<EppoConfigurationResponse> get(EppoConfigurationRequest request) {
    CompletableFuture<EppoConfigurationResponse> future = new CompletableFuture<>();
    Request httpRequest = buildRequest(request);

    client
        .newCall(httpRequest)
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                  EppoConfigurationResponse configResponse = handleResponse(response);
                  future.complete(configResponse);
                } catch (Exception e) {
                  future.completeExceptionally(e);
                } finally {
                  response.close();
                }
              }

              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("HTTP request failed: {}", e.getMessage(), e);
                future.completeExceptionally(
                    new RuntimeException(
                        "Unable to fetch from URL " + redactUrl(httpRequest.url()), e));
              }
            });

    return future;
  }

  private Request buildRequest(EppoConfigurationRequest request) {
    HttpUrl.Builder urlBuilder =
        HttpUrl.parse(request.getBaseUrl() + request.getResourcePath()).newBuilder();

    for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
      urlBuilder.addQueryParameter(param.getKey(), param.getValue());
    }

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());

    // Add conditional request header if we have a previous version
    String lastVersionId = request.getLastVersionId();
    if (lastVersionId != null && !lastVersionId.isEmpty()) {
      requestBuilder.header(IF_NONE_MATCH_HEADER, lastVersionId);
    }

    return requestBuilder.build();
  }

  private EppoConfigurationResponse handleResponse(Response response) throws IOException {
    int statusCode = response.code();
    String versionId = response.header(ETAG_HEADER);

    if (statusCode == 304) {
      log.debug("Configuration not modified (304)");
      return EppoConfigurationResponse.notModified(versionId);
    }

    if (response.isSuccessful()) {
      ResponseBody body = response.body();
      byte[] bodyBytes = body != null ? body.bytes() : new byte[0];
      log.debug("Configuration fetched successfully, {} bytes", bodyBytes.length);
      return EppoConfigurationResponse.success(statusCode, versionId, bodyBytes);
    }

    // Error response
    ResponseBody body = response.body();
    byte[] errorBytes = body != null ? body.bytes() : null;
    log.warn("Configuration fetch failed with status {}", statusCode);
    return EppoConfigurationResponse.error(statusCode, errorBytes);
  }

  private String redactUrl(HttpUrl url) {
    return url.toString().replaceAll("apiKey=[^&]*", "apiKey=<redacted>");
  }
}
