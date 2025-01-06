package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */

public class Camera {
private int id;
private final int frequency;
private ConcurrentLinkedQueue<StampedDetectedObjects> detectedObjectsList;
private STATUS status;
private StampedDetectedObjects lastFrames;

    public Camera(int id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjectsList = new ConcurrentLinkedQueue<StampedDetectedObjects>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFrequency() {
        return frequency;
    }

    public ConcurrentLinkedQueue<StampedDetectedObjects> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    public void setDetectedObjectsList(ConcurrentLinkedQueue<StampedDetectedObjects> detectedObjectsList) {
        this.detectedObjectsList = detectedObjectsList;
    }

    public void setLastFrames(StampedDetectedObjects lastFrames) {
        this.lastFrames = lastFrames;
    }

    public StampedDetectedObjects getLastFrames() {
        return lastFrames;
    }

    public void addDetectedObjectsList(StampedDetectedObjects object) {
        this.detectedObjectsList.add(object);
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }
    public StampedDetectedObjects getStampedByTime(int time){
        StampedDetectedObjects result = null;
        for (StampedDetectedObjects stamped : this.getDetectedObjectsList()){
            if(stamped.getTime() == time){
                result = stamped;
                break;
            }
        }
        return result;
    }
}
