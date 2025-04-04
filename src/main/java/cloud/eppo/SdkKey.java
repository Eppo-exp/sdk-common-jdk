package cloud.eppo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wrapper for an SDK key; built from the SDK Key token string, this class extracts encoded fields,
 * such as the customer-specific service gateway subdomain
 */
public class SdkKey {
  private final String sdkKey;
  private final Map<String, String> decodedParams;

  public SdkKey(String sdkKey) {
    this.sdkKey = sdkKey;
    this.decodedParams = decodeToken(sdkKey);
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
      final Map<String, String> query_pairs = new LinkedHashMap<>();
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
    return sdkKey;
  }

  /**
   * Checks if the SDK Key had the subdomain encoded.
   *
   * @return true if the token is valid and contains required parameters
   */
  public boolean isValid() {
    return !decodedParams.isEmpty() && getSubdomain() != null;
  }
}
