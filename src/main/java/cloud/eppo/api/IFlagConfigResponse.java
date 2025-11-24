package cloud.eppo.api;

import java.util.Map;

/** Interface for FlagConfigResponse allowing downstream SDKs to provide custom implementations. */
public interface IFlagConfigResponse {
  Map<String, ? extends IFlagConfig> getFlags();

  Map<String, ? extends IBanditReference> getBanditReferences();

  Format getFormat();

  enum Format {
    SERVER,
    CLIENT
  }
}
