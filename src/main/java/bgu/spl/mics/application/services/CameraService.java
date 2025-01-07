package bgu.spl.mics.application.services;

import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectedObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.*;

import java.util.List;
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
            System.out.println("Camera got tick "+ tick.getTickCounter());
            if(camera.getStatus() == STATUS.UP) {
                if (this.camera.getDetectedObjectsList().isEmpty()){
                   this.camera.setStatus(STATUS.DOWN);
                   terminate();
                }
                else {
                    int currentTime = tick.getTickCounter();
                    StampedDetectedObjects stampedList = new StampedDetectedObjects(currentTime);
                    StampedDetectedObjects currentFrames = camera.getStampedByTime(currentTime - camera.getFrequency());
                    if (currentFrames!=null){
                        for (DetectedObject object : currentFrames.getDetectedObjectsList()) {
                            if (object.getId().equals("ERROR")){
                                System.out.println("Camera recognized an ERROR at time "+currentTime);
                                this.camera.setStatus(STATUS.ERROR);
                                CrashedBroadcast crashedBroadcast = new CrashedBroadcast(this.getName());
                                crashedBroadcast.setErrorMessage("Camera disconnected");
                                crashedBroadcast.setFaultySensor(this.getName()+ " " + this.camera.getId());
                                crashedBroadcast.addLastCamerasFrame(this.camera ,this.camera.getLastFrames());
                                this.sendBroadcast(crashedBroadcast);
                                terminate();
                                break;
                            }
                            stampedList.getDetectedObjectsList().add(object);
                        }
                        this.camera.setLastFrames(currentFrames);
                        if((this.camera.getStatus()!=STATUS.ERROR) && (!stampedList.getDetectedObjectsList().isEmpty())){
                            System.out.println("Camera sends detected object "+stampedList.getDetectedObjectsList().peek().getId());
                            sendEvent(new DetectedObjectsEvent(stampedList));
                            StatisticalFolder.getInstance().addDetectedObjects(stampedList.getDetectedObjectsList().size());
                            //returns future , can be read result later
                        }

                    }
                }
            }
            else {
                terminate();
            }
        });
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if((terminate.getSenderId().equals("TimeService")) || (terminate.getSenderId().equals("LidarWorker")) || (terminate.getSenderId().equals("FusionSlam"))){
                System.out.println("Camera got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });
        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            System.out.println("Camera Service got crashed broadcast from "+crashed.getSenderId());
            if((crashed.getSenderId().equals("TimeService")) || (crashed.getSenderId().equals("LidarWorker")) || (crashed.getSenderId().equals("PoseService")) || (crashed.getSenderId().equals("FusionSlam"))){
                crashed.addLastCamerasFrame(this.camera ,this.camera.getLastFrames());
                terminate();
            }
        });

    }
}
