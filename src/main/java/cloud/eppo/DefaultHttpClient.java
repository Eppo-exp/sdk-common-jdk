package cloud.eppo;

import cloud.eppo.api.Callback;
import cloud.eppo.api.IHttpClient;
import cloud.eppo.exception.FetchException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default HTTP client implementation using OkHttp3. This is the standard implementation provided by
 * the SDK.
 */
public class DefaultHttpClient implements IHttpClient {
  private static final Logger log = LoggerFactory.getLogger(DefaultHttpClient.class);

  private final OkHttpClient client;
  private final String baseUrl;

  public DefaultHttpClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.client = buildOkHttpClient();
  }

  private static OkHttpClient buildOkHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
  }

  @Override
  public byte[] fetch(String endpoint, Map<String, String> queryParams) throws FetchException {
    Request request = buildRequest(endpoint, queryParams);

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        log.debug("Fetch successful for endpoint: {}", endpoint);
        return response.body().bytes();
      } else {
        int code = response.code();
        String message = code == 403 ? "Invalid API key" : "HTTP " + code;
        throw new FetchException(message, code, endpoint);
      }
    } catch (IOException e) {
      log.error("Network error fetching endpoint: {}", endpoint, e);
      throw new FetchException("Network failure: " + e.getMessage(), e);
    }
  }

  @Override
  public void fetchAsync(
      String endpoint, Map<String, String> queryParams, Callback<byte[]> callback) {
    Request request = buildRequest(endpoint, queryParams);

    client
        .newCall(request)
        .enqueue(
            new okhttp3.Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                  if (response.isSuccessful() && response.body() != null) {
                    log.debug("Async fetch successful for endpoint: {}", endpoint);
                    byte[] bytes = response.body().bytes();
                    callback.onSuccess(bytes);
                  } else {
                    int code = response.code();
                    String message = code == 403 ? "Invalid API key" : "HTTP " + code;
                    callback.onError(new FetchException(message, code, endpoint));
                  }
                } catch (IOException e) {
                  callback.onError(new FetchException("Failed to read response", e));
                } finally {
                  response.close();
                }
              }

              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("Network error fetching endpoint: {}", endpoint, e);
                callback.onError(new FetchException("Network failure: " + e.getMessage(), e));
              }
            });
  }

  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  private Request buildRequest(String endpoint, Map<String, String> queryParams) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();

    // Add all query parameters
    queryParams.forEach(urlBuilder::addQueryParameter);

    return new Request.Builder().url(urlBuilder.build()).build();
  }
}
