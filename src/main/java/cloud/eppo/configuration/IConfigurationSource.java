package cloud.eppo.configuration;

import cloud.eppo.api.Configuration;

public interface IConfigurationSource {
  public interface SuccessCallback {
    public void onSuccess(Configuration configuration);
  }

  public interface FailureCallback {
    public void onFailure(Throwable throwable);
  }

  public void load(SuccessCallback successCallback, FailureCallback failureCallback);
}
