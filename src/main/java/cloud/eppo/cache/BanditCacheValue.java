package cloud.eppo.cache;

import java.util.Objects;

public class BanditCacheValue implements AssignmentCacheValue {
  private final String banditKey;
  private final String actionKey;

  public BanditCacheValue(String banditKey, String actionKey) {
    this.banditKey = banditKey;
    this.actionKey = actionKey;
  }

  @Override
  public String getValueIdentifier() {
    return banditKey + ";" + actionKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BanditCacheValue that = (BanditCacheValue) o;
    return Objects.equals(banditKey, that.banditKey) && Objects.equals(actionKey, that.actionKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(banditKey, actionKey);
  }
}
