package cloud.eppo;

import com.fasterxml.jackson.databind.JsonNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
  private static final SimpleDateFormat UTC_ISO_DATE_FORMAT = buildUtcIsoDateFormat();
  private static final Logger log = LoggerFactory.getLogger(Utils.class);
  private static final MessageDigest md = buildMd5MessageDigest();
  private static Base64Codec base64Codec;

  public static void setBase64Codec(@NotNull Base64Codec base64Codec) {
    Utils.base64Codec = base64Codec;
  }

  private static MessageDigest buildMd5MessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error computing md5 hash", e);
    }
  }

  public static void throwIfEmptyOrNull(String input, String errorMessage) {
    if (input == null || input.isEmpty()) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Return the String representation of the zero-padded hexadecimal hash of a string input This is
   * useful for comparing against other string hashes, such as obfuscated flag names.
   */
  public static String getMD5Hex(String input) {
    // md5 the input
    md.reset();
    byte[] md5Bytes = md.digest(input.getBytes());
    // Pre-allocate a StringBuilder with a capacity of 32 characters
    StringBuilder hexString = new StringBuilder(32);

    for (byte b : md5Bytes) {
      // Append the two hex digits corresponding to the byte
      hexString.append(Character.forDigit((b >> 4) & 0xF, 16));
      hexString.append(Character.forDigit(b & 0xF, 16));
    }

    return hexString.toString();
  }

  /**
   * Return a deterministic pseudo-random integer based on the input that falls between 0
   * (inclusive) and a max value (exclusive) This is useful for randomly bucketing subjects or
   * shuffling bandit actions
   */
  public static int getShard(String input, int maxShardValue) {
    // md5 the input
    md.reset();
    byte[] md5Bytes = md.digest(input.getBytes());

    // Extract the first 4 bytes (8 digits) and convert to a long
    long value = 0;
    for (int i = 0; i < 4; i++) {
      value = (value << 8) | (md5Bytes[i] & 0xFF);
    }

    // Modulo into the shard space
    return (int) (value % maxShardValue);
  }

  public static Date parseUtcISODateNode(JsonNode isoDateStringElement) {
    if (isoDateStringElement == null || isoDateStringElement.isNull()) {
      return null;
    }
    String isoDateString = isoDateStringElement.asText();
    Date result = null;
    try {
      result = UTC_ISO_DATE_FORMAT.parse(isoDateString);
    } catch (ParseException e) {
      // We expect to fail parsing if the date is base 64 encoded
      // Thus we'll leave the result null for now and try again with the decoded value
    }

    if (result == null) {
      // Date may be encoded
      String decodedIsoDateString = base64Decode(isoDateString);
      try {
        result = UTC_ISO_DATE_FORMAT.parse(decodedIsoDateString);
      } catch (ParseException e) {
        log.warn("Date \"{}\" not in ISO date format", isoDateString);
      }
    }

    return result;
  }

  public static String getISODate(Date date) {
    return UTC_ISO_DATE_FORMAT.format(date);
  }

  /**
   * An implementation of the Base64Codec is required to be set before these methods work. ex:
   * Utils.setBase64Codec(new JavaBase64Codec());
   */
  public static String base64Encode(String input) {
    if (base64Codec == null) {
      throw new RuntimeException("Base64 codec not initialized");
    }
    if (input == null) {
      return null;
    }
    return base64Codec.base64Encode(input);
  }

  /**
   * An implementation of the Base64Codec is required to be set before these methods work. ex:
   * Utils.setBase64Codec(new JavaBase64Codec());
   */
  public static String base64Decode(String input) {
    if (base64Codec == null) {
      throw new RuntimeException("Base64 codec not initialized");
    }
    if (input == null) {
      return null;
    }
    return base64Codec.base64Decode(input);
  }

  private static SimpleDateFormat buildUtcIsoDateFormat() {
    // Note: we don't use DateTimeFormatter.ISO_DATE so that this supports older Android versions
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    return dateFormat;
  }

  public interface Base64Codec {
    String base64Encode(String input);

    String base64Decode(String input);
  }
}
