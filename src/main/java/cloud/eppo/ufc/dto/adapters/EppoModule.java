package cloud.eppo.ufc.dto.adapters;

import cloud.eppo.ufc.dto.EppoValue;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Date;

public class EppoModule {
  public static SimpleModule eppoModule() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(FlagConfigResponse.class, new FlagConfigResponseDeserializer());
    module.addDeserializer(EppoValue.class, new EppoValueDeserializer());
    module.addSerializer(EppoValue.class, new EppoValueSerializer());
    module.addSerializer(Date.class, new DateSerializer());
    return module;
  }
}
