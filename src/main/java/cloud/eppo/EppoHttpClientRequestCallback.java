package cloud.eppo;

public interface EppoHttpClientRequestCallback {
  void onSuccess(String responseBody);
  void onFailure(String errorMessage);
}
