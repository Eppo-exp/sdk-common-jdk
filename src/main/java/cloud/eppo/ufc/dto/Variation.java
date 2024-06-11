package cloud.eppo.ufc.dto;

public class Variation {
  private final String key;
  private final EppoValue value;

  public Variation(String key, EppoValue value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return this.key;
  }

  public EppoValue getValue() {
    return value;
  }
}
