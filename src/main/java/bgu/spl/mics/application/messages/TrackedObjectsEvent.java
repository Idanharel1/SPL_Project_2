package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackedObjectsEvent implements Event<String> {
    private ConcurrentLinkedQueue<TrackedObject> trackedObjectsList;

    public TrackedObjectsEvent(ConcurrentLinkedQueue<TrackedObject> trackedObjectsList) {
        this.trackedObjectsList = trackedObjectsList;
    }

    public ConcurrentLinkedQueue<TrackedObject> getTrackedObjectsList() {
        return trackedObjectsList;
    }

    public void setTrackedObjectsList(ConcurrentLinkedQueue<TrackedObject> trackedObjectsList) {
        this.trackedObjectsList = trackedObjectsList;
    }
}
