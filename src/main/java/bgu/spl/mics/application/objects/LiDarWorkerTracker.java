package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    private int id;
    private final int FREQUENCY = 2;
    private STATUS status;
    private ConcurrentLinkedQueue<TrackedObject> lastTrackedObjects;

    public LiDarWorkerTracker(int id){
        this.id = id;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ConcurrentLinkedQueue<TrackedObject>();
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

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public ConcurrentLinkedQueue<TrackedObject> getLastTrackedObject() {
        return lastTrackedObjects;
    }

    public void setLastTrackedObject(ConcurrentLinkedQueue<TrackedObject> lastTrackedObject) {
        this.lastTrackedObjects = lastTrackedObject;
    }

    public void addLastTrackedObject(TrackedObject object) {
        this.lastTrackedObjects.add(object);
    }
}
