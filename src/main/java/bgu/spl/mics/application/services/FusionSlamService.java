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
    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlam");
        this.fusionSlam = fusionSlam;
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        this.subscribeEvent(TrackedObjectsEvent.class , (TrackedObjectsEvent trackedObjectEvent) -> {
            System.out.println("Fusionslam got TrackedObject "+ trackedObjectEvent.getTrackedObjectsList().peek().getId());
            this.fusionSlam.addTrackedObject(trackedObjectEvent.getTrackedObjectsList());
            this.complete(trackedObjectEvent, "Fusion Slam Got " + trackedObjectEvent.getTrackedObjectsList().size() + " Tracked Objects");
        });
        this.subscribeEvent(PoseEvent.class , (PoseEvent poseEvent) -> {
            if (poseEvent.getPose() != null) {
                System.out.println("Fusionslam got Pose at time "+ poseEvent.getPose().getTime() + " sec");
                this.fusionSlam.addPose(poseEvent.getPose());
                this.complete(poseEvent,"Time: " + poseEvent.getPose().getTime() + ",X: " + poseEvent.getPose().getX() + ",Y: " + poseEvent.getPose().getY() + ",Yaw: " +poseEvent.getPose().getYaw());
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if(terminate.getSenderId().equals("TimeService")){
                System.out.println("FusionSlam got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            if(crashed.getSenderId().equals("TimeService")){
                terminate();
            }
        });

    }
}
