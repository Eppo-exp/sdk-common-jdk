package cloud.eppo.api;

import cloud.eppo.ufc.dto.VariationType;
import java.util.List;
import java.util.Map;

/** Interface for FlagConfig allowing downstream SDKs to provide custom implementations. */
public interface IFlagConfig {
  String getKey();

  boolean isEnabled();

  int getTotalShards();

  VariationType getVariationType();

  Map<String, ? extends IVariation> getVariations();

  List<? extends IAllocation> getAllocations();
}
