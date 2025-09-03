package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum VariationType {
  BOOLEAN("BOOLEAN"),
  INTEGER("INTEGER"),
  NUMERIC("NUMERIC"),
  STRING("STRING"),
  JSON("JSON"),
  ;

  @NotNull public final String value;

  VariationType(@NotNull String value) {
    throwIfNull(value, "value must not be null");
    this.value = value;
  }

  @Nullable
  public static VariationType fromString(@NotNull String value) {
    for (@NotNull final VariationType type : VariationType.values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    return null;
  }
}
