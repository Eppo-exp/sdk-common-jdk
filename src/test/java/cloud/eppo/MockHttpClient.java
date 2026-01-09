package cloud.eppo;

import cloud.eppo.api.Callback;
import cloud.eppo.api.IHttpClient;
import cloud.eppo.exception.FetchException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock HTTP client for testing.
 * Allows setting up mock responses and recording requests.
 */
public class MockHttpClient implements IHttpClient {
  private final Map<String, byte[]> responses = new HashMap<>();
  private final List<MockRequest> requestLog = new ArrayList<>();
  private final String baseUrl;

  public MockHttpClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Set a mock response for a specific endpoint.
   *
   * @param endpoint The endpoint path
   * @param response The response bytes to return
   */
  public void mockResponse(String endpoint, byte[] response) {
    responses.put(endpoint, response);
  }

  /**
   * Set a mock response for a specific endpoint from a string.
   *
   * @param endpoint The endpoint path
   * @param response The response string to return
   */
  public void mockResponse(String endpoint, String response) {
    mockResponse(endpoint, response.getBytes());
  }

  @Override
  public byte[] fetch(String endpoint, Map<String, String> queryParams) throws FetchException {
    logRequest(endpoint, queryParams);
    byte[] response = responses.get(endpoint);
    if (response == null) {
      throw new FetchException("No mock response for: " + endpoint, 404, endpoint);
    }
    return response;
  }

  @Override
  public void fetchAsync(
      String endpoint, Map<String, String> queryParams, Callback<byte[]> callback) {
    logRequest(endpoint, queryParams);
    try {
      byte[] result = fetch(endpoint, queryParams);
      callback.onSuccess(result);
    } catch (FetchException e) {
      callback.onError(e);
    }
  }

  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  private void logRequest(String endpoint, Map<String, String> queryParams) {
    requestLog.add(new MockRequest(endpoint, new HashMap<>(queryParams)));
  }

  /**
   * Get the log of all requests made to this client.
   *
   * @return List of requests in order they were made
   */
  public List<MockRequest> getRequestLog() {
    return new ArrayList<>(requestLog);
  }

  /**
   * Clear the request log.
   */
  public void clearRequestLog() {
    requestLog.clear();
  }

  /**
   * Represents a recorded request.
   */
  public static class MockRequest {
    private final String endpoint;
    private final Map<String, String> queryParams;

    public MockRequest(String endpoint, Map<String, String> queryParams) {
      this.endpoint = endpoint;
      this.queryParams = queryParams;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public Map<String, String> getQueryParams() {
      return queryParams;
    }

    @Override
    public String toString() {
      return "MockRequest{endpoint='" + endpoint + "', queryParams=" + queryParams + '}';
    }
  }
}
