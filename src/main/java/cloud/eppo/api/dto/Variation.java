package cloud.eppo.api.dto;

import cloud.eppo.api.EppoValue;
import java.util.Objects;

public interface Variation {
  String getKey();

  EppoValue getValue();

  class Default implements Variation {
    private final String key;
    private final EppoValue value;

    public Default(String key, EppoValue value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return "Variation{" + "key='" + key + '\'' + ", value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      Variation variation = (Variation) o;
      return Objects.equals(key, variation.getKey()) && Objects.equals(value, variation.getValue());
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
}
