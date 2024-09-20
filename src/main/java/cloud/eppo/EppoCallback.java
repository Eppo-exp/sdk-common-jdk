package cloud.eppo;

public interface EppoCallback<T> {
  void onSuccess(T result);

  void onFailure(String errorMessage);
}
