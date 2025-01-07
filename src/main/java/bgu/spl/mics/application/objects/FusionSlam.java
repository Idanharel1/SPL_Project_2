package bgu.spl.mics.application.objects;

import java.util.Iterator;
import java.util.Objects;
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

    private FusionSlam() {
        this.trackedObjects = new ConcurrentLinkedQueue<TrackedObject>();
        this.poses = new ConcurrentLinkedQueue<Pose>();
        this.landMarks = new ConcurrentLinkedQueue<LandMark>();
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

    public void addPose (Pose pose){
        if (pose!=null) {
            this.poses.add(pose);
            if((this.trackedObjects!=null) && (!this.trackedObjects.isEmpty())) {
                for (TrackedObject trackedObject : this.trackedObjects) {
                    if (trackedObject.getTime() == pose.getTime()) {
                        addAsLandmark(trackedObject, pose);
                        this.trackedObjects.remove(trackedObject);
                    }
                }
            }
        }
    }
    public void addTrackedObject (ConcurrentLinkedQueue<TrackedObject> newTrackedObjects){
        boolean foundPose = false;
        for (TrackedObject trackedObject : newTrackedObjects){
            for (Pose pose : this.poses) {
                if (pose.getTime() == trackedObject.getTime()) {
                    foundPose = true;
                    addAsLandmark(trackedObject, pose);
                }
            }
            if (!foundPose){
                this.trackedObjects.add(trackedObject);
            }
        }
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
            double xGlobal = (Math.cos(rad) * xLocal) - (Math.sin(rad) * yLocal) + xRobot;
            double yGlobal = (Math.sin(rad) * xLocal) + (Math.cos(rad) * yLocal) + yRobot;
            CloudPoint newCloudPoint = new CloudPoint(xGlobal, yGlobal);
            coordinates.add(newCloudPoint);
        }
        boolean isExist = false;
        LandMark newLandmark = new LandMark(trackedObject.getId(), trackedObject.getDescription(), coordinates);
        while(landmarkIter.hasNext()){
            LandMark currentLandmark = landmarkIter.next();
            if (currentLandmark.getId().equals(newLandmark.getId())){
                System.out.println("Object already a Landmark");
                isExist = true;
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
                //if one object has more coordinates
                Iterator<CloudPoint> lastIterator = newCoordinatesIterator;
                if (oldCoordinatesIterator.hasNext()){
                    lastIterator = oldCoordinatesIterator;
                }
                while (lastIterator.hasNext()){
                    avgCoordinates.add(lastIterator.next());
                }
                newLandmark.setCoordinates(avgCoordinates);
            }
        }
        if(!isExist){
            System.out.println("instance has " + StatisticalFolder.getInstance().getNumLandmarks().intValue() + " and about to be plus 1 because object " + newLandmark.getId());
            StatisticalFolder.getInstance().addNumLandmarks(1);
        }
        this.landMarks.add(newLandmark);
    }
}
