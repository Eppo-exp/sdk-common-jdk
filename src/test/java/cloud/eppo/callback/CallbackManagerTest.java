package cloud.eppo.callback;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

  @Test
  public void testUnsubscribeByReference() {
    CallbackManager<String> manager = new CallbackManager<>();
    List<String> received1 = new ArrayList<>();
    List<String> received2 = new ArrayList<>();

    Consumer<String> callback1 = received1::add;
    Consumer<String> callback2 = received2::add;

    manager.subscribe(callback1);
    manager.subscribe(callback2);

    manager.notifyCallbacks("first");
    assertEquals(1, received1.size());
    assertEquals(1, received2.size());

    // Unsubscribe callback1
    boolean removed = manager.unsubscribe(callback1);
    assertTrue(removed);

    manager.notifyCallbacks("second");
    assertEquals(1, received1.size()); // callback1 should not receive this
    assertEquals(2, received2.size()); // callback2 should receive this
  }

  @Test
  public void testUnsubscribeNonExistentCallback() {
    CallbackManager<String> manager = new CallbackManager<>();
    List<String> received = new ArrayList<>();

    Consumer<String> callback1 = received::add;
    Consumer<String> callback2 = s -> {};

    manager.subscribe(callback1);

    // Try to unsubscribe a callback that was never subscribed
    boolean removed = manager.unsubscribe(callback2);
    assertFalse(removed);

    // Original callback should still work
    manager.notifyCallbacks("test");
    assertEquals(1, received.size());
  }

  @Test
  public void testUnsubscribeSameCallbackTwice() {
    CallbackManager<String> manager = new CallbackManager<>();
    List<String> received = new ArrayList<>();

    Consumer<String> callback = received::add;

    manager.subscribe(callback);

    manager.notifyCallbacks("first");
    assertEquals(1, received.size());

    // Unsubscribe the first time - should return true
    boolean removed1 = manager.unsubscribe(callback);
    assertTrue(removed1);

    // Unsubscribe the second time - should return false
    boolean removed2 = manager.unsubscribe(callback);
    assertFalse(removed2);

    manager.notifyCallbacks("second");
    assertEquals(1, received.size()); // Should not receive the second notification
  }

  @Test
  public void testUnsubscribeWithMultipleSubscribers() {
    CallbackManager<Integer> manager = new CallbackManager<>();
    List<Integer> received1 = new ArrayList<>();
    List<Integer> received2 = new ArrayList<>();
    List<Integer> received3 = new ArrayList<>();

    Consumer<Integer> callback1 = received1::add;
    Consumer<Integer> callback2 = received2::add;
    Consumer<Integer> callback3 = received3::add;

    manager.subscribe(callback1);
    manager.subscribe(callback2);
    manager.subscribe(callback3);

    manager.notifyCallbacks(1);
    assertEquals(1, received1.size());
    assertEquals(1, received2.size());
    assertEquals(1, received3.size());

    // Unsubscribe middle callback
    manager.unsubscribe(callback2);

    manager.notifyCallbacks(2);
    assertEquals(2, received1.size());
    assertEquals(1, received2.size()); // callback2 should not receive this
    assertEquals(2, received3.size());
  }
}
