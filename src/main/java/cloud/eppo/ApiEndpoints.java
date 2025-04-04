package cloud.eppo;

import static cloud.eppo.Constants.DEFAULT_BASE_URL;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for constructing Eppo API base URL. Determines the effective base URL considering
 * baseUrl, subdomain from SDK token, and defaultUrl in that order.
 */
public class ApiEndpoints {

  private final SdkKey sdkToken;
  private final String baseUrl;

  /**
   * Creates a new ApiEndpoints instance.
   *
   * @param sdkKey SDK Key instance for subdomain
   * @param baseUrl Custom base URL (optional)
   */
  public ApiEndpoints(@NotNull SdkKey sdkKey, @Nullable String baseUrl) {
    this.sdkToken = sdkKey;
    this.baseUrl = baseUrl;
  }

  /**
   * Gets the normalized base URL based on the following priority: 1. If baseUrl is provided and not
   * equal to DEFAULT_BASE_URL, use it 2. If the SDK token contains a subdomain, use it with
   * DEFAULT_BASE_URL 3. Otherwise, fall back to DEFAULT_BASE_URL
   *
   * <p>The returned URL will: - Always have a protocol (defaults to https:// if none provided) -
   * Never end with a trailing slash
   *
   * @return The normalized base URL
   */
  public String getBaseUrl() {
    String effectiveUrl;

    if (baseUrl != null && !baseUrl.equals(DEFAULT_BASE_URL)) {
      // This is to prevent forcing the SDK to send requests to the CDN server without a subdomain
      // even when encoded in
      // SDK token.
      effectiveUrl = baseUrl;
    } else if (sdkToken != null && sdkToken.isValid()) {
      String subdomain = sdkToken.getSubdomain();
      if (subdomain != null) {
        String domainPart = DEFAULT_BASE_URL.replaceAll("^(https?://|//)", "");
        effectiveUrl = subdomain + "." + domainPart;
      } else {
        effectiveUrl = DEFAULT_BASE_URL;
      }
    } else {
      effectiveUrl = DEFAULT_BASE_URL;
    }

    // Remove any trailing slashes
    effectiveUrl = effectiveUrl.replaceAll("/+$", "");

    // Add protocol if missing
    if (!effectiveUrl.matches("^(https?://|//).*")) {
      effectiveUrl = "https://" + effectiveUrl;
    }

    return effectiveUrl;
  }
}
