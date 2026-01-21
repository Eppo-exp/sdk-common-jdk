package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;

public class ConfigurationResponse<PayloadType> {
  public PayloadType payload;
  public String eTag;
  public int statusCode;
  public String errorMessage;

  private ConfigurationResponse(
      PayloadType payload, String eTag, int statusCode, String errorMessage) {
    this.payload = payload;
    this.eTag = eTag;
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
  }

  private ConfigurationResponse(PayloadType payload, String eTag, int statusCode) {
    this.payload = payload;
    this.eTag = eTag;
    this.statusCode = statusCode;
    this.errorMessage = null;
  }

  public static class Flags {
    public static ConfigurationResponse<IFlagConfigResponse> notModified(String eTag) {
      return new ConfigurationResponse<>(null, eTag, 304);
    }

    public static ConfigurationResponse<IFlagConfigResponse> success(
        IFlagConfigResponse payload, String eTag) {
      return new ConfigurationResponse<>(payload, eTag, 200);
    }

    public static ConfigurationResponse<IFlagConfigResponse> error(
        int statusCode, String errorMessage) {
      return new ConfigurationResponse<>(null, null, statusCode, errorMessage);
    }
  }

  public static class Bandits {
    public static ConfigurationResponse<IBanditParametersResponse> notModified(String eTag) {
      return new ConfigurationResponse<>(null, eTag, 304);
    }

    public static ConfigurationResponse<IBanditParametersResponse> success(
        IBanditParametersResponse payload, String eTag) {
      return new ConfigurationResponse<>(payload, eTag, 200);
    }

    public static ConfigurationResponse<IBanditParametersResponse> error(
        int statusCode, String errorMessage) {
      return new ConfigurationResponse<>(null, null, statusCode, errorMessage);
    }
  }

  public boolean isNotModified() {
    return statusCode == 304;
  }

  public boolean isSuccess() {
    return statusCode == 200;
  }

  public boolean isError() {
    return statusCode != 200 && statusCode != 304;
  }
}
