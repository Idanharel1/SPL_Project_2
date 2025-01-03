package bgu.spl.mics;

import bgu.spl.mics.application.objects.LiDarDataBase;

import java.util.HashMap;
import java.util.Queue;
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
	private HashMap<Class<? extends Event>, ConcurrentLinkedQueue<MicroService>> eventQueueHashMap;
	private HashMap<Event<?>, Future> eventFutureHashMapHashMap;

	private HashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadcastQueueHashMap;
	//holds Queue per microservice for next message to handle
	private HashMap<MicroService, ConcurrentLinkedQueue<Message>> microServiceQueueHashMap;
	//what callback handles each event

	private MessageBusImpl() {} // Private constructor to prevent instantiation
	private static class SingletonHolder{
		private static final MessageBusImpl instance = new MessageBusImpl();
	}
	public static MessageBusImpl getInstance(){
		return MessageBusImpl.SingletonHolder.instance;
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		this.eventQueueHashMap.get(type).add(m);
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		this.broadcastQueueHashMap.get(type).add(m);
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		this.eventFutureHashMapHashMap.get(e).resolve(result);
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		for (MicroService m : broadcastQueueHashMap.get(b.getClass())){
			microServiceQueueHashMap.get(m).add(b);
		}
	}

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		MicroService current = null;
		Future<T> future = null;
		if (!this.eventQueueHashMap.get(e.getClass()).isEmpty()){
			current = this.eventQueueHashMap.get(e.getClass()).remove();
			this.microServiceQueueHashMap.get(current).add(e);
			this.eventQueueHashMap.get(e.getClass()).add(current);
			future = new Future<T>();
			this.eventFutureHashMapHashMap.put(e,future);
			current.notifyAll();
		}
		return future;
	}

	@Override
	public void register(MicroService m) {
		//creates its queue in microServiceQueueHashMap
		microServiceQueueHashMap.put(m, new ConcurrentLinkedQueue<Message>() );
	}

	@Override
	public void unregister(MicroService m) {
		//removes its queue from microServiceQueueHashMap
		microServiceQueueHashMap.remove(m);
		//removes each appearance in all event / broadcast queues in eventQueueHashMap , broadcastQueueHashMap
		for (Class<? extends Event> eventType : this.eventQueueHashMap.keySet()){
			eventQueueHashMap.get(eventType).remove(m);
		}
		for (Class<? extends Broadcast> broadcastType : this.broadcastQueueHashMap.keySet()){
			broadcastQueueHashMap.get(broadcastType).remove(m);
		}
	}

	@Override
	public synchronized Message awaitMessage(MicroService m) throws InterruptedException {
		//everytime a microservice gets in locking message bus if he doesn't have an event waiting for him he moves to blocked
		Message messsage = null;
		if(microServiceQueueHashMap.get(m) == null){
			throw new IllegalStateException("This microservice isn't registered");
		}
		while (microServiceQueueHashMap.get(m).isEmpty()){
			m.wait();
		}
		messsage = microServiceQueueHashMap.get(m).remove();
		return messsage;
	}

	

}
