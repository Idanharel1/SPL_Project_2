package bgu.spl.mics;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.services.*;
import bgu.spl.mics.application.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageBus functionality
 */
public class MessageBusTest {
    private MessageBusImpl messageBus;
    private MicroService microService1;
    private MicroService microService2;

    @BeforeEach
    public void setUp() {
        messageBus = MessageBusImpl.getInstance();
        // Create test microservices
        microService1 = new CameraService(new Camera(1, 0));
        microService2 = new LiDarService(new LiDarWorkerTracker(1, 2));
    }

    @Test
    public void testRegisterAndUnregister() {
        messageBus.register(microService1);
        // Try to get message - should not throw exception if registered
        assertDoesNotThrow(() -> messageBus.awaitMessage(microService1),
                "Registered service should not throw exception");

        messageBus.unregister(microService1);
        // After unregister, should throw exception
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microService1),
                "Unregistered service should throw IllegalStateException");
    }

    @Test
    public void testSubscribeAndSendEvent() {
        messageBus.register(microService1);
        messageBus.register(microService2);

        // Subscribe to DetectedObjectsEvent
        messageBus.subscribeEvent(DetectedObjectsEvent.class, microService2);

        // Create and send test event
        StampedDetectedObjects objects = new StampedDetectedObjects(1);
        DetectedObjectsEvent event = new DetectedObjectsEvent(objects);
        Future<String> future = messageBus.sendEvent(event);

        assertNotNull(future, "Should return Future object for event");

        // Test completion
        String result = "Event handled";
        messageBus.complete(event, result);
        assertEquals(result, future.get(), "Future should contain completed result");
    }

    @Test
    public void testRoundRobin() {
        // Create multiple services
        MicroService service1 = new LiDarService(new LiDarWorkerTracker(1 , 2));
        MicroService service2 = new LiDarService(new LiDarWorkerTracker(2 , 2));
        MicroService service3 = new LiDarService(new LiDarWorkerTracker(3 , 2));

        // Register and subscribe all services
        messageBus.register(service1);
        messageBus.register(service2);
        messageBus.register(service3);
        messageBus.subscribeEvent(DetectedObjectsEvent.class, service1);
        messageBus.subscribeEvent(DetectedObjectsEvent.class, service2);
        messageBus.subscribeEvent(DetectedObjectsEvent.class, service3);

        // Send multiple events
        StampedDetectedObjects objects = new StampedDetectedObjects(1);
        DetectedObjectsEvent event1 = new DetectedObjectsEvent(objects);
        DetectedObjectsEvent event2 = new DetectedObjectsEvent(objects);
        DetectedObjectsEvent event3 = new DetectedObjectsEvent(objects);

        messageBus.sendEvent(event1);
        messageBus.sendEvent(event2);
        messageBus.sendEvent(event3);

        try {
            // Each service should get one event in round-robin order
            Message msg1 = messageBus.awaitMessage(service1);
            Message msg2 = messageBus.awaitMessage(service2);
            Message msg3 = messageBus.awaitMessage(service3);

            assertTrue(msg1 instanceof DetectedObjectsEvent);
            assertTrue(msg2 instanceof DetectedObjectsEvent);
            assertTrue(msg3 instanceof DetectedObjectsEvent);
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        }
    }

    @Test
    public void testBroadcastMessage() {
        messageBus.register(microService1);
        messageBus.register(microService2);

        // Subscribe both services to broadcast
        messageBus.subscribeBroadcast(TickBroadcast.class, microService1);
        messageBus.subscribeBroadcast(TickBroadcast.class, microService2);

        // Send broadcast
        TickBroadcast broadcast = new TickBroadcast("test", 1);
        messageBus.sendBroadcast(broadcast);

        // Verify both services received broadcast
        try {
            Message msg1 = messageBus.awaitMessage(microService1);
            Message msg2 = messageBus.awaitMessage(microService2);

            assertTrue(msg1 instanceof TickBroadcast,
                    "First service should receive TickBroadcast");
            assertTrue(msg2 instanceof TickBroadcast,
                    "Second service should receive TickBroadcast");
            assertEquals(((TickBroadcast)msg1).getTickCounter(), 1,
                    "First broadcast should have correct tick count");
            assertEquals(((TickBroadcast)msg2).getTickCounter(), 1,
                    "Second broadcast should have correct tick count");
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        }
    }
}