package bgu.spl.mics.application.objects;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    // Singleton instance holder
    private ConcurrentLinkedQueue<LandMark> landMarks;
    private ConcurrentLinkedQueue<Pose> poses;
    private ConcurrentLinkedQueue<TrackedObject> trackedObjects;

    private static class FusionSlamHolder {
            private static final FusionSlam instance = new FusionSlam();
        }
        public static FusionSlam getInstance(){
            return FusionSlam.FusionSlamHolder.instance;
        }

    public ConcurrentLinkedQueue<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }

    public ConcurrentLinkedQueue<Pose> getPoses() {
        return poses;
    }

    public ConcurrentLinkedQueue<LandMark> getLandMarks() {
        return landMarks;
    }

    public synchronized void addPose (Pose pose){
       this.poses.add(pose);
        for (TrackedObject trackedObject : this.trackedObjects){
            if (trackedObject.getTime() == pose.getTime()){
                addAsLandmark(trackedObject , pose);
                this.trackedObjects.remove(trackedObject);
            }
        }
        this.notifyAll();
    }
    public synchronized void addTrackedObject (ConcurrentLinkedQueue<TrackedObject> trackedObjects){
        for (TrackedObject trackedObject : trackedObjects){
            for (Pose pose : this.poses){
                if (pose.getTime() == trackedObject.getTime()){
                    addAsLandmark(trackedObject , pose);
                }
                else {
                    this.trackedObjects.add(trackedObject);
                }
            }
        }
        this.notifyAll();
    }
    private void addAsLandmark (TrackedObject trackedObject , Pose pose){
        Iterator<LandMark> landmarkIter = this.getLandMarks().iterator();

        //calculation of newLandMark
        double rad = pose.getYaw() * (Math.PI / 180);
        double cosRad = Math.cos(rad);
        double sinRad = Math.sin(rad);
        double xRobot = pose.getX();
        double yRobot = pose.getY();
        ConcurrentLinkedQueue<CloudPoint> coordinates = new ConcurrentLinkedQueue<CloudPoint>();
        for (CloudPoint objectCloudPoint : trackedObject.getCoordinates()){
            double xLocal = objectCloudPoint.getX();
            double yLocal = objectCloudPoint.getY();
            double xGlobal = (cosRad * xLocal) - (sinRad * yLocal) + xRobot;
            double yGlobal = (sinRad * xLocal) + (cosRad * yLocal) + yRobot;
            CloudPoint newCloudPoint = new CloudPoint(xGlobal, yGlobal);
            coordinates.add(newCloudPoint);
        }
        LandMark newLandmark = new LandMark(trackedObject.getId(), trackedObject.getDescription(), coordinates);
        while(landmarkIter.hasNext()){
            LandMark currentLandmark = landmarkIter.next();
            if (currentLandmark.getId() == newLandmark.getId()){
                this.getLandMarks().remove(currentLandmark);
                ConcurrentLinkedQueue<CloudPoint> avgCoordinates = new ConcurrentLinkedQueue<CloudPoint>();
                Iterator<CloudPoint> oldCoordinatesIterator = currentLandmark.getCoordinates().iterator();
                Iterator<CloudPoint> newCoordinatesIterator = newLandmark.getCoordinates().iterator();
                while ((oldCoordinatesIterator.hasNext()) && (newCoordinatesIterator.hasNext())){
                    CloudPoint oldCloudPoint = oldCoordinatesIterator.next();
                    CloudPoint newCloudPoint = newCoordinatesIterator.next();
                    CloudPoint avgCloudPoint = new CloudPoint((oldCloudPoint.getX()+newCloudPoint.getX())/2.0 , (oldCloudPoint.getY()+newCloudPoint.getY())/2.0);
                    avgCoordinates.add(avgCloudPoint);
                }
                newLandmark.setCoordinates(avgCoordinates);
            }
        }
            this.landMarks.add(newLandmark);
    }
}
