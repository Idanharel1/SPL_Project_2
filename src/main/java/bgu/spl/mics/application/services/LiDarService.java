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
     * @param liDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker liDarWorkerTracker) {
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
            System.out.println("Lidar got detectedObject "+ detectedObjectEvent.getStampedDetectedObjects().getDetectedObjectsList().peek().getId());
            if(this.liDarWorkerTracker.getStatus() == STATUS.UP) {
                int currentTime = detectedObjectEvent.getStampedDetectedObjects().getTime();
                String result = "Lidar handled event of objects ";
                //filled queue of lastTrackedObject from queue we got from detectedObjectEvent and lidarDataBase data
                for (DetectedObject object : detectedObjectEvent.getStampedDetectedObjects().getDetectedObjectsList()) {
                    StampedCloudPoints cloudPoints = LiDarDataBase.getInstance().getCloudPointById(object.getId());
                    if(cloudPoints!=null) {
                        CloudPoint[] coordinates = cloudPoints.getCloudPoints();
                        TrackedObject trackedObject = new TrackedObject(object.getId(), currentTime, object.getDescription(), coordinates);
                        this.liDarWorkerTracker.getLastTrackedObject().add(trackedObject);
                        result = result + object.getId() + " ";
                    }
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
            System.out.println("Lidar got tick "+ tickBroadcast.getTickCounter());
            if(liDarWorkerTracker.getStatus() == STATUS.UP) {
                int currentTime = tickBroadcast.getTickCounter();

                if (LiDarDataBase.getInstance().isFinishedReading(currentTime - this.liDarWorkerTracker.getFrequency())){
                    this.liDarWorkerTracker.setStatus(STATUS.DOWN);
                    terminate();
                }
                else if (LiDarDataBase.getInstance().isErrorInTime(currentTime - this.liDarWorkerTracker.getFrequency())){
                    System.out.println("Camera recognized an ERROR at time "+currentTime);
                    this.liDarWorkerTracker.setStatus(STATUS.ERROR);
                    CrashedBroadcast crashedBroadcast = new CrashedBroadcast(this.getName());
                    crashedBroadcast.setErrorMessage("Sensor LidarWorker disconnected");
                    crashedBroadcast.setFaultySensor(this.getName()+this.liDarWorkerTracker.getId());
                    crashedBroadcast.addLastLidarWorkersFrames(this.liDarWorkerTracker , this.liDarWorkerTracker.getLastLidarWorkersFrames());
                    sendBroadcast(crashedBroadcast);
                    terminate();
                }
                if (!this.liDarWorkerTracker.getLastTrackedObject().isEmpty()) {
                    ConcurrentLinkedQueue<TrackedObject> trackedObjectsToSend = new ConcurrentLinkedQueue<>();
                    ConcurrentLinkedQueue<TrackedObject> pendingObjects = new ConcurrentLinkedQueue<>();
                    for (TrackedObject objectInTime : this.liDarWorkerTracker.getLastTrackedObject()) {
                        if (objectInTime.getTime() + this.liDarWorkerTracker.getFrequency() <= currentTime) {
                            trackedObjectsToSend.add(objectInTime);
                            this.liDarWorkerTracker.getLastTrackedObject().remove(objectInTime);
                        }
                        else {
                            pendingObjects.add(objectInTime);
                        }
                    }
                    if (!trackedObjectsToSend.isEmpty()) {
                        this.liDarWorkerTracker.setLastLidarWorkersFrames(trackedObjectsToSend);
                        System.out.println("Lidar sends tracked object "+ trackedObjectsToSend.peek().getId());
                        StatisticalFolder.getInstance().addTrackedObjects(trackedObjectsToSend.size());
                        this.sendEvent(new TrackedObjectsEvent(trackedObjectsToSend));
                    }
                    this.liDarWorkerTracker.setLastTrackedObject(pendingObjects);
                }

            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService") || (terminate.getSenderId().equals("FusionSlam")))){
                System.out.println("Lidar got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            System.out.println("lidar Service got crashed broadcast from "+crashed.getSenderId());
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("Camera")) || (crashed.getSenderId().equals("PoseService")) || (crashed.getSenderId().equals("FusionSlam"))){
                crashed.addLastLidarWorkersFrames(this.liDarWorkerTracker , this.liDarWorkerTracker.getLastLidarWorkersFrames());
                terminate();
            }
        });

    }
}
