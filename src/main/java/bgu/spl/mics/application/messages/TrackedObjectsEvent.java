package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackedObjectsEvent implements Event<String> {
    private String senderName;
    private ConcurrentLinkedQueue<TrackedObject> trackedObjectsList;

    public TrackedObjectsEvent(String senderName, ConcurrentLinkedQueue<TrackedObject> trackedObjectsList) {
        this.senderName = senderName;
        this.trackedObjectsList = trackedObjectsList;
    }

    public ConcurrentLinkedQueue<TrackedObject> getTrackedObjectsList() {
        return trackedObjectsList;
    }

    public void setTrackedObjectsList(ConcurrentLinkedQueue<TrackedObject> trackedObjectsList) {
        this.trackedObjectsList = trackedObjectsList;
    }


    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) { this.senderName = senderName; }

}
