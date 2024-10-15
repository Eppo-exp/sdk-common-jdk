package cloud.eppo.cache;

import java.util.concurrent.CompletableFuture;

public interface AsyncMap<K, V> {
  CompletableFuture<V> get(K key);

  CompletableFuture<Void> set(K key, V value);

  CompletableFuture<Boolean> has(K key);
}
