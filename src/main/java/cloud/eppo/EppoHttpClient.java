package cloud.eppo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

  public byte[] get(String path) {
    Request request = buildRequest(path);

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new RuntimeException("Failed to fetch from " + path);
      }

      return response.body().bytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void get(String path, RequestCallback callback) {
    Request request = buildRequest(path);
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                  log.debug("Fetch successful");
                  try {
                    callback.onSuccess(response.body().bytes());
                  } catch (IOException ex) {
                    callback.onFailure("Failed to read response from URL " + request.url());
                  }
                } else {
                  if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                    callback.onFailure("Invalid API key");
                  } else {
                    log.debug("Fetch failed with status code: {}", response.code());
                    callback.onFailure("Bad response from URL " + request.url());
                  }
                }
                response.close();
              }

              @Override
              public void onFailure(Call call, IOException e) {
                log.error(
                    "Http request failure: {} {}",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()),
                    e);
                callback.onFailure("Unable to fetch from URL " + request.url());
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

  public interface RequestCallback extends EppoCallback<byte[]> {}
}
