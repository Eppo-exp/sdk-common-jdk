package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.bytebuddy.implementation.bytecode.Throw;

import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cloud.eppo.api.Configuration;

public class ConfigurationRequestorJava6Test {
    private final File initialFlagConfigFile =
            new File("src/test/resources/static/initial-flag-config.json");
    private final File differentFlagConfigFile =
            new File("src/test/resources/static/boolean-flag.json");

    @Test
    public void testInitialConfigurationJava6() throws IOException {
        IConfigurationStoreJava6 configStore = Mockito.spy(new ConfigurationStoreJava6());
        EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

        ConfigurationRequestorJava6 requestor =
                new ConfigurationRequestorJava6(configStore, mockHttpClient, false, true);

        byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);

        // verify config is empty to start
        assertTrue(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(0)).saveConfigurationJava6(any());

        boolean success = requestor.setInitialConfigurationJava6(Configuration.builder(flagConfig, false).build());
        assertTrue(success);

        assertFalse(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(1)).saveConfigurationJava6(any());
        assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));
    }

    @Test
    public void testInitialConfigurationJava6DoesntClobberFetch() throws Exception {
        IConfigurationStoreJava6 configStore = Mockito.spy(new ConfigurationStoreJava6());
        EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

        ConfigurationRequestorJava6 requestor =
                new ConfigurationRequestorJava6(configStore, mockHttpClient, false, true);

        byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);
        byte[] fetchedFlagConfig = FileUtils.readFileToByteArray(differentFlagConfigFile);

        when(mockHttpClient.getJava6(Constants.FLAG_CONFIG_ENDPOINT)).thenReturn(fetchedFlagConfig);

        assertTrue(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(0)).saveConfigurationJava6(any());

        // The initial config contains only one flag keyed `numeric_flag`. The fetch response has only
        // one flag keyed
        // `boolean_flag`. We make sure to complete the fetch future first to verify the cache load does
        // not overwrite it.
        requestor.fetchAndSaveFromRemoteJava6();

        assertFalse(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(1)).saveConfigurationJava6(any());

        // set the initial config
        boolean success = requestor.setInitialConfigurationJava6(Configuration.builder(flagConfig, false).build());
        assertFalse(success);

        // `numeric_flag` is only in the cache which should have been ignored.
        assertNull(configStore.getConfiguration().getFlag("numeric_flag"));

        // `boolean_flag` is available only from the fetch
        assertNotNull(configStore.getConfiguration().getFlag("boolean_flag"));
    }

    @Test
    public void testBrokenFetchDoesntClobberCache() throws Exception {
        IConfigurationStoreJava6 configStore = Mockito.spy(new ConfigurationStoreJava6());
        EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

        ConfigurationRequestorJava6 requestor =
                new ConfigurationRequestorJava6(configStore, mockHttpClient, false, true);

        byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);

        CountDownLatch httpCountDownLatch = new CountDownLatch(1);
        BlockingQueue<Throwable> blockingQueue = new ArrayBlockingQueue<>(1);
        when(mockHttpClient.getJava6(Constants.FLAG_CONFIG_ENDPOINT))
                .thenAnswer((Answer<Void>) invocation -> {
                    httpCountDownLatch.countDown();
                    throw blockingQueue.take();
                });

        CountDownLatch requestorCountDownLatch = new CountDownLatch(1);
        Thread requestorThread = new Thread(() -> {
          try {
            requestor.fetchAndSaveFromRemoteJava6();
          } finally {
            requestorCountDownLatch.countDown();
          }
        }, "testBrokenFetchDoesntClobberCache-requestorThread");
        requestorThread.start();

        // Make sure we made the HTTP request
        assertTrue(httpCountDownLatch.await(1000, TimeUnit.MILLISECONDS));

        // verify that no config has been set yet.
        assertTrue(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(0)).saveConfigurationJava6(any());

        // Set initial config
        requestor.setInitialConfigurationJava6(Configuration.builder(flagConfig, false).build());

        // Error out the fetch
        blockingQueue.put(new Exception("Intentional exception"));

        // Make sure we finished processing the HTTP request
        assertTrue(requestorCountDownLatch.await(1000, TimeUnit.MILLISECONDS));

        assertFalse(configStore.getConfiguration().isEmpty());
        Mockito.verify(configStore, Mockito.times(1)).saveConfigurationJava6(any());

        // `numeric_flag` is only in the cache which should be available
        assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

        assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
    }

    @Test
    public void testCacheWritesAfterBrokenFetch() throws Exception {
        IConfigurationStoreJava6 configStore = Mockito.spy(new ConfigurationStoreJava6());
        EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

        ConfigurationRequestorJava6 requestor =
                new ConfigurationRequestorJava6(configStore, mockHttpClient, false, true);

        String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);

        when(mockHttpClient.getJava6(Constants.FLAG_CONFIG_ENDPOINT))
            .thenThrow(new RuntimeException("Intentional exception"));

        // verify that no config has been set yet.
        Mockito.verify(configStore, Mockito.times(0)).saveConfigurationJava6(any());

        // default configuration is empty config.
        assertTrue(configStore.getConfiguration().isEmpty());

        // Fetch from remote with an error
        try {
          requestor.fetchAndSaveFromRemoteJava6();
          fail("Expected RuntimeException");
        } catch (RuntimeException ignored) {
        }

        // Set the initial config after the fetch throws an error.
        boolean success = requestor.setInitialConfigurationJava6(new Configuration.Builder(flagConfig, false).build());
        assertTrue(success);

        // Verify that a configuration was saved by the requestor
        Mockito.verify(configStore, Mockito.times(1)).saveConfigurationJava6(any());
        assertFalse(configStore.getConfiguration().isEmpty());

        // `numeric_flag` is only in the cache which should be available
        assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

        assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
    }

    private static ServerSocket findServerSocket() {
        int startPort = 8000;
        int endPort = 1000;
        int port = startPort;
        ServerSocket serverSocket = null;
        do {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException ignore) {
                port++;
            }
        } while (serverSocket == null && port < endPort);
        if (serverSocket == null) {
            fail("Couldn't allocate ServerSocket between [" + startPort + ", " + endPort + ")");
        }

        return serverSocket;
    }

    @Test
    public void testInterruptedFetchDoesNotClobberCache() throws Exception {
        try (ServerSocket serverSocket = findServerSocket()) {
          IConfigurationStoreJava6 configStore = Mockito.spy(new ConfigurationStoreJava6());
          EppoHttpClient realHttpClient = new EppoHttpClient(
            "http://localhost:" + serverSocket.getLocalPort(),
            "apiKey",
            "sdkName",
            "sdkVersion"
          );

          ConfigurationRequestorJava6 requestor =
              new ConfigurationRequestorJava6(configStore, realHttpClient, false, true);

          // verify that no config has been set yet.
          Mockito.verify(configStore, Mockito.times(0)).saveConfigurationJava6(any());

          // default configuration is empty config.
          assertTrue(configStore.getConfiguration().isEmpty());

          CountDownLatch serverSocketCountDownLatch = new CountDownLatch(1);
          Thread serverSocketThread = new Thread(() -> {
            try {
              Socket ignored = serverSocket.accept();
              // intentionally don't close the ignored socket. It'll get closed
              // when we close the ServerSocket
              serverSocketCountDownLatch.countDown();
            } catch (IOException ignored) {
            }
          }, "testInterruptedFetchDoesNotClobberCache-serverSocket");
          serverSocketThread.start();

          // Fetch from remote and later interrupt
          CountDownLatch requestorCountDownLatch = new CountDownLatch(1);
          AtomicBoolean expectExceptionAtomicBoolean = new AtomicBoolean();
          AtomicReference<Throwable> unexpectedExceptionAtomicReference = new AtomicReference<>();
          AtomicReference<Throwable> expectedExceptionAtomicReference = new AtomicReference<>();
          Thread requestorThread = new Thread(() -> {
            try {
              requestor.fetchAndSaveFromRemoteJava6();
            } catch (RuntimeException expected) {
              if (expectExceptionAtomicBoolean.get()) {
                expectedExceptionAtomicReference.set(expected);
              } else {
                unexpectedExceptionAtomicReference.set(expected);
              }
            }
            requestorCountDownLatch.countDown();
          }, "testInterruptedFetchDoesNotClobberCache-requestor");
          requestorThread.start();

          // Wait until we connect to the "server"
          assertTrue(serverSocketCountDownLatch.await(1000, TimeUnit.MILLISECONDS));

          String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);

          // Set the initial config.
          boolean success = requestor.setInitialConfigurationJava6(new Configuration.Builder(flagConfig, false).build());
          assertTrue(success);

          // Verify that a configuration was saved by the requestor
          Mockito.verify(configStore, Mockito.times(1)).saveConfigurationJava6(any());
          assertFalse(configStore.getConfiguration().isEmpty());

          expectExceptionAtomicBoolean.set(true);
          requestorThread.interrupt();
          assertTrue(requestorCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
          Throwable unexpectedException = unexpectedExceptionAtomicReference.get();
          assertNull(unexpectedException);

          Throwable expectedException = expectedExceptionAtomicReference.get();
          assertNotNull(expectedException);
          assertEquals(RuntimeException.class, expectedException.getClass());

          // `numeric_flag` is only in the cache which should be available
          assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

          assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
        }
    }

    private ConfigurationStoreJava6 mockConfigStoreJava6;
    private EppoHttpClient mockHttpClient;
    private ConfigurationRequestorJava6 requestor;

    @BeforeEach
    public void setup() {
        mockConfigStoreJava6 = mock(ConfigurationStoreJava6.class);
        mockHttpClient = mock(EppoHttpClient.class);
        requestor = new ConfigurationRequestorJava6(mockConfigStoreJava6, mockHttpClient, false, true);
    }

    @Test
    public void testConfigurationChangeListener() throws IOException {
        // Setup mock response
        String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
        when(mockHttpClient.get(anyString())).thenReturn(flagConfig.getBytes());

        List<Configuration> receivedConfigs = new ArrayList<>();

        // Subscribe to configuration changes
        Runnable unsubscribe = requestor.onConfigurationChange(receivedConfigs::add);

        // Initial fetch should trigger the callback
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(1, receivedConfigs.size());

        // Another fetch should trigger the callback again (fetches aren't optimized with eTag yet).
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(2, receivedConfigs.size());

        // Unsubscribe should prevent further callbacks
        unsubscribe.run();
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(2, receivedConfigs.size()); // Count should remain the same
    }

    @Test
    public void testMultipleConfigurationChangeListeners() {
        // Setup mock response
        when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());

        AtomicInteger callCount1 = new AtomicInteger(0);
        AtomicInteger callCount2 = new AtomicInteger(0);

        // Subscribe multiple listeners
        Runnable unsubscribe1 = requestor.onConfigurationChange(v -> callCount1.incrementAndGet());
        Runnable unsubscribe2 = requestor.onConfigurationChange(v -> callCount2.incrementAndGet());

        // Fetch should trigger both callbacks
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(1, callCount1.get());
        assertEquals(1, callCount2.get());

        // Unsubscribe first listener
        unsubscribe1.run();
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(1, callCount1.get()); // Should not increase
        assertEquals(2, callCount2.get()); // Should increase

        // Unsubscribe second listener
        unsubscribe2.run();
        requestor.fetchAndSaveFromRemoteJava6();
        assertEquals(1, callCount1.get()); // Should not increase
        assertEquals(2, callCount2.get()); // Should not increase
    }

    @Test
    public void testConfigurationChangeListenerIgnoresFailedFetch() {
        // Setup mock response to simulate failure
        when(mockHttpClient.getJava6(anyString())).thenThrow(new RuntimeException("Fetch failed"));

        AtomicInteger callCount = new AtomicInteger(0);
        requestor.onConfigurationChange(v -> callCount.incrementAndGet());

        // Failed fetch should not trigger the callback
        try {
            requestor.fetchAndSaveFromRemoteJava6();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Expected
        }
        assertEquals(0, callCount.get());
    }

    @Test
    public void testConfigurationChangeListenerIgnoresFailedSave() {
        // Setup mock responses
        when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());
        doThrow(new RuntimeException("Save failed"))
                .when(mockConfigStoreJava6).saveConfigurationJava6(any());

        AtomicInteger callCount = new AtomicInteger(0);
        requestor.onConfigurationChange(v -> callCount.incrementAndGet());

        // Failed save should not trigger the callback
        try {
            requestor.fetchAndSaveFromRemoteJava6();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Pass
        }
        assertEquals(0, callCount.get());
    }

    @Test
    public void testConfigurationChangeListenerSave() throws Exception {
        BlockingQueue<byte[]> blockingQueue = new ArrayBlockingQueue<>(1);
        // Setup mock responses
        when(mockHttpClient.getJava6(anyString()))
                .thenAnswer((Answer<byte[]>) invocation -> blockingQueue.take());

        AtomicInteger callCount = new AtomicInteger(0);
        requestor.onConfigurationChange(v -> callCount.incrementAndGet());

        // Start fetch
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread requestorThread = new Thread(() -> {
            requestor.fetchAndSaveFromRemoteJava6();
            countDownLatch.countDown();
        }, "testConfigurationChangeListenerSave-requesterThread");
        requestorThread.start();
        assertEquals(0, callCount.get()); // Callback should not be called yet

        // Complete the save
        blockingQueue.put("{\"flags\":{}}".getBytes());
        // Verify that the fetch completed
        assertTrue(countDownLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(1, callCount.get()); // Callback should be called after save completes
    }
}
