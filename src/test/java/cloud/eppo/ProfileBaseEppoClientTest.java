package cloud.eppo;

import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import cloud.eppo.ufc.dto.Attributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ProfileBaseEppoClientTest {
  private static final Logger log = LoggerFactory.getLogger(BaseEppoClientTest.class);

  private static final String DUMMY_FLAG_API_KEY = "dummy-flags-api-key"; // Will load flags-v1
  private static final String TEST_HOST =
    "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";

  private static BaseEppoClient eppoClient;
  private static final AssignmentLogger mockAssignmentLogger = mock(AssignmentLogger.class);
  private static final BanditLogger mockBanditLogger = mock(BanditLogger.class);

  @BeforeAll
  public static void initClient() {
    eppoClient =
      new BaseEppoClient(
        DUMMY_FLAG_API_KEY,
        "java",
        "3.0.0",
        TEST_HOST,
        mockAssignmentLogger,
        mockBanditLogger,
        false,
        false);

    eppoClient.loadConfiguration();

    log.info("Test client initialized");
  }

  @Test
  public void testGetStringAssignment() {
    Map<String, AtomicInteger> variationCounts = new HashMap<>();
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("country", "FR");

    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    long startTime = threadBean.getCurrentThreadCpuTime();

    int numIterations = 10000;

    for (int i = 0; i < numIterations; i++) {
      String subjectKey = "subject" + i;
      String assignedVariation = eppoClient.getStringAssignment("new-user-onboarding", subjectKey, subjectAttributes, "default");
      AtomicInteger existingCount = variationCounts.computeIfAbsent(assignedVariation, k -> new AtomicInteger(0));
      existingCount.incrementAndGet();
    }
    long endTime = threadBean.getCurrentThreadCpuTime();
    long elapsedTime = endTime - startTime;

    log.info("Assignment counts: {}", variationCounts);
    log.info("CPU Time: {}", elapsedTime);

    // Assert assignments shook out as expected based the shard ranges
    assertEquals(4, variationCounts.keySet().size());
    // Expect ~40% default
    assertEquals(0.40, variationCounts.get("default").doubleValue() / numIterations, 0.02);
    // Expect ~30% control (50% of 60%)
    assertEquals(0.30, variationCounts.get("control").doubleValue() / numIterations, 0.02);
    // Expect ~18% red (30% of 60%)
    assertEquals(0.18, variationCounts.get("red").doubleValue() / numIterations, 0.02);
    // Expect ~12% yellow (20% of 60%)
    assertEquals(0.12, variationCounts.get("yellow").doubleValue() / numIterations, 0.02);

    // Seeing ~150,000,000 - 188,000,000 for 10k iterations
    long maxTotalTime = 20000 * numIterations;
    assertTrue(elapsedTime < maxTotalTime);
  }
}
