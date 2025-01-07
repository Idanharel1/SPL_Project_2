package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.Objects;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private FusionSlam fusionSlam;
    private boolean isPoseServiceTerminated = false;
    private boolean isLidarServiceTerminated = false;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */

    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlam");
        this.fusionSlam = fusionSlam;
    }

    public boolean isPoseServiceTerminated() {
        return isPoseServiceTerminated;
    }

    public void setPoseServiceTerminated(boolean poseServiceTerminated) {
        isPoseServiceTerminated = poseServiceTerminated;
    }

    public boolean isLidarServiceTerminated() {
        return isLidarServiceTerminated;
    }

    public void setLidarServiceTerminated(boolean lidarServiceTerminated) {
        isLidarServiceTerminated = lidarServiceTerminated;
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        this.subscribeEvent(TrackedObjectsEvent.class , (TrackedObjectsEvent trackedObjectEvent) -> {
            if ((trackedObjectEvent!= null) && (trackedObjectEvent.getTrackedObjectsList()!= null)) {
                System.out.println("Fusionslam got TrackedObject " + trackedObjectEvent.getTrackedObjectsList().peek().getId());
                this.fusionSlam.addTrackedObject(trackedObjectEvent.getTrackedObjectsList());
                this.complete(trackedObjectEvent, "Fusion Slam Got " + trackedObjectEvent.getTrackedObjectsList().size() + " Tracked Objects");
            }
        });
        this.subscribeEvent(PoseEvent.class , (PoseEvent poseEvent) -> {
            if ((poseEvent!=null) && (poseEvent.getPose() != null)) {
                System.out.println("Fusionslam got Pose at time "+ poseEvent.getPose().getTime() + " sec");
                this.fusionSlam.addPose(poseEvent.getPose());
                this.complete(poseEvent,"Time: " + poseEvent.getPose().getTime() + ",X: " + poseEvent.getPose().getX() + ",Y: " + poseEvent.getPose().getY() + ",Yaw: " +poseEvent.getPose().getYaw());
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            System.out.println("Fusion slam saw a terminated service: "+terminate.getSenderId());
            if(terminate.getSenderId().equals("LidarWorker")){
                this.setLidarServiceTerminated(true);
            }
            if(terminate.getSenderId().equals("PoseService")){
                this.setPoseServiceTerminated(true);
            }
            if((terminate.getSenderId().equals("TimeService")) || (this.isLidarServiceTerminated && this.isPoseServiceTerminated && this.fusionSlam.getTrackedObjects().isEmpty())){
                System.out.println("FusionSlam got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            System.out.println("FusionSlam Service got crashed broadcast from "+crashed.getSenderId());
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("LidarWorker")) || (crashed.getSenderId().equals("PoseService")) || (crashed.getSenderId().equals("Camera"))){
                System.out.println("FusionSlam got terminated from "+ crashed.getSenderId());
                terminate();
            }
        });

    }
}
