package bgu.spl.mics;

import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Pose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MessageBusImplTest {

    private MessageBusImpl messageBus;
    private MicroService ms1;
    private MicroService ms2;

    @BeforeEach
    public void setUp() {
        // Initialize the MessageBus and MicroServices
        messageBus = MessageBusImpl.getInstance();
        ms1 = new MockMicroService("MicroService1");
        ms2 = new MockMicroService("MicroService2");
    }

    @Test
    public void testRegister() {
// @PRE-CONDITION: The MicroService has not been registered.
// @POST-CONDITION: A new queue is created for the MicroService.

        messageBus.register(ms1);
        assertTrue(messageBus.getMicroServiceQueueHashMap().containsKey(ms1), "MicroService1 should have a queue.");

// Re-registering should not create a new queue
        messageBus.register(ms1);
        int sizeBefore = messageBus.getMicroServiceQueueHashMap().size();
        messageBus.register(ms1);
        assertEquals(sizeBefore, messageBus.getMicroServiceQueueHashMap().size());
    }

    @Test
    public void testSubscribeEvent() {
        // @PRE-CONDITION: The MicroService is registered
        // @POST-CONDITION: The MicroService is added to the event map queue.
        messageBus.register(ms1);
        messageBus.subscribeEvent(PoseEvent.class, ms1);
        assertTrue(messageBus.getEventQueueHashMap().get(PoseEvent.class).contains(ms1),
                "MicroService1 should be subscribed to PoseEvent.");
    }

    @Test
    public void testSubscribeBroadcast() {
// @PRE-CONDITION: The MicroService has been registered. The broadcast class is
// valid.
// @POST-CONDITION: The MicroService is added to the broadcast subscribers list.

        messageBus.register(ms1);
        messageBus.subscribeBroadcast(TickBroadcast.class, ms1);
        assertTrue(messageBus.getBroadcastQueueHashMap().get(TickBroadcast.class).contains(ms1),
                "MicroService1 should be subscribed to TickBroadcast.");
    }

    @Test
    public void testSendBroadcast() throws InterruptedException {
// @PRE-CONDITION: All the MicroServices are registered. The broadcast message
// is valid.
// @POST-CONDITION: The broadcast message is added to the queues of all
// subscribed MicroServices.

        messageBus.register(ms1);
        messageBus.register(ms2);
        messageBus.subscribeBroadcast(TickBroadcast.class, ms1);
        messageBus.subscribeBroadcast(TickBroadcast.class, ms2);

// Send a broadcast
        TickBroadcast broadcast = new TickBroadcast("TimeService",0);
        messageBus.sendBroadcast(broadcast);

// Check that both microservices received the message
        ConcurrentLinkedQueue<Message> queue1 = messageBus.getMicroServiceQueueHashMap().get(ms1);
        ConcurrentLinkedQueue<Message> queue2 = messageBus.getMicroServiceQueueHashMap().get(ms2);

        assertEquals(broadcast, queue1.remove(),
                "MicroService1 should have received the TickBroadcast.");
        assertEquals(broadcast, queue2.remove(),
                "MicroService2 should have received the TickBroadcast.");
    }

    @Test

    public void testSendEvent() throws InterruptedException {
// @PRE-CONDITION: The event is valid. At least one MicroService is subscribed
// to the event.
// @POST-CONDITION: The event is sent to the appropriate MicroService and Future
// is returned.

        messageBus.register(ms1);
        messageBus.subscribeEvent(PoseEvent.class, ms1);
        Pose pose = new Pose(1, 2, 3, 3);

        PoseEvent event = new PoseEvent(pose);
        bgu.spl.mics.Future<String> future = (bgu.spl.mics.Future<String>) messageBus.sendEvent(event);

        assertNotNull(future, "Future should be returned for the event.");

// Simulate processing the event and completing the future
        messageBus.complete(event, "Time: " + pose.getTime() + ",X: " + pose.getX() + ",Y: " + pose.getY() + ",Yaw: " +pose.getYaw());

        String result;
        result = future.get();
        assertEquals("Time: " + event.getPose().getTime() + ",X: " + event.getPose().getX() + ",Y: " + event.getPose().getY() + ",Yaw: " +event.getPose().getYaw(), result, "The event result should match the completed event.");

    }

    @Test
    public void testComplete() throws InterruptedException {
// @PRE-CONDITION: The event is successfully processed and not completed yet.
// @POST-CONDITION: The Future associated with the event is resolved with the
// result.

        messageBus.register(ms1);
        messageBus.subscribeEvent(PoseEvent.class, ms1);
        Pose pose = new Pose(7, 3, 30, 8);

        PoseEvent event = new PoseEvent(pose);
        bgu.spl.mics.Future<String> future = (bgu.spl.mics.Future<String>) messageBus.sendEvent(event);

// Simulate processing the event and completing the future
        messageBus.complete(event, "Time: " + pose.getTime() + ",X: " + pose.getX() + ",Y: " + pose.getY() + ",Yaw: " +pose.getYaw());

        String result;
        result = future.get();
        assertEquals("Time: " + event.getPose().getTime() + ",X: " + event.getPose().getX() + ",Y: " + event.getPose().getY() + ",Yaw: " +event.getPose().getYaw(), result, "The event result should match the completed event.");

    }

    @Test
    public void testUnregister() {
// @PRE-CONDITION: The MicroService has been registered and is not processing
// messages.
// @POST-CONDITION: The MicroService is removed from the list of registered
// MicroServices, and no queues exist for it.

        messageBus.register(ms1);
        messageBus.unregister(ms1);
        assertFalse(messageBus.getMicroServiceQueueHashMap().containsKey(ms1),
                "MicroService1 should be unregistered and have no queue.");
    }

    @Test
    public void testAwaitMessage() throws InterruptedException {
// @PRE-CONDITION: The MicroService is registered.
// @POST-CONDITION: The MicroService waits until a message is available.

        messageBus.register(ms1);
        messageBus.subscribeBroadcast(TickBroadcast.class, ms1);

        TickBroadcast tickMessage = new TickBroadcast("TimeService",0);

// Send a broadcast and test awaiting it
        messageBus.sendBroadcast(tickMessage);

// Test if the message is received
        Message receivedMessage = messageBus.awaitMessage(ms1);
        assertEquals(tickMessage, receivedMessage, "The awaited message should be the TickBroadcast.");
    }

    // A mock MicroService class to simulate the behavior of a real MicroService
    private class MockMicroService extends MicroService {
        public MockMicroService(String name) {
            super(name);
        }

        @Override
        protected void initialize() {
// Initialization for mock services (doesn't need to do anything for this test)
        }
    }
}
