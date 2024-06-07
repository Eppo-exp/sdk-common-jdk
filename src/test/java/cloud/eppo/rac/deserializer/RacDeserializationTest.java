package cloud.eppo.rac.deserializer;

import static com.google.common.truth.Truth.assertThat;

import cloud.eppo.rac.dto.*;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class RacDeserializationTest {
  private final ObjectMapper objectMapper =
      new ObjectMapper(
              new JsonFactoryBuilder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build())
          .registerModule(new Jdk8Module())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void testDeserialization() throws JsonProcessingException {
    File mockRacResponse = new File("src/test/resources/rac-experiments-v3.json");
    String jsonString = readResource(mockRacResponse);
    ExperimentConfigurationResponse response =
        objectMapper.readValue(jsonString, ExperimentConfigurationResponse.class);
    assertThat(response.getFlags().keySet().size()).isEqualTo(10);
    ExperimentConfiguration experiment = response.getFlags().get("randomization_algo");
    assertThat(experiment.getSubjectShards()).isEqualTo(10000);
    assertThat(experiment.getAllocations().keySet().size()).isEqualTo(1);
    Allocation allocation = experiment.getAllocations().get("allocation-experiment-1");
    assertThat(allocation.getPercentExposure()).isEqualTo(0.4533);
    assertThat(experiment.getRules().get(0).getAllocationKey())
        .isEqualTo("allocation-experiment-1");
    assertThat(allocation.getVariations().get(0).getName()).isEqualTo("control");
    assertThat(allocation.getVariations().get(0).getAlgorithmType())
        .isEqualTo(AlgorithmType.OVERRIDE);
    assertThat(allocation.getVariations().get(0).getShardRange().getStart()).isEqualTo(0);
    assertThat(allocation.getVariations().get(0).getShardRange().getEnd()).isEqualTo(3333);
    assertThat(allocation.getVariations().get(1).getName()).isEqualTo("red");
    assertThat(allocation.getVariations().get(2).getName()).isEqualTo("green");
    ExperimentConfiguration targetingRulesExp =
        response.getFlags().get("targeting_rules_experiment");
    List<Condition> conditions = targetingRulesExp.getRules().get(0).getConditions();
    assertThat(conditions.get(0).getValue().arrayValue())
        .isEqualTo(Arrays.asList("iOS", "Android"));
  }

  private static String readResource(File mockRacResponse) {
    try {
      return FileUtils.readFileToString(mockRacResponse, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock RAC data: " + e.getMessage(), e);
    }
  }
}
