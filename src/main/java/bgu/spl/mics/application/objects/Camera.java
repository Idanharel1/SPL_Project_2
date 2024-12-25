package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */

public class Camera {
private int id;
private final int FREQUENCY = 2;
private ConcurrentLinkedQueue<StampedDetectedObjects> detectedObjectsList;
private STATUS status;

    public Camera(int id) {
        this.id = id;
        this.status = STATUS.UP;
        this.detectedObjectsList = new ConcurrentLinkedQueue<StampedDetectedObjects>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFREQUENCY() {
        return FREQUENCY;
    }

    public ConcurrentLinkedQueue<StampedDetectedObjects> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    public void setDetectedObjectsList(ConcurrentLinkedQueue<StampedDetectedObjects> detectedObjectsList) {
        this.detectedObjectsList = detectedObjectsList;
    }

    public void addDetectedObjectsList(StampedDetectedObjects object) {
        this.detectedObjectsList.add(object);
    }
}
