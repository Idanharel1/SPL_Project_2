package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.application.objects.*;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CrashedBroadcast implements Broadcast {

    private String senderId;
    private String errorMessage;
    private String faultySensor;
    private HashMap<Camera, StampedDetectedObjects> lastCamerasFrame;
    private HashMap<LiDarWorkerTracker, ConcurrentLinkedQueue<TrackedObject>> lastLidarWorkersFrames;
    private ConcurrentLinkedQueue<Pose> poses;


    public CrashedBroadcast(String senderId) {
        this.senderId = senderId;
        ErrorObject error =ErrorObject.getInstance();
        error.setCrashed(true);
        error.setCrashedBroadcast(this);
        this.lastCamerasFrame = new HashMap<>();
        this.lastLidarWorkersFrames = new HashMap<>();

    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFaultySensor() {
        return faultySensor;
    }

    public void setFaultySensor(String faultySensor) {
        this.faultySensor = faultySensor;
    }

    public HashMap<LiDarWorkerTracker, ConcurrentLinkedQueue<TrackedObject>> getLastLidarWorkersFrames() {
        return lastLidarWorkersFrames;
    }

    public void addLastLidarWorkersFrames(LiDarWorkerTracker lidar, ConcurrentLinkedQueue<TrackedObject> lastLidarWorkersFrames) {
        this.lastLidarWorkersFrames.putIfAbsent(lidar, lastLidarWorkersFrames);
    }

    public HashMap<Camera, StampedDetectedObjects> getLastCamerasFrame() {
        return lastCamerasFrame;
    }

    public void addLastCamerasFrame(Camera camera, StampedDetectedObjects lastCamerasFrame) {
        if(lastCamerasFrame!=null) {
            this.lastCamerasFrame.putIfAbsent(camera, lastCamerasFrame);
        }
    }

    public ConcurrentLinkedQueue<Pose> getPoses() {
        return poses;
    }

    public void setPoses(ConcurrentLinkedQueue<Pose> poses) {
        this.poses = poses;
    }
}
