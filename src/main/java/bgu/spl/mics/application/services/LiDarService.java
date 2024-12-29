package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectedObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 *
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    private LiDarWorkerTracker liDarWorkerTracker;
    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker) {
        super("Lidar Worker");
        this.liDarWorkerTracker = liDarWorkerTracker;
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        this.subscribeEvent(DetectedObjectsEvent.class, (DetectedObjectsEvent detectedObjectEvent) ->{
            int currentTime = detectedObjectEvent.getStampedDetectedObjects().getTime();
            for (DetectedObject object : detectedObjectEvent.getStampedDetectedObjects().getDetectedObjectsList()) {
                CloudPoint[] coordinates = LiDarDataBase.getInstance().getCloudPointById(object.getId()).getCloudPoints();
                TrackedObject trackedObject = new TrackedObject(object.getId(),currentTime, object.getDescription(),coordinates);
            }

        });






        this.subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            if(liDarWorkerTracker.getStatus() == STATUS.UP) {
                int realTime = tick.getTickCounter();
                ConcurrentLinkedQueue<StampedDetectedObjects> stampedList = new ConcurrentLinkedQueue<StampedDetectedObjects>();
                for (StampedDetectedObjects object : camera.getDetectedObjectsList()) {
                    if (object.getTime() + camera.getFREQUENCY() == realTime) {
                        stampedList.add(object);
                    }
                }
                if(!stampedList.isEmpty()){
                    sendEvent(new DetectedObjectsEvent(realTime, stampedList));
                }
            }
            else {
                terminate();;
                sendBroadcast(new CrashedBroadcast("Lidarservice " + liDarWorkerTracker.getId() + "got crashed"));
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{terminate();});
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{terminate();});

    }
}
