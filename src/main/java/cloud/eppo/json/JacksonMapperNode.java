package cloud.eppo.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonMapperNode implements MapperNode {
  private final JsonNode jsonNode;

  public JacksonMapperNode(JsonNode jsonNode) {
    this.jsonNode = jsonNode;
  }

  @Override
  public void put(String fieldName, String v) {
    ((ObjectNode) jsonNode).put(fieldName, v);
  }

  public JsonNode getJsonNode() {
    return jsonNode;
  }
}
