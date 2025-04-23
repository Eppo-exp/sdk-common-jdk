package cloud.eppo.callback;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallbackManagerTest {

  private CallbackManager<String, List<String>> createCallbackManager() {
    return new CallbackManager<>(
        // Can't use a lambda as they were only introduced in java8
        new CallbackManager.Dispatcher<String, List<String>>() {
          @Override
          public void dispatch(List<String> callback, String data) {
            callback.add(data);
          }
        });
  }

  @Test
  public void testSubscribeAndNotify() {
    CallbackManager<String, List<String>> CallbackManager = createCallbackManager();

    List<String> received = new ArrayList<>();

    Runnable unsubscribe = CallbackManager.subscribe(received);

    CallbackManager.notifyCallbacks("test message");
    assertEquals(1, received.size());
    assertEquals("test message", received.get(0));

    unsubscribe.run();
    CallbackManager.notifyCallbacks("second message");
    assertEquals(1, received.size()); // Still 1 because we unsubscribed
  }

  @Test
  public void testThrowingCallback() {
    // The helper-created manager includes a dispatcher which pushes the data to the `add` method.
    CallbackManager<String, List<String>> manager = createCallbackManager();
    List<String> received = new ArrayList<>();

    List<String> throwingList =
        new ArrayList<String>() {
          @Override
          public boolean add(String o) {
            throw new RuntimeException("test message");
          }
        };

    Runnable unsubscribe1 = manager.subscribe(throwingList);
    Runnable unsubscribe2 = manager.subscribe(received);

    manager.notifyCallbacks("value");
    assertEquals(1, received.size());
  }

  @Test
  public void testMultipleSubscribers() {
    CallbackManager<Integer, List<Integer>> manager =
        new CallbackManager<>(
            new CallbackManager.Dispatcher<Integer, List<Integer>>() {

              @Override
              public void dispatch(List<Integer> callback, Integer data) {
                callback.add(data);
              }
            });
    List<Integer> received1 = new ArrayList<>();
    List<Integer> received2 = new ArrayList<>();

    manager.subscribe(received1);
    manager.subscribe(received2);

    manager.notifyCallbacks(42);

    assertEquals(1, received1.size());
    assertEquals(1, received2.size());
    assertEquals(42, received1.get(0));
    assertEquals(42, received2.get(0));
  }

  @Test
  public void testUnsubscribe() {
    CallbackManager<String, List<String>> manager = createCallbackManager();
    List<String> received = new ArrayList<>();

    Runnable unsubscribe1 = manager.subscribe(received);
    Runnable unsubscribe2 = manager.subscribe(received);

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
    CallbackManager<String, List<String>> manager = createCallbackManager();

    List<String> received = new ArrayList<>();

    manager.subscribe(received);
    manager.subscribe(received);

    manager.notifyCallbacks("value");
    assertEquals(2, received.size());

    manager.clear();

    manager.notifyCallbacks("value");
    assertEquals(2, received.size());
  }
}
