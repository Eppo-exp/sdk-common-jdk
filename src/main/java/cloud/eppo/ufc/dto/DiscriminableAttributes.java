package cloud.eppo.ufc.dto;

public interface DiscriminableAttributes {

  Attributes getNumericAttributes();

  Attributes getCategoricalAttributes();

  Attributes getAllAttributes();
}
