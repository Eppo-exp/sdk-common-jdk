package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public enum EppoValueType {
  NULL {
    public boolean isNull() {
      return true;
    }

    public boolean isBoolean() {
      return false;
    }

    public boolean isNumeric() {
      return false;
    }

    public boolean isString() {
      return false;
    }

    public boolean isStringArray() {
      return false;
    }

    @Override @NotNull
    public String toString(
        @Nullable Boolean boolValue,
        @Nullable Double doubleValue,
        @Nullable String stringValue,
        @Nullable List<String> stringArrayValue) {
      return "";
    }
  },
  BOOLEAN {
    public boolean isNull() {
      return false;
    }

    public boolean isBoolean() {
      return true;
    }

    public boolean isNumeric() {
      return false;
    }

    public boolean isString() {
      return false;
    }

    public boolean isStringArray() {
      return false;
    }

    @Override @NotNull
    public String toString(
        @Nullable Boolean boolValue,
        @Nullable Double doubleValue,
        @Nullable String stringValue,
        @Nullable List<String> stringArrayValue) {
      throwIfNull(boolValue, "boolValue must not be null");
      return boolValue.toString();
    }
  },
  NUMBER {
    public boolean isNull() {
      return false;
    }

    public boolean isBoolean() {
      return false;
    }

    public boolean isNumeric() {
      return true;
    }

    public boolean isString() {
      return false;
    }

    public boolean isStringArray() {
      return false;
    }

    @Override @NotNull
    public String toString(
        @Nullable Boolean boolValue,
        @Nullable Double doubleValue,
        @Nullable String stringValue,
        @Nullable List<String> stringArrayValue) {
      throwIfNull(doubleValue, "doubleValue must not be null");
      return doubleValue.toString();
    }
  },
  STRING {
    public boolean isNull() {
      return false;
    }

    public boolean isBoolean() {
      return false;
    }

    public boolean isNumeric() {
      return false;
    }

    public boolean isString() {
      return true;
    }

    public boolean isStringArray() {
      return false;
    }

    @Override @NotNull
    public String toString(
        @Nullable Boolean boolValue,
        @Nullable Double doubleValue,
        @Nullable String stringValue,
        @Nullable List<String> stringArrayValue) {
      throwIfNull(stringValue, "stringValue must not be null");
      return stringValue;
    }
  },
  ARRAY_OF_STRING {
    public boolean isNull() {
      return false;
    }

    public boolean isBoolean() {
      return false;
    }

    public boolean isNumeric() {
      return false;
    }

    public boolean isString() {
      return false;
    }

    public boolean isStringArray() {
      return true;
    }

    @Override @NotNull
    public String toString(
        @Nullable Boolean boolValue,
        @Nullable Double doubleValue,
        @Nullable String stringValue,
        @Nullable List<String> stringArrayValue) {
      throwIfNull(stringArrayValue, "stringArray must not be null");
      return EppoValueType.joinStringArray(stringArrayValue);
    }
  },
  ;

  public abstract boolean isNull();
  public abstract boolean isBoolean();
  public abstract boolean isNumeric();
  public abstract boolean isString();
  public abstract boolean isStringArray();

  @NotNull
  public abstract String toString(
      @Nullable Boolean boolValue,
      @Nullable Double doubleValue,
      @Nullable String stringValue,
      @Nullable List<String> stringArrayValue);

  /** This method is to allow for Android 21 support; String.join was introduced in API 26 */
  @NotNull
  private static String joinStringArray(@NotNull List<String> stringArray) {
    if (stringArray.isEmpty()) {
      return "";
    }
    String delimiter = ", ";
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<String> iterator = stringArray.iterator();
    while (iterator.hasNext()) {
      stringBuilder.append(iterator.next());
      if (iterator.hasNext()) {
        stringBuilder.append(delimiter);
      }
    }
    return stringBuilder.toString();
  }
}
