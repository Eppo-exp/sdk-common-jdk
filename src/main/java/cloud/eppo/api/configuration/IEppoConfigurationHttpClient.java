package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import java.util.concurrent.CompletableFuture;

public interface IEppoConfigurationHttpClient {

  <T extends IFlagConfigResponse> ConfigurationResponse<IFlagConfigResponse> fetchFlagConfiguration(
      ConfigurationRequest request);

  <T extends IFlagConfigResponse>
      CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> fetchFlagConfigurationAsync(
          ConfigurationRequest request);

  <T extends IBanditParametersResponse>
      ConfigurationResponse<IBanditParametersResponse> fetchBanditConfiguration(
          ConfigurationRequest request);

  <T extends IBanditParametersResponse>
      CompletableFuture<ConfigurationResponse<IBanditParametersResponse>>
          fetchBanditConfigurationAsync(ConfigurationRequest request);
}
