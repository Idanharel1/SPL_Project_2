package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectedObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    private Camera camera;
    public CameraService(Camera camera) {
        super("Camera");
        this.camera = camera;
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        this.subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            if(camera.getStatus() == STATUS.UP) {
                if (this.camera.getDetectedObjectsList().isEmpty()){
                   this.camera.setStatus(STATUS.DOWN);
                   terminate();
                }
                else {
                    int currentTime = tick.getTickCounter();
                    StampedDetectedObjects stampedList = new StampedDetectedObjects(currentTime);
                    for (DetectedObject object : camera.getStampedByTime(currentTime - camera.getFrequency()).getDetectedObjectsList()) {
                        if (object.getId() == "ERROR"){
                            this.camera.setStatus(STATUS.ERROR);
                            sendBroadcast(new CrashedBroadcast("Camera disconnected"));
                            terminate();
                            break;
                        }
                        stampedList.getDetectedObjectsList().add(object);
                    }
                    if((this.camera.getStatus()!=STATUS.ERROR) && (!stampedList.getDetectedObjectsList().isEmpty())){
                        sendEvent(new DetectedObjectsEvent(stampedList));
                        StatisticalFolder.getInstance().addDetectedObjects(stampedList.getDetectedObjectsList().size());
                        //returns future , can be read result later
                    }
                }
            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService")) || (terminate.getSenderId().equals("LidarWorker")) || (terminate.getSenderId().equals("FusionSlam"))){
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("LidarWorker")) || (crashed.getSenderId().equals("FusionSlam"))){
                terminate();
            }
        });

    }
}
