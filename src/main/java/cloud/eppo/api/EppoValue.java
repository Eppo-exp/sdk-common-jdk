package cloud.eppo.api;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cloud.eppo.ufc.dto.EppoValueType;
import java.util.List;
import java.util.Objects;

public class EppoValue {
  @NotNull protected final EppoValueType type;
  @Nullable protected final Boolean boolValue;
  @Nullable protected final Double doubleValue;
  @Nullable protected final String stringValue;
  @Nullable protected final List<String> stringArrayValue;

  protected EppoValue() {
    this(
      EppoValueType.NULL,
      null,
      null,
      null,
      null
    );
  }

  protected EppoValue(boolean boolValue) {
    this(
      EppoValueType.BOOLEAN,
      boolValue,
      null,
      null,
      null
    );
  }

  protected EppoValue(double doubleValue) {
    this(
      EppoValueType.NUMBER,
      null,
      doubleValue,
      null,
      null
    );
  }

  protected EppoValue(@NotNull String stringValue) {
    this(
      EppoValueType.STRING,
      null,
      null,
      throwIfNull(stringValue, "stringValue must not be null"),
      null
    );
  }

  protected EppoValue(@NotNull List<String> stringArrayValue) {
    this(
      EppoValueType.ARRAY_OF_STRING,
      null,
      null,
      null,
      throwIfNull(stringArrayValue, "stringArrayValue must not be null")
    );
  }

  private EppoValue(
      @NotNull EppoValueType type,
      @Nullable Boolean boolValue,
      @Nullable Double doubleValue,
      @Nullable String stringValue,
      @Nullable List<String> stringArrayValue) {
    throwIfNull(type, "type must not be null");

    this.type = type;
    this.boolValue = boolValue;
    this.doubleValue = doubleValue;
    this.stringValue = stringValue;
    this.stringArrayValue = stringArrayValue;
  }

  @NotNull
  public static EppoValue nullValue() {
    return new EppoValue();
  }

  @NotNull
  public static EppoValue valueOf(boolean boolValue) {
    return new EppoValue(boolValue);
  }

  @NotNull
  public static EppoValue valueOf(double doubleValue) {
    return new EppoValue(doubleValue);
  }

  @NotNull
  public static EppoValue valueOf(@NotNull String stringValue) {
    return new EppoValue(stringValue);
  }

  @NotNull
  public static EppoValue valueOf(@NotNull List<String> value) {
    return new EppoValue(value);
  }

  public boolean booleanValue() {
    @Nullable final Boolean boolValue = this.boolValue;
    if (boolValue == null) {
      throw new NullPointerException("boolValue is null for type: " + type);
    }
    return boolValue;
  }

  public int intValue() {
    @Nullable final Double doubleValue = this.doubleValue;
    if (doubleValue == null) {
      throw new NullPointerException("doubleValue is null for type: " + type);
    }
    return doubleValue.intValue();
  }

  public double doubleValue() {
    @Nullable final Double doubleValue = this.doubleValue;
    if (doubleValue == null) {
      throw new NullPointerException("doubleValue is null for type: " + type);
    }
    return doubleValue;
  }

  @NotNull
  public String stringValue() {
    @Nullable final String stringValue = this.stringValue;
    if (stringValue == null) {
      throw new NullPointerException("stringValue is null for type: " + type);
    }
    return stringValue;
  }

  public List<String> stringArrayValue() {
    @Nullable final List<String> stringArrayValue = this.stringArrayValue;
    if (stringArrayValue == null) {
      throw new NullPointerException("stringArrayValue is null for type: " + type);
    }
    return stringArrayValue;
  }

  public boolean isNull() {
    return type.isNull();
  }

  public boolean isBoolean() {
    return this.type.isBoolean();
  }

  public boolean isNumeric() {
    return this.type.isNumeric();
  }

  public boolean isString() {
    return this.type.isString();
  }

  public boolean isStringArray() {
    return type.isStringArray();
  }

  @NotNull
  public EppoValueType getType() {
    return type;
  }

  @Override @NotNull
  public String toString() {
    return type.toString(boolValue, doubleValue, stringValue, stringArrayValue);
  }

  @Override
  public boolean equals(@Nullable Object otherObject) {
    if (this == otherObject) {
      return true;
    }
    if (otherObject == null || getClass() != otherObject.getClass()) {
      return false;
    }
    EppoValue otherEppoValue = (EppoValue) otherObject;
    return type == otherEppoValue.type
        && Objects.equals(boolValue, otherEppoValue.boolValue)
        && Objects.equals(doubleValue, otherEppoValue.doubleValue)
        && Objects.equals(stringValue, otherEppoValue.stringValue)
        && Objects.equals(stringArrayValue, otherEppoValue.stringArrayValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, boolValue, doubleValue, stringValue, stringArrayValue);
  }
}
