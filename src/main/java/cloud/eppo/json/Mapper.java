package cloud.eppo.json;

import java.io.IOException;

public interface Mapper {
  MapperNode readTree(byte[] content) throws MapperException;
  MapperNode readTree(String content) throws MapperJsonProcessingException;
  <T> T readValue(byte[] src, Class<T> valueType) throws IOException;
  byte[] writeValueAsBytes(MapperNode value) throws MapperException;
}
