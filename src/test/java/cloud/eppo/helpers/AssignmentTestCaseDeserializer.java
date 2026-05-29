package cloud.eppo.helpers;

import cloud.eppo.api.AllocationDetails;
import cloud.eppo.api.AllocationEvaluationCode;
import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.api.EvaluationDetails;
import cloud.eppo.api.FlagEvaluationCode;
import cloud.eppo.api.MatchedRule;
import cloud.eppo.api.RuleCondition;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.ufc.dto.adapters.EppoValueDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssignmentTestCaseDeserializer extends StdDeserializer<AssignmentTestCase> {
  private final EppoValueDeserializer eppoValueDeserializer = new EppoValueDeserializer();

  public AssignmentTestCaseDeserializer() {
    super(AssignmentTestCase.class);
  }

  @Override
  public AssignmentTestCase deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode rootNode = parser.getCodec().readTree(parser);
    String flag = rootNode.get("flag").asText();
    VariationType variationType = VariationType.fromString(rootNode.get("variationType").asText());
    TestCaseValue defaultValue = deserializeTestCaseValue(rootNode.get("defaultValue"));
    List<SubjectAssignment> subjects = deserializeSubjectAssignments(rootNode.get("subjects"));
    return new AssignmentTestCase(flag, variationType, defaultValue, subjects);
  }

  private List<SubjectAssignment> deserializeSubjectAssignments(JsonNode jsonNode) {
    List<SubjectAssignment> subjectAssignments = new ArrayList<>();
    if (jsonNode != null && jsonNode.isArray()) {
      for (JsonNode subjectAssignmentNode : jsonNode) {
        String subjectKey = subjectAssignmentNode.get("subjectKey").asText();

        Attributes subjectAttributes = new Attributes();
        JsonNode attributesNode = subjectAssignmentNode.get("subjectAttributes");
        if (attributesNode != null && attributesNode.isObject()) {
          for (Iterator<Map.Entry<String, JsonNode>> it = attributesNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String attributeName = entry.getKey();
            EppoValue attributeValue = eppoValueDeserializer.deserializeNode(entry.getValue());
            subjectAttributes.put(attributeName, attributeValue);
          }
        }

        TestCaseValue assignment =
            deserializeTestCaseValue(subjectAssignmentNode.get("assignment"));

        EvaluationDetails evaluationDetails = null;
        JsonNode evaluationDetailsNode = subjectAssignmentNode.get("evaluationDetails");
        if (evaluationDetailsNode != null && !evaluationDetailsNode.isNull()) {
          evaluationDetails = deserializeEvaluationDetails(evaluationDetailsNode);
        }

        subjectAssignments.add(
            new SubjectAssignment(subjectKey, subjectAttributes, assignment, evaluationDetails));
      }
    }

    return subjectAssignments;
  }

  private EvaluationDetails deserializeEvaluationDetails(JsonNode node) {
    String environmentName = getTextOrNull(node, "environmentName");
    String flagEvaluationCodeStr = getTextOrNull(node, "flagEvaluationCode");
    FlagEvaluationCode flagEvaluationCode = FlagEvaluationCode.fromString(flagEvaluationCodeStr);
    String flagEvaluationDescription = getTextOrNull(node, "flagEvaluationDescription");
    String banditKey = getTextOrNull(node, "banditKey");
    String banditAction = getTextOrNull(node, "banditAction");
    String variationKey = getTextOrNull(node, "variationKey");

    EppoValue variationValue = null;
    if (node.has("variationValue") && !node.get("variationValue").isNull()) {
      JsonNode valueNode = node.get("variationValue");
      if (valueNode.isObject() || valueNode.isArray()) {
        // For JSON objects/arrays, convert to string representation
        variationValue = EppoValue.valueOf(valueNode.toString());
      } else {
        // For primitives, use the deserializer
        variationValue = eppoValueDeserializer.deserializeNode(valueNode);
      }
    }

    MatchedRule matchedRule = null;
    if (node.has("matchedRule") && !node.get("matchedRule").isNull()) {
      matchedRule = deserializeMatchedRule(node.get("matchedRule"));
    }

    AllocationDetails matchedAllocation = null;
    if (node.has("matchedAllocation") && !node.get("matchedAllocation").isNull()) {
      matchedAllocation = deserializeAllocationDetails(node.get("matchedAllocation"));
    }

    List<AllocationDetails> unmatchedAllocations = new ArrayList<>();
    if (node.has("unmatchedAllocations")) {
      JsonNode unmatchedNode = node.get("unmatchedAllocations");
      if (unmatchedNode.isArray()) {
        for (JsonNode allocationNode : unmatchedNode) {
          unmatchedAllocations.add(deserializeAllocationDetails(allocationNode));
        }
      }
    }

    List<AllocationDetails> unevaluatedAllocations = new ArrayList<>();
    if (node.has("unevaluatedAllocations")) {
      JsonNode unevaluatedNode = node.get("unevaluatedAllocations");
      if (unevaluatedNode.isArray()) {
        for (JsonNode allocationNode : unevaluatedNode) {
          unevaluatedAllocations.add(deserializeAllocationDetails(allocationNode));
        }
      }
    }

    return new EvaluationDetails(
        environmentName,
        null, // configFetchedAt - not available in test data
        null, // configPublishedAt - not available in test data
        flagEvaluationCode,
        flagEvaluationDescription,
        banditKey,
        banditAction,
        variationKey,
        variationValue,
        matchedRule,
        matchedAllocation,
        unmatchedAllocations,
        unevaluatedAllocations);
  }

  private MatchedRule deserializeMatchedRule(JsonNode node) {
    Set<RuleCondition> conditions = new HashSet<>();
    if (node.has("conditions")) {
      JsonNode conditionsNode = node.get("conditions");
      if (conditionsNode.isArray()) {
        for (JsonNode conditionNode : conditionsNode) {
          String attribute = conditionNode.get("attribute").asText();
          String operator = conditionNode.get("operator").asText();
          EppoValue value = null;
          if (conditionNode.has("value")) {
            JsonNode valueNode = conditionNode.get("value");
            if (valueNode.isArray()) {
              List<String> arrayValue = new ArrayList<>();
              for (JsonNode item : valueNode) {
                arrayValue.add(item.asText());
              }
              value = EppoValue.valueOf(arrayValue);
            } else if (valueNode.isTextual()) {
              value = EppoValue.valueOf(valueNode.asText());
            } else if (valueNode.isNumber()) {
              value = EppoValue.valueOf(valueNode.asDouble());
            } else if (valueNode.isBoolean()) {
              value = EppoValue.valueOf(valueNode.asBoolean());
            }
          }
          conditions.add(new RuleCondition(attribute, operator, value));
        }
      }
    }
    return new MatchedRule(conditions);
  }

  private AllocationDetails deserializeAllocationDetails(JsonNode node) {
    String key = getTextOrNull(node, "key");
    String allocationEvaluationCodeStr = getTextOrNull(node, "allocationEvaluationCode");
    AllocationEvaluationCode allocationEvaluationCode =
        AllocationEvaluationCode.fromString(allocationEvaluationCodeStr);
    Integer orderPosition = null;
    if (node.has("orderPosition") && !node.get("orderPosition").isNull()) {
      orderPosition = node.get("orderPosition").asInt();
    }
    return new AllocationDetails(key, allocationEvaluationCode, orderPosition);
  }

  private String getTextOrNull(JsonNode node, String fieldName) {
    if (node.has(fieldName) && !node.get(fieldName).isNull()) {
      return node.get(fieldName).asText();
    }
    return null;
  }

  private TestCaseValue deserializeTestCaseValue(JsonNode jsonNode) {
    if (jsonNode != null && (jsonNode.isObject() || jsonNode.isArray())) {
      return TestCaseValue.valueOf(jsonNode);
    } else {
      return TestCaseValue.copyOf(eppoValueDeserializer.deserializeNode(jsonNode));
    }
  }
}
