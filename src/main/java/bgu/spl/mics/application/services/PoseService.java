package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
private GPSIMU gps;
    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gps = gpsimu;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        this.subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            if(gps.getStatus() == STATUS.UP) {
                int currentTime = tick.getTickCounter();
                Pose currentPose = gps.getCurrentPose(currentTime);
                this.sendEvent(new PoseEvent(currentPose));
            }
            else {
                terminate();
                sendBroadcast(new CrashedBroadcast("PoseService " + this.getName() + "got crashed"));
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService")) || (terminate.getSenderId().equals("FusionSlam"))){
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("FusionSlam"))){
                terminate();
            }
        });

    }
}
