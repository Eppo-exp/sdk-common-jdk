package cloud.eppo;

import cloud.eppo.exception.InvalidApiKeyException;
import java.io.IOException;
import java.net.HttpURLConnection;
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

public class EppoHttpClient implements IEppoHttpClient {
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

  @Override
  public byte[] get(String path) {
    Request request = buildRequest(path);
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return response.body().bytes();
      }

      throw response.code() == HttpURLConnection.HTTP_FORBIDDEN
          ? new InvalidApiKeyException("Invalid API key")
          : new HttpException("Fetch failed with status code: " + response.code());

    } catch (IOException e) {
      throw new HttpException("Http request failure", e);
    }
  }

  @Override
  public void getAsync(String path, EppoHttpCallback callback) {
    Request request = buildRequest(path);
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful() && response.body() != null) {
                  try {
                    callback.onSuccess(response.body().bytes());
                  } catch (IOException ex) {
                    callback.onFailure(
                        new HttpException(
                            "Failed to read response from URL {}" + request.url(), ex));
                  }
                } else {
                  callback.onFailure(
                      response.code() == HttpURLConnection.HTTP_FORBIDDEN
                          ? new InvalidApiKeyException("Invalid API key")
                          : new HttpException("Bad response from URL " + request.url()));
                }
                response.close();
              }

              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(e);
              }
            });
  }

  private Request buildRequest(String path) {
    HttpUrl httpUrl =
        HttpUrl.parse(baseUrl + path)
            .newBuilder()
            .addQueryParameter("apiKey", apiKey)
            .addQueryParameter("sdkName", sdkName)
            .addQueryParameter("sdkVersion", sdkVersion)
            .build();

    return new Request.Builder().url(httpUrl).build();
  }
}
