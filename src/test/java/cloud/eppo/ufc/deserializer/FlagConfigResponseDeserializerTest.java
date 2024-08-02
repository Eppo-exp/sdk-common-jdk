package cloud.eppo.ufc.deserializer;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FlagConfigResponseDeserializerTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule());

  @Test
  public void testDeserializePlainText() throws IOException {
    File testUfc = new File("src/test/resources/flags-v1.json");
    FileReader fileReader = new FileReader(testUfc);
    FlagConfigResponse configResponse = mapper.readValue(fileReader, FlagConfigResponse.class);

    assertTrue(configResponse.getFlags().size() >= 13);
    assertTrue(configResponse.getFlags().containsKey("empty_flag"));
    assertTrue(configResponse.getFlags().containsKey("disabled_flag"));
    assertTrue(configResponse.getFlags().containsKey("no_allocations_flag"));
    assertTrue(configResponse.getFlags().containsKey("numeric_flag"));
    assertTrue(configResponse.getFlags().containsKey("invalid-value-flag"));
    assertTrue(configResponse.getFlags().containsKey("kill-switch"));
    assertTrue(configResponse.getFlags().containsKey("semver-test"));
    assertTrue(configResponse.getFlags().containsKey("comparator-operator-test"));
    assertTrue(configResponse.getFlags().containsKey("start-and-end-date-test"));
    assertTrue(configResponse.getFlags().containsKey("null-operator-test"));
    assertTrue(configResponse.getFlags().containsKey("new-user-onboarding"));
    assertTrue(configResponse.getFlags().containsKey("integer-flag"));
    assertTrue(configResponse.getFlags().containsKey("json-config-flag"));
    assertTrue(configResponse.getFlags().containsKey("json-array-config-flag"));

    FlagConfig flagConfig = configResponse.getFlags().get("kill-switch");
    assertNotNull(flagConfig);
    assertEquals(flagConfig.getKey(), "kill-switch");
    assertTrue(flagConfig.isEnabled());
    assertEquals(VariationType.BOOLEAN, flagConfig.getVariationType());

    Map<String, Variation> variations = flagConfig.getVariations();
    assertEquals(2, variations.size());
    Variation onVariation = variations.get("on");
    assertNotNull(onVariation);
    assertEquals("on", onVariation.getKey());
    assertTrue(onVariation.getValue().booleanValue());
    Variation offVariation = variations.get("off");
    assertNotNull(offVariation);
    assertEquals("off", offVariation.getKey());
    assertFalse(offVariation.getValue().booleanValue());

    List<Allocation> allocations = flagConfig.getAllocations();
    assertEquals(3, allocations.size());

    Allocation northAmericaAllocation = allocations.get(0);
    assertEquals("on-for-NA", northAmericaAllocation.getKey());
    assertTrue(northAmericaAllocation.doLog());
    assertEquals(1, northAmericaAllocation.getRules().size());
    TargetingCondition northAmericaCondition =
        northAmericaAllocation.getRules().iterator().next().getConditions().iterator().next();
    assertEquals("country", northAmericaCondition.getAttribute());
    assertEquals(OperatorType.ONE_OF, northAmericaCondition.getOperator());
    List<String> expectedValues = new ArrayList<>();
    expectedValues.add("US");
    expectedValues.add("Canada");
    expectedValues.add("Mexico");
    assertEquals(expectedValues, northAmericaCondition.getValue().stringArrayValue());

    assertEquals(1, northAmericaAllocation.getSplits().size());
    Split northAmericaSplit = northAmericaAllocation.getSplits().iterator().next();
    assertEquals("on", northAmericaSplit.getVariationKey());

    Shard northAmericaShard = northAmericaSplit.getShards().iterator().next();
    assertEquals("some-salt", northAmericaShard.getSalt());

    ShardRange northAmericaRange = northAmericaShard.getRanges().iterator().next();
    assertEquals(0, northAmericaRange.getStart());
    assertEquals(10000, northAmericaRange.getEnd());

    Allocation fiftyPlusAllocation = allocations.get(1);
    assertEquals("on-for-age-50+", fiftyPlusAllocation.getKey());
    assertTrue(fiftyPlusAllocation.doLog());
    assertEquals(1, fiftyPlusAllocation.getRules().size());
    TargetingCondition fiftyPlusCondition =
        fiftyPlusAllocation.getRules().iterator().next().getConditions().iterator().next();
    assertEquals("age", fiftyPlusCondition.getAttribute());
    assertEquals(OperatorType.GREATER_THAN_OR_EQUAL_TO, fiftyPlusCondition.getOperator());
    assertEquals(50, fiftyPlusCondition.getValue().doubleValue(), 0.0);

    assertEquals(1, fiftyPlusAllocation.getSplits().size());
    Split fiftyPlusSplit = fiftyPlusAllocation.getSplits().iterator().next();
    assertEquals("on", fiftyPlusSplit.getVariationKey());

    Shard fiftyPlusShard = fiftyPlusSplit.getShards().iterator().next();
    assertEquals("some-salt", fiftyPlusShard.getSalt());

    ShardRange fiftyPlusRange = fiftyPlusShard.getRanges().iterator().next();
    assertEquals(0, fiftyPlusRange.getStart());
    assertEquals(10000, fiftyPlusRange.getEnd());

    Allocation offForAll = allocations.get(2);
    assertEquals("off-for-all", offForAll.getKey());
    assertTrue(offForAll.doLog());
    assertEquals(0, offForAll.getRules().size());

    assertEquals(1, offForAll.getSplits().size());
    Split offForAllSplit = offForAll.getSplits().iterator().next();
    assertEquals("off", offForAllSplit.getVariationKey());
    assertEquals(0, offForAllSplit.getShards().size());

    // test for `json-array-config-flag` flag
    FlagConfig jsonArrayConfigFlag = configResponse.getFlags().get("json-array-config-flag");
    assertNotNull(jsonArrayConfigFlag);
    jsonArrayConfigFlag.getVariations().forEach((key, variation) -> {
      if (key.equals("one")) {
        assertEquals("one", variation.getKey());
        assertEquals("[{ \"integer\": 1, \"string\": \"one\", \"float\": 1.0 }, { \"integer\": 2, \"string\": \"two\", \"float\": 2.0 }]", variation.getValue().stringValue());
      } else if (key.equals("two")) {
        assertEquals("two", variation.getKey());
        assertEquals("[{ \"integer\": 3, \"string\": \"three\", \"float\": 3.0 }, { \"integer\": 4, \"string\": \"four\", \"float\": 4.0 }]", variation.getValue().stringValue());
      } else {
        fail("Unexpected variation key: " + key);
      }
    });
  }
}
