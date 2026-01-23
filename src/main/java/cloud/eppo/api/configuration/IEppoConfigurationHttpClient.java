package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import java.util.concurrent.CompletableFuture;

public interface IEppoConfigurationHttpClient {

  CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> fetchFlagConfiguration(
      ConfigurationRequest request);

  CompletableFuture<ConfigurationResponse<IBanditParametersResponse>> fetchBanditConfiguration(
      ConfigurationRequest request);
}
