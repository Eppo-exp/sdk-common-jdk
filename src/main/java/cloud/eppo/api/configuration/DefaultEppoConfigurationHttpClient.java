package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import cloud.eppo.ufc.dto.BanditParametersResponse;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of an HTTP client for fetching Eppo configuration.
 *
 * <p>This client uses OkHttp for HTTP requests and Jackson (with EppoModule) for JSON parsing. It
 * provides async-only methods that return CompletableFuture for non-blocking configuration fetches.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>ETag support via If-None-Match header for conditional requests
 *   <li>10-second timeouts for connect, read, and write operations
 *   <li>Type-safe responses with parsed configuration objects
 *   <li>API key redaction in error messages
 * </ul>
 */
public class DefaultEppoConfigurationHttpClient implements IEppoConfigurationHttpClient {
  private static final Logger log =
      LoggerFactory.getLogger(DefaultEppoConfigurationHttpClient.class);

  private final OkHttpClient okHttpClient;
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  public DefaultEppoConfigurationHttpClient() {
    this.okHttpClient =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
  }

  /**
   * Asynchronously fetches flag configuration from the Eppo API.
   *
   * @param request The configuration request containing URL, API key, and optional ETag
   * @return A CompletableFuture that completes with the configuration response
   */
  @Override
  public CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> fetchFlagConfiguration(
      ConfigurationRequest request) {
    return executeRequest(
        request,
        FlagConfigResponse.class,
        ConfigurationResponse.Flags::success,
        ConfigurationResponse.Flags::notModified,
        ConfigurationResponse.Flags::error);
  }

  /**
   * Asynchronously fetches bandit configuration from the Eppo API.
   *
   * @param request The configuration request containing URL, API key, and optional ETag
   * @return A CompletableFuture that completes with the bandit configuration response
   */
  @Override
  public CompletableFuture<ConfigurationResponse<IBanditParametersResponse>>
      fetchBanditConfiguration(ConfigurationRequest request) {
    return executeRequest(
        request,
        BanditParametersResponse.class,
        ConfigurationResponse.Bandits::success,
        ConfigurationResponse.Bandits::notModified,
        ConfigurationResponse.Bandits::error);
  }

  private <T, R> CompletableFuture<ConfigurationResponse<R>> executeRequest(
      ConfigurationRequest configRequest,
      Class<T> responseClass,
      SuccessFactory<T, R> successFactory,
      NotModifiedFactory<R> notModifiedFactory,
      ErrorFactory<R> errorFactory) {

    return CompletableFuture.supplyAsync(
        () -> {
          Request request = buildOkHttpRequest(configRequest);
          try (Response response = okHttpClient.newCall(request).execute()) {
            return handleResponse(
                response, responseClass, successFactory, notModifiedFactory, errorFactory);
          } catch (IOException e) {
            String redactedUrl = redactApiKey(configRequest.url, configRequest.apiKey);
            log.error("Network error fetching configuration from {}", redactedUrl, e);
            return errorFactory.create(500, "Network error: " + e.getMessage());
          }
        });
  }

  private Request buildOkHttpRequest(ConfigurationRequest configRequest) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(configRequest.url).newBuilder();
    urlBuilder.addQueryParameter("apiKey", configRequest.apiKey);
    urlBuilder.addQueryParameter("sdkName", configRequest.sdkName);
    urlBuilder.addQueryParameter("sdkVersion", configRequest.sdkVersion);

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build()).get();

    if (configRequest.previousETag != null && !configRequest.previousETag.isEmpty()) {
      requestBuilder.addHeader("If-None-Match", configRequest.previousETag);
    }

    return requestBuilder.build();
  }

  private <T, R> ConfigurationResponse<R> handleResponse(
      Response response,
      Class<T> responseClass,
      SuccessFactory<T, R> successFactory,
      NotModifiedFactory<R> notModifiedFactory,
      ErrorFactory<R> errorFactory) {

    int statusCode = response.code();
    String eTag = response.header("ETag");

    if (statusCode == 200) {
      try {
        if (response.body() == null) {
          return errorFactory.create(500, "Empty response body");
        }
        String responseBody = response.body().string();
        T payload = OBJECT_MAPPER.readValue(responseBody, responseClass);
        return successFactory.create(payload, eTag);
      } catch (IOException e) {
        log.error("Failed to parse response body", e);
        return errorFactory.create(500, "JSON parsing error: " + e.getMessage());
      }
    } else if (statusCode == 304) {
      return notModifiedFactory.create(eTag);
    } else if (statusCode == 403) {
      return errorFactory.create(403, "Invalid API key");
    } else {
      String errorMessage = "HTTP " + statusCode;
      try {
        if (response.body() != null) {
          String responseBody = response.body().string();
          if (responseBody != null && !responseBody.isEmpty()) {
            errorMessage += ": " + responseBody;
          }
        }
      } catch (IOException e) {
        log.warn("Failed to read error response body", e);
      }
      return errorFactory.create(statusCode, errorMessage);
    }
  }

  private String redactApiKey(String url, String apiKey) {
    if (url == null || apiKey == null) {
      return url;
    }
    return url.replace(apiKey, "***REDACTED***");
  }

  @FunctionalInterface
  private interface SuccessFactory<T, R> {
    ConfigurationResponse<R> create(T payload, String eTag);
  }

  @FunctionalInterface
  private interface NotModifiedFactory<R> {
    ConfigurationResponse<R> create(String eTag);
  }

  @FunctionalInterface
  private interface ErrorFactory<R> {
    ConfigurationResponse<R> create(int statusCode, String errorMessage);
  }
}
