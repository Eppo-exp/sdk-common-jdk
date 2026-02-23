package cloud.eppo.api.dto;

import cloud.eppo.api.EppoValue;
import java.io.Serializable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface Variation extends Serializable {
  @NotNull String getKey();

  @NotNull EppoValue getValue();

  class Default implements Variation {
    private static final long serialVersionUID = 1L;
    private final @NotNull String key;
    private final @NotNull EppoValue value;

    public Default(@NotNull String key, @NotNull EppoValue value) {
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

    @Override
    @NotNull public String getKey() {
      return this.key;
    }

    @Override
    @NotNull public EppoValue getValue() {
      return value;
    }
  }
}
