package cloud.eppo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
  private static final ThreadLocal<SimpleDateFormat> UTC_ISO_DATE_FORMAT = buildUtcIsoDateFormat();
  private static final Logger log = LoggerFactory.getLogger(Utils.class);
  private static final ThreadLocal<MessageDigest> md = buildMd5MessageDigest();

  @SuppressWarnings("AnonymousHasLambdaAlternative")
  private static ThreadLocal<MessageDigest> buildMd5MessageDigest() {
    return new ThreadLocal<MessageDigest>() {
      @Override
      protected MessageDigest initialValue() {
        try {
          return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException("Error initializing MD5 hash", e);
        }
      }
    };
  }

  @SuppressWarnings("AnonymousHasLambdaAlternative")
  private static ThreadLocal<SimpleDateFormat> buildUtcIsoDateFormat() {
    return new ThreadLocal<SimpleDateFormat>() {
      @Override
      protected SimpleDateFormat initialValue() {
        // Note: we don't use DateTimeFormatter.ISO_DATE so that this supports older Android
        // versions
        SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return dateFormat;
      }
    };
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
    md.get().reset();
    byte[] md5Bytes = md.get().digest(input.getBytes());
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
    md.get().reset();
    byte[] md5Bytes = md.get().digest(input.getBytes());

    // Extract the first 4 bytes (8 digits) and convert to a long
    long value = 0;
    for (int i = 0; i < 4; i++) {
      value = (value << 8) | (md5Bytes[i] & 0xFF);
    }

    // Modulo into the shard space
    return (int) (value % maxShardValue);
  }

  public static String getISODate(Date date) {
    return UTC_ISO_DATE_FORMAT.get().format(date);
  }

  public static String base64Encode(String input) {
    if (input == null) {
      return null;
    }
    return new String(Base64.getEncoder().encode(input.getBytes(StandardCharsets.UTF_8)));
  }

  public static String base64Decode(String input) {
    if (input == null) {
      return null;
    }
    byte[] decodedBytes = Base64.getDecoder().decode(input);
    if (decodedBytes.length == 0 && !input.isEmpty()) {
      throw new RuntimeException(
          "zero byte output from Base64; if not running on Android hardware be sure to use RobolectricTestRunner");
    }
    return new String(decodedBytes);
  }
}
