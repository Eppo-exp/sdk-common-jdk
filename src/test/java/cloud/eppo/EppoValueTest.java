package cloud.eppo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cloud.eppo.ufc.dto.EppoValue;
import org.junit.jupiter.api.Test;

public class EppoValueTest {
  @Test
  public void testDoubleValue() {
    EppoValue eppoValue = EppoValue.valueOf(123.4567);
    assertEquals(123.4567, eppoValue.doubleValue(), 0.0);
  }
}
