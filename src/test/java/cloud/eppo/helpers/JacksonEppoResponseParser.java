package cloud.eppo.helpers;

import cloud.eppo.Utils;
import cloud.eppo.exception.JsonParsingException;
import cloud.eppo.helpers.dto.adapters.EppoModule;
import cloud.eppo.ufc.dto.BanditParametersResponse;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacksonEppoResponseParser implements Utils.EppoResponseJsonParser {

  private static final Logger log = LoggerFactory.getLogger(JacksonEppoResponseParser.class);
  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule());

  @Override
  public FlagConfigResponse parseFlagConfigResponse(byte[] responseBody)
      throws JsonParsingException {
    try {
      return mapper.readValue(responseBody, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new JsonParsingException(e);
    }
  }

  @Override
  public BanditParametersResponse parseBanditParametersResponse(byte[] responseBody)
      throws JsonParsingException {
    try {
      return mapper.readValue(responseBody, BanditParametersResponse.class);
    } catch (IOException e) {
      throw new JsonParsingException(e);
    }
  }

  public static Date parseUtcISODateNode(JsonNode isoDateStringElement) {
    if (isoDateStringElement == null || isoDateStringElement.isNull()) {
      return null;
    }
    String isoDateString = isoDateStringElement.asText();
    return Utils.parseUtcISODateString(isoDateString);
  }
}
