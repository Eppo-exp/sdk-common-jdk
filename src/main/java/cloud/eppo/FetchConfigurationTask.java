package cloud.eppo;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchConfigurationTask extends TimerTask {
  private static final Logger log = LoggerFactory.getLogger(FetchConfigurationTask.class);
  private final Runnable runnable;
  private final Timer timer;
  private final long intervalInMillis;
  private final long jitterInMillis;
  private final long maxJitter;

  public FetchConfigurationTask(
      Runnable runnable, Timer timer, long intervalInMillis, long jitterInMillis) {
    assert (jitterInMillis > 0);

    this.runnable = runnable;
    this.timer = timer;
    this.intervalInMillis = intervalInMillis;
    this.maxJitter = intervalInMillis / 2;
    this.jitterInMillis = jitterInMillis;
  }

  public void scheduleNext() {
    // Limit jitter to half the interval. Also, prevents user-provided jitter from under-running the
    // delay below 0.
    long jitter =
        Math.min(maxJitter, Math.round(Math.floor((Math.random() * this.jitterInMillis))));
    long delay = this.intervalInMillis - jitter;
    FetchConfigurationTask nextTask =
        new FetchConfigurationTask(runnable, timer, intervalInMillis, jitterInMillis);
    timer.schedule(nextTask, delay);
  }

  @Override
  public void run() {
    // TODO: retry on failed fetches
    try {
      runnable.run();
    } catch (Exception e) {
      log.error("[Eppo SDK] Error fetching experiment configuration", e);
    }
    scheduleNext();
  }
}
