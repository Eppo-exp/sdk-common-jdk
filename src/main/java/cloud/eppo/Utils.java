package cloud.eppo;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
  private static final SimpleDateFormat isoUtcDateFormat = buildUtcIsoDateFormat();
  private static final Logger log = LoggerFactory.getLogger(Utils.class);

  public static String getMD5Hex(String input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error computing md5 hash", e);
    }
    byte[] messageDigest = md.digest(input.getBytes());
    BigInteger no = new BigInteger(1, messageDigest);
    StringBuilder hashText = new StringBuilder(no.toString(16));
    while (hashText.length() < 32) {
      hashText.insert(0, "0");
    }

    return hashText.toString();
  }

  public static Date parseUtcISODateElement(JsonNode isoDateStringElement) {
    if (isoDateStringElement == null || isoDateStringElement.isNull()) {
      return null;
    }
    String isoDateString = isoDateStringElement.asText();
    Date result = null;
    try {
      result = isoUtcDateFormat.parse(isoDateString);
    } catch (ParseException e) {
      // We expect to fail parsing if the date is base 64 encoded
      // Thus we'll leave the result null for now and try again with the decoded value
    }

    if (result == null) {
      // Date may be encoded
      String decodedIsoDateString = base64Decode(isoDateString);
      try {
        result = isoUtcDateFormat.parse(decodedIsoDateString);
      } catch (ParseException e) {
        log.warn("Date \"{}\" not in ISO date format", isoDateString);
      }
    }

    return result;
  }

  public static String getISODate(Date date) {
    return isoUtcDateFormat.format(date);
  }

  public static String base64Decode(String input) {
    if (input == null) {
      return null;
    }
    byte[] decodedBytes = Base64.decodeBase64(input);
    if (decodedBytes.length == 0 && !input.isEmpty()) {
      throw new RuntimeException(
          "zero byte output from Base64; if not running on Android hardware be sure to use RobolectricTestRunner");
    }
    return new String(decodedBytes);
  }

  private static SimpleDateFormat buildUtcIsoDateFormat() {
    // Note: we don't use DateTimeFormatter.ISO_DATE so that this supports older Android versions
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    return dateFormat;
  }
}
