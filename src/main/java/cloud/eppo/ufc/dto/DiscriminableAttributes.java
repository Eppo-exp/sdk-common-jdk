package cloud.eppo.ufc.dto;

public interface DiscriminableAttributes {

  public Attributes getNumericAttributes();

  public Attributes getCategoricalAttributes();

  public Attributes getAllAttributes();
}
