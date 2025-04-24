package cloud.eppo.api;

public interface EppoActionCallback<T> {
  void onSuccess(T data);

  void onFailure(Throwable error);
}
