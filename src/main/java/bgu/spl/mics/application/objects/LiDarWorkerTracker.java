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
    private final int frequency;
    private STATUS status;
    private ConcurrentLinkedQueue<TrackedObject> lastTrackedObjects;
    private ConcurrentLinkedQueue<TrackedObject> lastLidarWorkersFrames;

    public LiDarWorkerTracker(int id, int frequency){
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ConcurrentLinkedQueue<TrackedObject>();
        this.lastLidarWorkersFrames = new ConcurrentLinkedQueue<TrackedObject>();
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

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public ConcurrentLinkedQueue<TrackedObject> getLastLidarWorkersFrames() {
        return lastLidarWorkersFrames;
    }

    public void setLastLidarWorkersFrames(ConcurrentLinkedQueue<TrackedObject> lastLidarWorkersFrames) {
        this.lastLidarWorkersFrames = lastLidarWorkersFrames;
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
