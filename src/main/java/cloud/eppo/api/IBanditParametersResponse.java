package cloud.eppo.api;

import java.util.Map;

/**
 * Interface for BanditParametersResponse allowing downstream SDKs to provide custom implementations.
 */
public interface IBanditParametersResponse {
  Map<String, ? extends IBanditParameters> getBandits();
}
