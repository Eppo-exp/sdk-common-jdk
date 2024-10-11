package cloud.eppo.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssignmentTest {

  @Test
  public void testCoarseKey() {
    Assignment assignment =
        new Assignment(
            "featureFlag-allocationKey",
            "featureFlag",
            "allocationKey",
            "variationKey",
            "subjectKey",
            null,
            null,
            null);

    String coarseKey = assignment.getIdentifier();

    assertEquals("subjectKey-featureFlag-variationKey", coarseKey);
  }
}
