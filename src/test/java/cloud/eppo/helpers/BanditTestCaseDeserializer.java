package cloud.eppo.helpers;

import cloud.eppo.api.*;
import cloud.eppo.ufc.dto.adapters.GsonAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.*;

public class BanditTestCaseDeserializer implements JsonDeserializer<BanditTestCase> {
  private final Gson gson = GsonAdapter.createGson();

  @Override
  public BanditTestCase deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject rootNode = json.getAsJsonObject();
    String flag = rootNode.get("flag").getAsString();
    String defaultValue = rootNode.get("defaultValue").getAsString();
    List<BanditSubjectAssignment> subjects =
        deserializeSubjectBanditAssignments(rootNode.get("subjects"));
    return new BanditTestCase(flag, defaultValue, subjects);
  }

  private List<BanditSubjectAssignment> deserializeSubjectBanditAssignments(
      JsonElement jsonElement) {
    List<BanditSubjectAssignment> subjectAssignments = new ArrayList<>();
    if (jsonElement != null && jsonElement.isJsonArray()) {
      for (JsonElement element : jsonElement.getAsJsonArray()) {
        JsonObject subjectAssignmentNode = element.getAsJsonObject();
        String subjectKey = subjectAssignmentNode.get("subjectKey").getAsString();
        JsonElement attributesNode = subjectAssignmentNode.get("subjectAttributes");
        ContextAttributes attributes = new ContextAttributes();
        if (attributesNode != null && attributesNode.isJsonObject()) {
          Attributes numericAttributes =
              deserializeAttributes(attributesNode.getAsJsonObject().get("numericAttributes"));
          Attributes categoricalAttributes =
              deserializeAttributes(attributesNode.getAsJsonObject().get("categoricalAttributes"));
          attributes = new ContextAttributes(numericAttributes, categoricalAttributes);
        }
        Actions actions = deserializeActions(subjectAssignmentNode.get("actions"));
        JsonObject assignmentNode = subjectAssignmentNode.get("assignment").getAsJsonObject();
        String variationAssignment = assignmentNode.get("variation").getAsString();
        JsonElement actionAssignmentNode = assignmentNode.get("action");
        String actionAssignment =
            actionAssignmentNode.isJsonNull() ? null : actionAssignmentNode.getAsString();
        BanditResult assignment = new BanditResult(variationAssignment, actionAssignment);
        subjectAssignments.add(
            new BanditSubjectAssignment(subjectKey, attributes, actions, assignment));
      }
    }

    return subjectAssignments;
  }

  private Actions deserializeActions(JsonElement jsonElement) {
    BanditActions actions = new BanditActions();
    if (jsonElement != null && jsonElement.isJsonArray()) {
      for (JsonElement element : jsonElement.getAsJsonArray()) {
        JsonObject actionNode = element.getAsJsonObject();
        String actionKey = actionNode.get("actionKey").getAsString();
        Attributes numericAttributes = deserializeAttributes(actionNode.get("numericAttributes"));
        Attributes categoricalAttributes =
            deserializeAttributes(actionNode.get("categoricalAttributes"));
        ContextAttributes attributes =
            new ContextAttributes(numericAttributes, categoricalAttributes);
        actions.put(actionKey, attributes);
      }
    }
    return actions;
  }

  private Attributes deserializeAttributes(JsonElement jsonElement) {
    Attributes attributes = new Attributes();
    if (jsonElement != null && jsonElement.isJsonObject()) {
      JsonObject jsonObject = jsonElement.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
        String attributeName = entry.getKey();
        EppoValue attributeValue = TestUtils.deserializeEppoValue(entry.getValue());
        attributes.put(attributeName, attributeValue);
      }
    }
    return attributes;
  }
}
