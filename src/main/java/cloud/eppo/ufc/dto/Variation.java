package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import cloud.eppo.api.EppoValue;

public class Variation {
  @NotNull private final String key;
  @NotNull private final EppoValue value;

  public Variation(@NotNull String key, @NotNull EppoValue value) {
    throwIfNull(key, "key must not be null");
    throwIfNull(value, "value must not be null");

    this.key = key;
    this.value = value;
  }

  @Override @NotNull
  public String toString() {
    return "Variation{" +
      "key='" + key + '\'' +
      ", value=" + value +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Variation variation = (Variation) o;
    return Objects.equals(key, variation.key)
            && Objects.equals(value, variation.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @NotNull
  public String getKey() {
    return this.key;
  }

  @NotNull
  public EppoValue getValue() {
    return value;
  }
}
