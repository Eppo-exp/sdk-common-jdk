package cloud.eppo.api;

import java.util.List;

/** Interface for BanditReference allowing downstream SDKs to provide custom implementations. */
public interface IBanditReference {
  String getModelVersion();

  List<? extends IBanditFlagVariation> getFlagVariations();
}
