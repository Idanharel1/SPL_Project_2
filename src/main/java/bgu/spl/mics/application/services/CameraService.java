package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectedObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
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
        super("camera");
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
                int currentTime = tick.getTickCounter();
                StampedDetectedObjects stampedList = new StampedDetectedObjects(currentTime);
                for (DetectedObject object : camera.getStampedByTime(currentTime - camera.getFREQUENCY()).getDetectedObjectsList()) {
                    stampedList.getDetectedObjectsList().add(object);
                }
                if(!stampedList.getDetectedObjectsList().isEmpty()){
                    sendEvent(new DetectedObjectsEvent(stampedList));
                }
            }
            else {
                terminate();;
                sendBroadcast(new CrashedBroadcast("Camera " + camera.getId() + "got crashed"));
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{terminate();});
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{terminate();});

    }
}
