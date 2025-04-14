package cloud.eppo.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import cloud.eppo.ufc.dto.adapters.EppoModule;

public class JacksonMapper implements Mapper {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());
  @Override
  public MapperNode readTree(byte[] content) throws MapperException {
    try {
      JsonNode jsonNode = mapper.readTree(content);
      return new JacksonMapperNode(jsonNode);
    } catch (IOException e) {
      throw new MapperException(e);
    }
  }

  @Override
  public MapperNode readTree(String content) throws MapperJsonProcessingException {
    try {
      JsonNode jsonNode = mapper.readTree(content);
      return new JacksonMapperNode(jsonNode);
    } catch (JsonProcessingException e) {
      throw new MapperJsonProcessingException(e);
    }
  }

  @Override
  public <T> T readValue(byte[] src, Class<T> valueType) throws IOException {
    try {
      return mapper.readValue(src, valueType);
    } catch (IOException e) {
      throw new MapperException(e);
    }
  }

  @Override
  public byte[] writeValueAsBytes(MapperNode value) throws MapperException {
    try {
      return mapper.writeValueAsBytes(((JacksonMapperNode)value).getJsonNode());
    } catch (IOException e) {
      throw new MapperException(e);
    }
  }
}
