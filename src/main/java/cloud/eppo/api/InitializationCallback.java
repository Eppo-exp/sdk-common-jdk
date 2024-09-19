package cloud.eppo.api;

public interface InitializationCallback {
    void onComplete();

    void onError(String errorMessage);
}
