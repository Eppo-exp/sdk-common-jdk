package cloud.eppo.helpers;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.ufc.dto.VariationType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignmentTestCaseDeserializer implements JsonDeserializer<AssignmentTestCase> {
  @Override
  public AssignmentTestCase deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject rootNode = json.getAsJsonObject();
    String flag = rootNode.get("flag").getAsString();
    VariationType variationType =
        VariationType.fromString(rootNode.get("variationType").getAsString());
    TestCaseValue defaultValue = deserializeTestCaseValue(rootNode.get("defaultValue"));
    List<SubjectAssignment> subjects = deserializeSubjectAssignments(rootNode.get("subjects"));
    return new AssignmentTestCase(flag, variationType, defaultValue, subjects);
  }

  private List<SubjectAssignment> deserializeSubjectAssignments(JsonElement jsonElement) {
    List<SubjectAssignment> subjectAssignments = new ArrayList<>();
    if (jsonElement != null && jsonElement.isJsonArray()) {
      for (JsonElement subjectAssignmentElement : jsonElement.getAsJsonArray()) {
        JsonObject subjectAssignmentNode = subjectAssignmentElement.getAsJsonObject();
        String subjectKey = subjectAssignmentNode.get("subjectKey").getAsString();

        Attributes subjectAttributes = new Attributes();
        JsonElement attributesNode = subjectAssignmentNode.get("subjectAttributes");
        if (attributesNode != null && attributesNode.isJsonObject()) {
          JsonObject attributesObj = attributesNode.getAsJsonObject();
          for (Map.Entry<String, JsonElement> entry : attributesObj.entrySet()) {
            String attributeName = entry.getKey();
            EppoValue attributeValue = TestUtils.deserializeEppoValue(entry.getValue());
            subjectAttributes.put(attributeName, attributeValue);
          }
        }

        TestCaseValue assignment =
            deserializeTestCaseValue(subjectAssignmentNode.get("assignment"));

        subjectAssignments.add(new SubjectAssignment(subjectKey, subjectAttributes, assignment));
      }
    }

    return subjectAssignments;
  }

  private TestCaseValue deserializeTestCaseValue(JsonElement jsonElement) {
    if (jsonElement != null && (jsonElement.isJsonObject() || jsonElement.isJsonArray())) {
      return TestCaseValue.valueOf(jsonElement);
    } else {
      return TestCaseValue.copyOf(TestUtils.deserializeEppoValue(jsonElement));
    }
  }
}
