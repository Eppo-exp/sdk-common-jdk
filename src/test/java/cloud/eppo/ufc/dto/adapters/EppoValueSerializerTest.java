package cloud.eppo.ufc.dto.adapters;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.EppoValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EppoValueSerializerTest {
  private ObjectMapper mapper;

  @BeforeEach
  public void setUp() {
    mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(EppoValue.class, new EppoValueSerializer());
    mapper.registerModule(module);
  }

  @Test
  public void testSerializeBooleanValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(true);

    String json = mapper.writeValueAsString(value);

    assertEquals("true", json);
  }

  @Test
  public void testSerializeBooleanFalseValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(false);

    String json = mapper.writeValueAsString(value);

    assertEquals("false", json);
  }

  @Test
  public void testSerializeNumericValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(3.14);

    String json = mapper.writeValueAsString(value);

    assertEquals("3.14", json);
  }

  @Test
  public void testSerializeIntegerNumericValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(42.0);

    String json = mapper.writeValueAsString(value);

    assertEquals("42.0", json);
  }

  @Test
  public void testSerializeStringValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf("hello");

    String json = mapper.writeValueAsString(value);

    assertEquals("\"hello\"", json);
  }

  @Test
  public void testSerializeEmptyStringValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf("");

    String json = mapper.writeValueAsString(value);

    assertEquals("\"\"", json);
  }

  @Test
  public void testSerializeStringArrayValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(Arrays.asList("a", "b", "c"));

    String json = mapper.writeValueAsString(value);

    assertEquals("[\"a\",\"b\",\"c\"]", json);
  }

  @Test
  public void testSerializeEmptyStringArrayValue() throws JsonProcessingException {
    EppoValue value = EppoValue.valueOf(Arrays.asList());

    String json = mapper.writeValueAsString(value);

    assertEquals("[]", json);
  }

  @Test
  public void testSerializeNullValue() throws JsonProcessingException {
    EppoValue value = EppoValue.nullValue();

    String json = mapper.writeValueAsString(value);

    assertEquals("null", json);
  }
}
