package cloud.eppo.ufc.dto.adapters;

import cloud.eppo.api.EppoValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;

public class EppoValueSerializer extends StdSerializer<EppoValue> {
  protected EppoValueSerializer(Class<EppoValue> t) {
    super(t);
  }

  public EppoValueSerializer() {
    this(null);
  }

  @Override
  public void serialize(EppoValue src, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    if (src.isBoolean()) {
      jgen.writeBoolean(src.booleanValue());
    } else if (src.isNumeric()) {
      jgen.writeNumber(src.doubleValue());
    } else if (src.isString()) {
      jgen.writeString(src.stringValue());
    } else if (src.isStringArray()) {
      List<String> list = src.stringArrayValue();
      // Defense in depth: constructors normalize a null payload to NULL type, but Java
      // deserialization bypasses constructors, so an ARRAY_OF_STRING-typed value with a
      // null payload can still arrive from a Configuration serialized by a pre-fix SDK.
      if (list == null) {
        jgen.writeNull();
      } else {
        String[] arr = list.toArray(new String[0]);
        jgen.writeArray(arr, 0, arr.length);
      }
    } else {
      jgen.writeNull();
    }
  }
}
