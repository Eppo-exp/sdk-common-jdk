package cloud.eppo.cache;

import cloud.eppo.api.IAssignmentCache;
import cloud.eppo.logging.Assignment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapAssignmentCache implements IAssignmentCache {
  private final Map<String, Assignment> mapCache = new ConcurrentHashMap<>();

  @Override
  public void put(@NotNull String key, @NotNull Assignment assignment) {
    mapCache.put(key, assignment);
  }

  @Nullable @Override
  public Assignment get(@NotNull String key) {
    return mapCache.get(key);
  }

  @Override
  public boolean containsKey(@NotNull String key) {
    return mapCache.containsKey(key);
  }

  @Override
  public void remove(@NotNull String key) {
    mapCache.remove(key);
  }
}
