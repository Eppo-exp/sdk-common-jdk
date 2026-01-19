package cloud.eppo.api;

/** Interface for Variation allowing downstream SDKs to provide custom implementations. */
public interface IVariation {
  String getKey();

  IEppoValue getValue();
}
