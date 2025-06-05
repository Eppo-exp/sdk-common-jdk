package cloud.eppo.ufc.dto;

import java.util.Objects;

import cloud.eppo.api.EppoValue;

public class Variation {
  private final String key;
  private final EppoValue value;

  public Variation(String key, EppoValue value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String toString() {
    return "Variation{" +
      "key='" + key + '\'' +
      ", value=" + value +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Variation variation = (Variation) o;
    return Objects.equals(key, variation.key)
            && Objects.equals(value, variation.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  public String getKey() {
    return this.key;
  }

  public EppoValue getValue() {
    return value;
  }
}
