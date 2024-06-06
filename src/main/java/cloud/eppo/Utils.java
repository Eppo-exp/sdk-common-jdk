package cloud.eppo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Utils {
  public static String getMD5Hex(String input) {
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
}
