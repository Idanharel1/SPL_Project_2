package bgu.spl.mics;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.LiDarDataBase;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	//holds Queue per event / broadcast to know which microservices are registered to it
	private ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<MicroService>> eventQueueHashMap;
	private ConcurrentHashMap<Event<?>, Future> eventFutureHashMapHashMap;

	private ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadcastQueueHashMap;
	//holds Queue per microservice for next message to handle
	private ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> microServiceQueueHashMap;
	//what callback handles each event


	private MessageBusImpl() {
		this.broadcastQueueHashMap = new ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>>();
		this.eventFutureHashMapHashMap = new ConcurrentHashMap<Event<?>, Future>();
		this.microServiceQueueHashMap = new ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>>();
		this.eventQueueHashMap = new ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<MicroService>>();

		this.broadcastQueueHashMap.put(TickBroadcast.class, new ConcurrentLinkedQueue<>());
		this.broadcastQueueHashMap.put(TerminatedBroadcast.class, new ConcurrentLinkedQueue<>());
		this.broadcastQueueHashMap.put(CrashedBroadcast.class, new ConcurrentLinkedQueue<>());
		this.eventQueueHashMap.put(PoseEvent.class, new ConcurrentLinkedQueue<>());
		this.eventQueueHashMap.put(DetectedObjectsEvent.class, new ConcurrentLinkedQueue<>());
		this.eventQueueHashMap.put(TrackedObjectsEvent.class, new ConcurrentLinkedQueue<>());

	}

	private static class SingletonHolder{
		private static final MessageBusImpl instance = new MessageBusImpl();
	}
	public static MessageBusImpl getInstance(){
		return MessageBusImpl.SingletonHolder.instance;
	}

	// @PRE-CONDITION: The MicroService is registered
	// @POST-CONDITION: The MicroService is added to the event map queue.
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		this.eventQueueHashMap.get(type).add(m);
	}

	// @PRE-CONDITION: The MicroService is registered
	// @POST-CONDITION: The MicroService is added to the broadcast map queue.
	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		this.broadcastQueueHashMap.computeIfAbsent(type, k -> new ConcurrentLinkedQueue<>()).add(m);
	}

	// @PRE-CONDITION: The event is successfully processed and not completed yet.
	// @POST-CONDITION: The Future associated with the event is resolved with the result.
	@Override
	public <T> void complete(Event<T> e, T result) {
		if (this.eventFutureHashMapHashMap.get(e)!=null) {
			this.eventFutureHashMapHashMap.get(e).resolve(result);
		}
	}

	// @PRE-CONDITION: MicroServices are subscribed to the broadcast message
	// @POST-CONDITION: The broadcast message is added to the queues of all subscribed MicroServices.
	@Override
	public void sendBroadcast(Broadcast b) {
		for (MicroService m : broadcastQueueHashMap.get(b.getClass())){
			synchronized (microServiceQueueHashMap.get(m)){
				microServiceQueueHashMap.get(m).add(b);
				microServiceQueueHashMap.get(m).notifyAll();
			// we know that each iteraion wakes up all threads but it is better when microservice is treating each message independetly
			}
		}
	}

	// @PRE-CONDITION: The event is valid. At least one MicroService is subscribed to the event.
	// @POST-CONDITION: The event is sent to the appropriate MicroService, moves him to be ast in line and Future is returned.
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		MicroService current = null;
		Future<T> future = null;
		synchronized (this.eventQueueHashMap) {
			if (!this.eventQueueHashMap.get(e.getClass()).isEmpty()) {
				current = this.eventQueueHashMap.get(e.getClass()).remove();
				this.eventQueueHashMap.get(e.getClass()).add(current);
			}
		}
		if(current!=null) {
			synchronized (microServiceQueueHashMap.get(current)) {
				future = new Future<T>();
					this.eventFutureHashMapHashMap.put(e, future);
				this.microServiceQueueHashMap.get(current).add(e);
				this.microServiceQueueHashMap.get(current).notifyAll();
			}
		}
		return future;
	}

	// @PRE-CONDITION: The MicroService has not been registered.
	// @POST-CONDITION: A new queue is created for the MicroService.
	@Override
	public void register(MicroService m) {
		//creates its queue in microServiceQueueHashMap
		microServiceQueueHashMap.put(m, new ConcurrentLinkedQueue<>());
	}

	// @PRE-CONDITION: The MicroService has been registered and is not processing messages.
	// @POST-CONDITION: The MicroService is removed from the list of registered MicroServices, and no queues exist for it.
	@Override
	public void unregister(MicroService m) {
		microServiceQueueHashMap.remove(m);
		//removes each appearance in all event / broadcast queues in eventQueueHashMap , broadcastQueueHashMap
		for (Class<? extends Event> eventType : this.eventQueueHashMap.keySet()) {
			eventQueueHashMap.get(eventType).remove(m);
		}
		for (Class<? extends Broadcast> broadcastType : this.broadcastQueueHashMap.keySet()) {
			broadcastQueueHashMap.get(broadcastType).remove(m);
		}
	}

	// @PRE-CONDITION: The MicroService is registered.
	// @POST-CONDITION: The MicroService waits until a message is available and then it is added to its queue and returned.
	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		//everytime a microservice gets in locking message bus if he doesn't have an event waiting for him he moves to blocked
		Message messsage = null;
		if(microServiceQueueHashMap.get(m) == null){
			throw new IllegalStateException("This microservice isn't registered");
		}
		synchronized (microServiceQueueHashMap.get(m)) {
			while (microServiceQueueHashMap.get(m).isEmpty()) {
				microServiceQueueHashMap.get(m).wait();
			}
		}
		messsage = microServiceQueueHashMap.get(m).remove();
		return messsage;
	}

	//getters for testing purposes only
	public ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<MicroService>> getEventQueueHashMap() {
		return eventQueueHashMap;
	}

	public ConcurrentHashMap<Event<?>, Future> getEventFutureHashMapHashMap() {
		return eventFutureHashMapHashMap;
	}

	public ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> getBroadcastQueueHashMap() {
		return broadcastQueueHashMap;
	}

	public ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> getMicroServiceQueueHashMap() {
		return microServiceQueueHashMap;
	}
}
