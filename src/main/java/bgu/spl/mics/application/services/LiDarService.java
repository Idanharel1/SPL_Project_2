package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
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
        super("LidarWorker");
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
            if(this.liDarWorkerTracker.getStatus() == STATUS.UP) {
                int currentTime = detectedObjectEvent.getStampedDetectedObjects().getTime();
                String result = "Lidar handled event of objects ";
                //filled queue of lastTrackedObject from queue we got from detectedObjectEvent and lidarDataBase data
                for (DetectedObject object : detectedObjectEvent.getStampedDetectedObjects().getDetectedObjectsList()) {
                    CloudPoint[] coordinates = LiDarDataBase.getInstance().getCloudPointById(object.getId()).getCloudPoints();
                    TrackedObject trackedObject = new TrackedObject(object.getId(), currentTime, object.getDescription(), coordinates);
                    this.liDarWorkerTracker.getLastTrackedObject().add(trackedObject);
                    result = result + object.getId() + " ";
                }
                //sends true to camera's future
                this.complete(detectedObjectEvent , result);
                //suppose to send back coordinates to the lidarDataBase

            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TickBroadcast.class , (TickBroadcast tickBroadcast)->{
            if(liDarWorkerTracker.getStatus() == STATUS.UP) {
                int currentTime = tickBroadcast.getTickCounter();
                if (LiDarDataBase.getInstance().isFinishedReading(currentTime - this.liDarWorkerTracker.getFrequency())){
                    this.liDarWorkerTracker.setStatus(STATUS.DOWN);
                    terminate();
                }
                else if (LiDarDataBase.getInstance().isErrorInTime(currentTime - this.liDarWorkerTracker.getFrequency())){
                    this.liDarWorkerTracker.setStatus(STATUS.ERROR);
                    sendBroadcast(new CrashedBroadcast("Sensor LidarWorker disconnected"));
                    terminate();
                }
                else {
                    if (!this.liDarWorkerTracker.getLastTrackedObject().isEmpty()) {
                        ConcurrentLinkedQueue<TrackedObject> trackedObjectsToSend = new ConcurrentLinkedQueue<>();
                        for (TrackedObject objectInTime : this.liDarWorkerTracker.getLastTrackedObject()) {
                            if (objectInTime.getTime() + this.liDarWorkerTracker.getFrequency() == currentTime) {
                                trackedObjectsToSend.add(objectInTime);
                                this.liDarWorkerTracker.getLastTrackedObject().remove(objectInTime);
                            }
                        }
                        if (!trackedObjectsToSend.isEmpty()) {
                            this.sendEvent(new TrackedObjectsEvent(trackedObjectsToSend));
                            StatisticalFolder.getInstance().addTrackedObjects(trackedObjectsToSend.size());
                        }
                    }
                    //takes the list of tracked objects and send tracked broadcast
                }
            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService") || (terminate.getSenderId().equals("FusionSlam")))){
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            if((crashed.getSenderId().equals("TimeService") || (crashed.getSenderId().equals("FusionSlam")))){
                terminate();
            }
        });

    }
}
