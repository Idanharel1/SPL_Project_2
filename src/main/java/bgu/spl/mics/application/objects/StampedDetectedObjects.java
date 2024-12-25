package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
private int time;
private ConcurrentLinkedQueue<DetectedObject> detectedObjectsList;

    public StampedDetectedObjects(int time) {
        this.time = time;
        this.detectedObjectsList = new ConcurrentLinkedQueue<DetectedObject>();
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

    public void addDetectedObjectsList(DetectedObject object) {
        this.detectedObjectsList.add(object);
    }
}
