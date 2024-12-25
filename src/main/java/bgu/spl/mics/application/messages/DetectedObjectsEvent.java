package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DetectedObjectsEvent implements Event<String> {
    private String senderName;
    private int time;
    private ConcurrentLinkedQueue<DetectedObject> detectedObjectsList;

    public DetectedObjectsEvent(String senderName, int time, ConcurrentLinkedQueue<DetectedObject> detectedObjectsList) {
        this.senderName = senderName;
        this.time = time;
        this.detectedObjectsList = detectedObjectsList;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public ConcurrentLinkedQueue<DetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    public void setDetectedObjectsList(ConcurrentLinkedQueue<DetectedObject> detectedObjectsList) {
        this.detectedObjectsList = detectedObjectsList;
    }
}
