package bgu.spl.mics;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.services.*;
import bgu.spl.mics.application.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MessageBusImplTest {
    private MessageBusImpl messageBus;
    private List<MicroService> microServices;
    private static final int NUM_SERVICES = 5;
    
    @BeforeEach
    public void setUp() {
        messageBus = MessageBusImpl.getInstance();
        microServices = new ArrayList<>();
        
        // Create multiple services
        for (int i = 0; i < NUM_SERVICES; i++) {
            if (i % 2 == 0) {
                microServices.add(new CameraService(new Camera(i)));
            } else {
                microServices.add(new LiDarService(new LiDarWorkerTracker(i)));
            }
        }
        
        // Clean up any previous registrations
        for (MicroService m : microServices) {
            messageBus.unregister(m);
        }
    }
    
    @Test
    public void testConcurrentEventSubscription() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_SERVICES);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Create threads for each service to subscribe
        List<Thread> threads = new ArrayList<>();
        for (MicroService service : microServices) {
            Thread t = new Thread(() -> {
                try {
                    messageBus.register(service);
                    messageBus.subscribeEvent(DetectedObjectsEvent.class, service);
                    startLatch.await(); // Wait for all to be ready
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads.add(t);
            t.start();
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Subscription timed out");
        assertEquals(NUM_SERVICES, successCount.get(), "Not all services subscribed successfully");
    }
    
    @Test
    public void testRoundRobinEventDistribution() throws InterruptedException {
        // Register and subscribe services
        for (MicroService service : microServices) {
            messageBus.register(service);
            messageBus.subscribeEvent(DetectedObjectsEvent.class, service);
        }
        
        int numEvents = 10;
        CountDownLatch eventLatch = new CountDownLatch(numEvents);
        AtomicInteger[] eventCounts = new AtomicInteger[NUM_SERVICES];
        for (int i = 0; i < NUM_SERVICES; i++) {
            eventCounts[i] = new AtomicInteger(0);
        }
        
        // Create receiver threads
        List<Thread> receiverThreads = new ArrayList<>();
        for (int i = 0; i < NUM_SERVICES; i++) {
            final int serviceIndex = i;
            Thread t = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Message msg = messageBus.awaitMessage(microServices.get(serviceIndex));
                        if (msg instanceof DetectedObjectsEvent) {
                            eventCounts[serviceIndex].incrementAndGet();
                            eventLatch.countDown();
                        }
                    }
                } catch (InterruptedException e) {
                    // Expected when we're done
                }
            });
            receiverThreads.add(t);
            t.start();
        }
        
        // Send events
        Thread senderThread = new Thread(() -> {
            for (int i = 0; i < numEvents; i++) {
                StampedDetectedObjects objects = new StampedDetectedObjects(i);
                DetectedObjectsEvent event = new DetectedObjectsEvent(objects);
                messageBus.sendEvent(event);
                try {
                    Thread.sleep(10); // Small delay between sends
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        senderThread.start();
        
        // Wait for all events to be received
        assertTrue(eventLatch.await(5, TimeUnit.SECONDS), "Event distribution timed out");
        
        // Cleanup threads
        receiverThreads.forEach(Thread::interrupt);
        for (Thread t : receiverThreads) {
            t.join(1000);
        }
        
        // Verify roughly even distribution
        int totalEvents = 0;
        for (AtomicInteger count : eventCounts) {
            totalEvents += count.get();
        }
        assertEquals(numEvents, totalEvents, "Not all events were received");
    }
    
    @Test
    public void testConcurrentBroadcastDelivery() throws InterruptedException {
        CountDownLatch broadcastLatch = new CountDownLatch(NUM_SERVICES);
        AtomicInteger receiveCount = new AtomicInteger(0);
        
        // Register and subscribe all services to broadcast
        for (MicroService service : microServices) {
            messageBus.register(service);
            messageBus.subscribeBroadcast(TickBroadcast.class, service);
        }
        
        // Create receiver threads
        List<Thread> receiverThreads = new ArrayList<>();
        for (MicroService service : microServices) {
            Thread t = new Thread(() -> {
                try {
                    Message msg = messageBus.awaitMessage(service);
                    if (msg instanceof TickBroadcast) {
                        receiveCount.incrementAndGet();
                        broadcastLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            receiverThreads.add(t);
            t.start();
        }
        
        // Send broadcast
        TickBroadcast broadcast = new TickBroadcast("test", 1);
        messageBus.sendBroadcast(broadcast);
        
        // Wait for all services to receive broadcast
        assertTrue(broadcastLatch.await(5, TimeUnit.SECONDS), "Broadcast reception timed out");
        assertEquals(NUM_SERVICES, receiveCount.get(), "Not all services received the broadcast");
    }
    
    @Test
    public void testEventFutureResolution() throws InterruptedException {
        MicroService sender = microServices.get(0);
        MicroService handler = microServices.get(1);
        messageBus.register(sender);
        messageBus.register(handler);
        messageBus.subscribeEvent(DetectedObjectsEvent.class, handler);
        
        CountDownLatch resolutionLatch = new CountDownLatch(1);
        String expectedResult = "Handled";
        
        // Handler thread
        Thread handlerThread = new Thread(() -> {
            try {
                Message msg = messageBus.awaitMessage(handler);
                if (msg instanceof DetectedObjectsEvent) {
                    messageBus.complete((Event) msg, expectedResult);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        handlerThread.start();
        
        // Sender thread
        Thread senderThread = new Thread(() -> {
            StampedDetectedObjects objects = new StampedDetectedObjects(1);
            DetectedObjectsEvent event = new DetectedObjectsEvent(objects);
            Future<String> future = messageBus.sendEvent(event);
            try {
                String result = future.get(5, TimeUnit.SECONDS);
                if (expectedResult.equals(result)) {
                    resolutionLatch.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        senderThread.start();
        
        assertTrue(resolutionLatch.await(5, TimeUnit.SECONDS), "Future resolution timed out");
    }
}
