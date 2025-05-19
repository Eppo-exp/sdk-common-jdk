package cloud.eppo.ufc.dto.adapters;

import cloud.eppo.api.EppoValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

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
    switch (src.type) {
      case NULL:
        jgen.writeNull();
        break;
      case BOOLEAN:
        jgen.writeBoolean(src.booleanValue());
        break;
      case NUMBER:
        jgen.writeNumber(src.doubleValue());
        break;
      case STRING:
        jgen.writeString(src.stringValue());
        break;
      case ARRAY_OF_STRING:
        String[] arr = src.stringArrayValue().toArray(new String[0]);
        jgen.writeArray(arr, 0, arr.length);
        break;
    }
  }
}
