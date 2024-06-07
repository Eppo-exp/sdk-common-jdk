package cloud.eppo;

import cloud.eppo.model.ShardRange;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ShardUtils {
  /** Returns whether the given shard is within the range. */
  public static boolean isShardInRange(int shard, ShardRange range) {
    return shard >= range.getStart() && shard < range.getEnd();
  }

  /** Convert input into md5 hex */
  public static String getHex(String input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error computing md5 hash", e);
    }
    byte[] messageDigest = md.digest(input.getBytes());
    BigInteger no = new BigInteger(1, messageDigest);
    String hashText = no.toString(16);
    while (hashText.length() < 32) {
      hashText = "0" + hashText;
    }

    return hashText;
  }

  public static int getShard(String input, int maxShardValue) {
    StringBuilder hashText = new StringBuilder(getHex(input));
    while (hashText.length() < 32) {
      hashText.insert(0, "0");
    }
    return (int) (Long.parseLong(hashText.substring(0, 8), 16) % maxShardValue);
  }
}
