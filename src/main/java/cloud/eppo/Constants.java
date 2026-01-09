package cloud.eppo;

/** Constants Class */
public class Constants {
  /** API Endpoint Settings */
  public static final String BANDIT_ENDPOINT = "/flag-config/v1/bandits";

  public static final String FLAG_CONFIG_ENDPOINT = "/flag-config/v1/config";
  public static final String DEFAULT_BASE_URL = "https://fscdn.eppo.cloud/api";

  /** Poller Settings */
  private static final long MILLISECOND_IN_ONE_SECOND = 1000;

  public static final long DEFAULT_POLLING_INTERVAL_MILLIS = 30 * MILLISECOND_IN_ONE_SECOND;
  public static final long DEFAULT_JITTER_INTERVAL_RATIO = 10;
}
