package cloud.eppo.helpers;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoValueDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

public class BanditTestCaseDeserializer extends StdDeserializer<BanditTestCase> {
  private final EppoValueDeserializer eppoValueDeserializer = new EppoValueDeserializer();

  public BanditTestCaseDeserializer() {
    super(BanditTestCase.class);
  }

  @Override
  public BanditTestCase deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode rootNode = parser.getCodec().readTree(parser);
    String flag = rootNode.get("flag").asText();
    String defaultValue = rootNode.get("defaultValue").asText();
    List<SubjectBanditAssignment> subjects = deserializeSubjectBanditAssignments(rootNode.get("subjects"));
    return new BanditTestCase(flag, defaultValue, subjects);
  }

  private List<SubjectBanditAssignment> deserializeSubjectBanditAssignments(JsonNode jsonNode) {
    List<SubjectBanditAssignment> subjectAssignments = new ArrayList<>();
    if (jsonNode != null && jsonNode.isArray()) {
      for (JsonNode subjectAssignmentNode : jsonNode) {
        String subjectKey = subjectAssignmentNode.get("subjectKey").asText();
        JsonNode attributesNode = subjectAssignmentNode.get("subjectAttributes");
        ContextAttributes attributes = new ContextAttributes();
        if (attributesNode != null && attributesNode.isObject()) {
          Attributes numericAttributes = deserializeAttributes(attributesNode.get("numericAttributes"));
          Attributes categoricalAttributes = deserializeAttributes(attributesNode.get("categoricalAttributes"));
          attributes = new ContextAttributes(numericAttributes, categoricalAttributes);
        }
        Map<String, DiscriminableAttributes> actions = deserializeActions(subjectAssignmentNode.get("actions"));
        JsonNode assignmentNode = subjectAssignmentNode.get("assignment");
        String variationAssignment = assignmentNode.get("variation").asText();
        JsonNode actionAssignmentNode = assignmentNode.get("action");
        String actionAssignment = actionAssignmentNode.isNull() ? null : actionAssignmentNode.asText();
        BanditResult assignment = new BanditResult(variationAssignment, actionAssignment);
        subjectAssignments.add(new SubjectBanditAssignment(subjectKey, attributes, actions, assignment));
      }
    }

    return subjectAssignments;
  }

  private Attributes deserializeAttributes(JsonNode jsonNode) {
    Attributes attributes = new Attributes();
    if (jsonNode != null && jsonNode.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        String attributeName = entry.getKey();
        EppoValue attributeValue = eppoValueDeserializer.deserializeNode(entry.getValue());
        attributes.put(attributeName, attributeValue);
      }
    }
    return attributes;
  }

  private Map<String, DiscriminableAttributes> deserializeActions(JsonNode jsonNode) {
    Map<String, DiscriminableAttributes> actions = new HashMap<>();
    if (jsonNode != null && jsonNode.isArray()) {
      for (JsonNode actionNode : jsonNode) {
        String actionKey = actionNode.get("actionKey").asText();
        Attributes numericAttributes = deserializeAttributes(actionNode.get("numericAttributes"));
        Attributes categoricalAttributes = deserializeAttributes(actionNode.get("categoricalAttributes"));
        ContextAttributes attributes = new ContextAttributes(numericAttributes, categoricalAttributes);
        actions.put(actionKey, attributes);
      }
    }
    return actions;
  }
}