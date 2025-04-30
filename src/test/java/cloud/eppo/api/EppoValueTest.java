package cloud.eppo.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
