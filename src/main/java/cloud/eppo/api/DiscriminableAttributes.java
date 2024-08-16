package cloud.eppo.api;

public interface DiscriminableAttributes {

  Attributes getNumericAttributes();

  Attributes getCategoricalAttributes();

  Attributes getAllAttributes();
}
