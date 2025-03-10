package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.concurrent.atomic.AtomicInteger;

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
            System.out.println("Poseservice got tick "+ tick.getTickCounter());
            int currentTime = tick.getTickCounter();
            this.gps.setCurrentTick(currentTime);
            if(gps.getStatus() == STATUS.UP) {
                Pose currentPose = gps.getCurrentPose(currentTime);
                if(currentPose!=null){
                    System.out.println("Pose service sent PoseEvent with time "+ currentTime + " sec");
                    StatisticalFolder.getInstance().setSystemRuntime(new AtomicInteger(currentTime));
                    this.sendEvent(new PoseEvent(currentPose));
                }
                if (this.gps.isFinishedReading(currentTime)){
                    this.gps.setStatus(STATUS.DOWN);
                    System.out.println("Pose service got terminated");
                    terminate();
                }
            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService")) || (terminate.getSenderId().equals("FusionSlam"))){
                System.out.println("gps got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            System.out.println("Pose Service got crashed broadcast from "+crashed.getSenderId());
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("LidarWorker")) || (crashed.getSenderId().equals("Camera")) || (crashed.getSenderId().equals("FusionSlam"))){
                crashed.setPoses(this.gps.posesUntilTick(this.gps.getCurrentTick()));
                terminate();
            }
        });

    }
}
