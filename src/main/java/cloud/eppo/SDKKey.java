package cloud.eppo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for an SDK key; built from the SDK Key token string, this class extracts encoded fields,
 * such as the customer-specific service gateway subdomain
 */
public class SDKKey {
  private static final Logger log = LoggerFactory.getLogger(BaseEppoClient.class);

  private final String sdkTokenString;
  private final Map<String, String> decodedParams;

  /** @param sdkToken The "SDK Key" string provided by the user. */
  public SDKKey(String sdkToken) {
    this.sdkTokenString = sdkToken;
    this.decodedParams = decodeToken(sdkToken);
  }

  @Override
  public String toString() {
    return "SDKKey{" +
      "sdkTokenString='" + sdkTokenString + '\'' +
      ", decodedParams=" + decodedParams +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SDKKey sdkKey = (SDKKey) o;
    return Objects.equals(sdkTokenString, sdkKey.sdkTokenString) &&
            Objects.equals(decodedParams, sdkKey.decodedParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sdkTokenString, decodedParams);
  }

  private Map<String, String> decodeToken(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        return Collections.emptyMap();
      }

      String payload = parts[1];
      if (payload == null) {
        return Collections.emptyMap();
      }

      String decodedString = Utils.base64Decode(payload);
      final Map<String, String> query_pairs = new HashMap<>();
      final String[] pairs = decodedString.split("&");

      for (String pair : pairs) {
        if (pair.isEmpty()) {
          continue;
        }
        final String[] pairParts = pair.split("=");
        final String key = URLDecoder.decode(pairParts[0], StandardCharsets.UTF_8.name());
        final String value =
            pairParts.length > 1
                ? URLDecoder.decode(pairParts[1], StandardCharsets.UTF_8.name())
                : null;

        query_pairs.put(key, value);
      }
      return query_pairs;

    } catch (Exception e) {
      log.error("[Eppo SDK] error parsing SDK Key {}", token, e);
      return Collections.emptyMap();
    }
  }

  /**
   * Gets the subdomain from the decoded token.
   *
   * @return The subdomain or null if not present
   */
  public String getSubdomain() {
    return decodedParams.get("cs");
  }

  /** Gets the full SDK Key token string. */
  public String getToken() {
    return sdkTokenString;
  }

  /**
   * Checks if the SDK Key had the subdomain encoded.
   *
   * @return true if the token is valid and contains required parameters
   */
  public boolean isValid() {
    return !decodedParams.isEmpty();
  }
}
