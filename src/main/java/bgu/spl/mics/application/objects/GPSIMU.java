package bgu.spl.mics.application.objects;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick;
    private STATUS status;
    private ConcurrentLinkedQueue<Pose> PoseList;

    public GPSIMU(int currentTick, STATUS status, ConcurrentLinkedQueue<Pose> poseList) {
        this.currentTick = currentTick;
        this.status = status;
        PoseList = poseList;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(int currentTick) {
        this.currentTick = currentTick;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public ConcurrentLinkedQueue<Pose> getPoseList() {
        return PoseList;
    }

    public void setPoseList(ConcurrentLinkedQueue<Pose> poseList) {
        PoseList = poseList;
    }

    public Pose getCurrentPose(int tick){
        Pose currentPose = null;
        for(Pose p1 : this.PoseList){
            if (p1.getTime() == tick){
                currentPose = p1;
                break;
            }
        }
        return currentPose;
    }
}
