package cloud.eppo.ufc.dto;

import java.util.HashMap;
import java.util.Map;

public class ContextAttributes implements DiscriminableAttributes {

  // TODO Not final and setters
  private final Attributes numericAttributes;
  private final Attributes categoricalAttributes;

  public ContextAttributes() {
    this(new Attributes(), new Attributes());
  }

  public ContextAttributes(Attributes numericAttributes, Attributes categoricalAttributes) {
    this.numericAttributes = numericAttributes;
    this.categoricalAttributes = categoricalAttributes;
  }

  @Override
  public Attributes getNumericAttributes() {
    return numericAttributes;
  }

  @Override
  public Attributes getCategoricalAttributes() {
    return numericAttributes;
  }

  @Override
  public Attributes getAllAttributes() {
    Map<String, EppoValue> allAttributes = new HashMap<>();
    allAttributes.putAll(numericAttributes);
    allAttributes.putAll(categoricalAttributes);
    return new Attributes(allAttributes);
  }
}
