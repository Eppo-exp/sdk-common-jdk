package cloud.eppo;

import cloud.eppo.api.EppoActionCallback;

public interface IEppoHttpClient {
  byte[] get(String path);

  void getAsync(String path, Callback callback);

  interface Callback extends EppoActionCallback<byte[]> {}

  class HttpException extends RuntimeException {
    public HttpException(String message) {
      super(message);
    }

    public HttpException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
