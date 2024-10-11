package cloud.eppo.api;

import cloud.eppo.logging.Assignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IAssignmentCache {
  void put(@NotNull String key, @NotNull Assignment assignment);

  @Nullable Assignment get(@NotNull String key);

  boolean containsKey(@NotNull String key);

  void remove(@NotNull String key);
}
