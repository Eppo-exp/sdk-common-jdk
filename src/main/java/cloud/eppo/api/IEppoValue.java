package cloud.eppo.api;

import cloud.eppo.ufc.dto.EppoValueType;
import java.util.List;

/** Interface for EppoValue allowing downstream SDKs to provide custom implementations. */
public interface IEppoValue {
  boolean booleanValue();

  double doubleValue();

  String stringValue();

  List<String> stringArrayValue();

  boolean isNull();

  boolean isBoolean();

  boolean isNumeric();

  boolean isString();

  boolean isStringArray();

  EppoValueType getType();
}
