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

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
//		synchronized(this.eventQueueHashMap) {
			this.eventQueueHashMap.get(type).add(m);
//		}
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
//		synchronized(this.broadcastQueueHashMap){
			this.broadcastQueueHashMap.get(type).add(m);
//		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
//		synchronized(this.eventFutureHashMapHashMap){
			//Might need to check if future is null before resolving it
			this.eventFutureHashMapHashMap.get(e).resolve(result);
//		}
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		for (MicroService m : broadcastQueueHashMap.get(b.getClass())){
			synchronized (this.microServiceQueueHashMap.get(m)){

				microServiceQueueHashMap.get(m).add(b);
				microServiceQueueHashMap.get(m).notifyAll();
			// we know that each iteraion wakes up all threads but it is better when microservice is treating each message independetly
			}
//			this.broadcastQueueHashMap.notifyAll();
		}
	}

	
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
//				synchronized(this.eventFutureHashMapHashMap) {
					this.eventFutureHashMapHashMap.put(e, future);
//				}

				this.microServiceQueueHashMap.get(current).add(e);
				this.microServiceQueueHashMap.get(current).notifyAll();
			}
		}
		return future;
	}

	@Override
	public void register(MicroService m) {
		//creates its queue in microServiceQueueHashMap
//		synchronized(this.microServiceQueueHashMap) {
			microServiceQueueHashMap.put(m, new ConcurrentLinkedQueue<>());
//		}
	}

	@Override
	public void unregister(MicroService m) {
//		synchronized(this.microServiceQueueHashMap) {
			//removes its queue from microServiceQueueHashMap
			microServiceQueueHashMap.remove(m);
//		}
//		synchronized(this.eventQueueHashMap) {
			//removes each appearance in all event / broadcast queues in eventQueueHashMap , broadcastQueueHashMap
			for (Class<? extends Event> eventType : this.eventQueueHashMap.keySet()) {
				eventQueueHashMap.get(eventType).remove(m);
			}
//		}
//		synchronized(this.broadcastQueueHashMap) {
			for (Class<? extends Broadcast> broadcastType : this.broadcastQueueHashMap.keySet()) {
				broadcastQueueHashMap.get(broadcastType).remove(m);
			}
//		}
	}

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
			messsage = microServiceQueueHashMap.get(m).remove();
		}
		return messsage;
	}

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
