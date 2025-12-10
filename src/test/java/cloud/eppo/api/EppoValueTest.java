package cloud.eppo.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class EppoValueTest {
  @Test
  public void testDoubleValue() {
    EppoValue eppoValue = EppoValue.valueOf(123.4567);
    assertEquals(123.4567, eppoValue.doubleValue(), 0.0);
  }

  @Test
  public void testToStringWithStringArray() {
    // Test with multiple values
    List<String> values = Arrays.asList("one", "two", "three");
    EppoValue value = EppoValue.valueOf(values);
    assertEquals("one, two, three", value.toString());

    // Test with single value
    List<String> singleValue = Arrays.asList("solo");
    EppoValue singleValueObj = EppoValue.valueOf(singleValue);
    assertEquals("solo", singleValueObj.toString());

    // Test with empty list
    List<String> emptyList = new ArrayList<>();
    EppoValue emptyValue = EppoValue.valueOf(emptyList);
    assertEquals("", emptyValue.toString());
  }

  @Test
  public void testStringArrayJoining() {
    // Test joining behavior with various arrays
    List<String> values = Arrays.asList("a", "b", "c");
    EppoValue value = EppoValue.valueOf(values);
    assertEquals("a, b, c", value.toString());

    // Test with values containing the delimiter
    List<String> commaValues = Arrays.asList("first,item", "second ,item");
    EppoValue commaValue = EppoValue.valueOf(commaValues);
    assertEquals("first,item, second ,item", commaValue.toString());
  }

  @Test
  public void testToStringConsistencyAcrossTypes() {
    // Verify string representation is consistent across types
    EppoValue boolValue = EppoValue.valueOf(true);
    assertEquals("true", boolValue.toString());

    EppoValue numValue = EppoValue.valueOf(42.5);
    assertEquals("42.5", numValue.toString());

    EppoValue strValue = EppoValue.valueOf("test");
    assertEquals("test", strValue.toString());

    EppoValue nullValue = EppoValue.nullValue();
    assertEquals("", nullValue.toString());

    // String array should now use our custom joiner
    List<String> array = Arrays.asList("test1", "test2");
    EppoValue arrayValue = EppoValue.valueOf(array);
    assertEquals("test1, test2", arrayValue.toString());
  }

  @Test
  public void testUnwrapBoolean() {
    EppoValue boolValue = EppoValue.valueOf(true);
    Boolean result = boolValue.unwrap(VariationType.BOOLEAN);
    assertEquals(Boolean.TRUE, result);

    EppoValue falseValue = EppoValue.valueOf(false);
    Boolean falseResult = falseValue.unwrap(VariationType.BOOLEAN);
    assertEquals(Boolean.FALSE, falseResult);
  }

  @Test
  public void testUnwrapInteger() {
    EppoValue numValue = EppoValue.valueOf(42.0);
    Integer result = numValue.unwrap(VariationType.INTEGER);
    assertEquals(Integer.valueOf(42), result);

    EppoValue negativeValue = EppoValue.valueOf(-17.0);
    Integer negativeResult = negativeValue.unwrap(VariationType.INTEGER);
    assertEquals(Integer.valueOf(-17), negativeResult);
  }

  @Test
  public void testUnwrapNumeric() {
    EppoValue numValue = EppoValue.valueOf(123.456);
    Double result = numValue.unwrap(VariationType.NUMERIC);
    assertEquals(Double.valueOf(123.456), result);

    EppoValue intValue = EppoValue.valueOf(100.0);
    Double intResult = intValue.unwrap(VariationType.NUMERIC);
    assertEquals(Double.valueOf(100.0), intResult);
  }

  @Test
  public void testUnwrapString() {
    EppoValue strValue = EppoValue.valueOf("hello world");
    String result = strValue.unwrap(VariationType.STRING);
    assertEquals("hello world", result);

    EppoValue emptyValue = EppoValue.valueOf("");
    String emptyResult = emptyValue.unwrap(VariationType.STRING);
    assertEquals("", emptyResult);
  }

  @Test
  public void testUnwrapJsonValid() {
    String jsonString = "{\"foo\":\"bar\",\"count\":42}";
    EppoValue jsonValue = EppoValue.valueOf(jsonString);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isObject());
    assertEquals("bar", result.get("foo").asText());
    assertEquals(42, result.get("count").asInt());
  }

  @Test
  public void testUnwrapJsonArray() {
    String jsonArrayString = "[1,2,3,4,5]";
    EppoValue jsonValue = EppoValue.valueOf(jsonArrayString);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isArray());
    assertEquals(5, result.size());
    assertEquals(1, result.get(0).asInt());
    assertEquals(5, result.get(4).asInt());
  }

  @Test
  public void testUnwrapJsonWithSpecialCharacters() {
    String jsonString = "{\"a\":\"kümmert\",\"b\":\"schön\"}";
    EppoValue jsonValue = EppoValue.valueOf(jsonString);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isObject());
    assertEquals("kümmert", result.get("a").asText());
    assertEquals("schön", result.get("b").asText());
  }

  @Test
  public void testUnwrapJsonWithEmojis() {
    String jsonString = "{\"a\":\"🤗\",\"b\":\"🌸\"}";
    EppoValue jsonValue = EppoValue.valueOf(jsonString);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isObject());
    assertEquals("🤗", result.get("a").asText());
    assertEquals("🌸", result.get("b").asText());
  }

  @Test
  public void testUnwrapJsonWithWhitespace() {
    String jsonString = "{ \"key\": \"value\", \"number\": 123 }";
    EppoValue jsonValue = EppoValue.valueOf(jsonString);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isObject());
    assertEquals("value", result.get("key").asText());
    assertEquals(123, result.get("number").asInt());
  }

  @Test
  public void testUnwrapJsonInvalid() {
    String invalidJson = "not valid json {";
    EppoValue jsonValue = EppoValue.valueOf(invalidJson);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertNull(result, "Invalid JSON should return null");
  }

  @Test
  public void testUnwrapJsonEmpty() {
    String emptyJson = "{}";
    EppoValue jsonValue = EppoValue.valueOf(emptyJson);
    JsonNode result = jsonValue.unwrap(VariationType.JSON);

    assertTrue(result.isObject());
    assertEquals(0, result.size());
  }
}
