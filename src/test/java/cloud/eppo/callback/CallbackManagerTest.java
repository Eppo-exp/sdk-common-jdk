package cloud.eppo.callback;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallbackManagerTest {

  @Test
  public void testSubscribeAndNotify() {
    CallbackManager<String> CallbackManager = new CallbackManager<>();
    List<String> received = new ArrayList<>();

    Runnable unsubscribe = CallbackManager.subscribe(received::add);

    CallbackManager.notifyCallbacks("test message");
    assertEquals(1, received.size());
    assertEquals("test message", received.get(0));

    unsubscribe.run();
    CallbackManager.notifyCallbacks("second message");
    assertEquals(1, received.size()); // Still 1 because we unsubscribed
  }

  @Test
  public void testThrowingCallback() {
    CallbackManager<String> manager = new CallbackManager<>();
    List<String> received = new ArrayList<>();

    Runnable unsubscribe1 =
        manager.subscribe(
            (s) -> {
              throw new RuntimeException("test message");
            });
    Runnable unsubscribe2 = manager.subscribe(received::add);

    manager.notifyCallbacks("value");
    assertEquals(1, received.size());
  }

  @Test
  public void testMultipleSubscribers() {
    CallbackManager<Integer> manager = new CallbackManager<>();
    List<Integer> received1 = new ArrayList<>();
    List<Integer> received2 = new ArrayList<>();

    manager.subscribe(received1::add);
    manager.subscribe(received2::add);

    manager.notifyCallbacks(42);

    assertEquals(1, received1.size());
    assertEquals(1, received2.size());
    assertEquals(42, received1.get(0));
    assertEquals(42, received2.get(0));
  }

  @Test
  public void testUnsubscribe() {
    CallbackManager<String> manager = new CallbackManager<>();
    List<String> received = new ArrayList<>();

    Runnable unsubscribe1 = manager.subscribe(received::add);
    Runnable unsubscribe2 = manager.subscribe(received::add);

    manager.notifyCallbacks("value");
    assertEquals(2, received.size());

    unsubscribe1.run();
    manager.notifyCallbacks("value");

    // Only one subscriber adds to the list
    assertEquals(3, received.size());

    unsubscribe2.run();
    manager.notifyCallbacks("value");

    // No change to the size after both subscribers are cancelled
    assertEquals(3, received.size());
  }

  @Test
  public void testClear() {
    CallbackManager<String> manager = new CallbackManager<>();

    List<String> received = new ArrayList<>();

    manager.subscribe(received::add);
    manager.subscribe(received::add);

    manager.notifyCallbacks("value");
    assertEquals(2, received.size());

    manager.clear();

    manager.notifyCallbacks("value");
    assertEquals(2, received.size());
  }
}
