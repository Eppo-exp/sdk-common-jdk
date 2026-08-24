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
      // Defense in depth: valueOf(List) normalizes null to nullValue(), but protected
      // constructors are callable from subclasses and Java deserialization bypasses
      // constructors entirely, so an ARRAY_OF_STRING-typed value with a null payload
      // remains reachable.
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
