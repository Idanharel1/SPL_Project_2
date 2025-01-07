package bgu.spl.mics.application;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param filePath Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static List<Camera> parseCamers(String filePath){
        List<Camera> result = new ArrayList<>();
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
            JsonArray camerasToCreate = jsonObject.get("Cameras").getAsJsonObject().get("CamerasConfigurations").getAsJsonArray();
            Iterator<JsonElement> camerasJsonIter = camerasToCreate.iterator();
            while (camerasJsonIter.hasNext()){
                JsonElement currentCameraJson = camerasJsonIter.next();
                String cameraDataPathString = jsonObject.get("Cameras").getAsJsonObject().get("camera_datas_path").getAsString();
                Path cameraDataPath = Paths.get(filePath).getParent();
                cameraDataPathString = cameraDataPath.resolve(cameraDataPathString).toString();
                String currentCameraKey = currentCameraJson.getAsJsonObject().get("camera_key").getAsString();
                try (FileReader cameraReader = new FileReader(cameraDataPathString)) {
                    JsonObject cameraDataJson = gson.fromJson(cameraReader, JsonObject.class);
                    JsonArray currentCameraDataJsonArray = cameraDataJson.get(currentCameraKey).getAsJsonArray();
                    Iterator<JsonElement> currentCameraDataJsonIter = currentCameraDataJsonArray.iterator();
                    ConcurrentLinkedQueue<StampedDetectedObjects> stampedQueue = new ConcurrentLinkedQueue<StampedDetectedObjects>();
                    while(currentCameraDataJsonIter.hasNext()){
                        JsonElement currentCameraJsonElement = currentCameraDataJsonIter.next();
                        ConcurrentLinkedQueue<DetectedObject> detectedQueue = new ConcurrentLinkedQueue<DetectedObject>();
                        Iterator<JsonElement> currentDetectedObjectListIter = currentCameraJsonElement.getAsJsonObject().get("detectedObjects").getAsJsonArray().iterator();
                        while(currentDetectedObjectListIter.hasNext()){
                            JsonElement currentDetectedObject = currentDetectedObjectListIter.next();
                            DetectedObject currentObject = new DetectedObject(currentDetectedObject.getAsJsonObject().get("id").getAsString(),currentDetectedObject.getAsJsonObject().get("description").getAsString());
                            detectedQueue.add(currentObject);
                        }
                        StampedDetectedObjects currentStampedDetectedObjects = new StampedDetectedObjects(currentCameraJsonElement.getAsJsonObject().get("time").getAsInt());
                        currentStampedDetectedObjects.setDetectedObjectsList(detectedQueue);
                        stampedQueue.add(currentStampedDetectedObjects);
                    }
                    Camera currentCamera = new Camera(currentCameraJson.getAsJsonObject().get("id").getAsInt() , currentCameraJson.getAsJsonObject().get("frequency").getAsInt());
                    currentCamera.setDetectedObjectsList(stampedQueue);
                    result.add(currentCamera);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static TimeService parseTimeService(String filePath){
        TimeService timeService = null;
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
            timeService = new TimeService(jsonObject.get("TickTime").getAsInt() ,jsonObject.get("Duration").getAsInt() );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timeService;
    }
    public static List<LiDarWorkerTracker> parseLidar(String filepath){
        List<LiDarWorkerTracker> result = new ArrayList<>();
        Gson gson = new Gson();
        try (FileReader configReader = new FileReader(filepath)) {
            JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
            JsonArray lidarObjects = configJson.get("LiDarWorkers").getAsJsonObject().get("LidarConfigurations").getAsJsonArray();
            for(JsonElement object : lidarObjects) {
                LiDarWorkerTracker lidar1 = new LiDarWorkerTracker(object.getAsJsonObject().get("id").getAsInt(), object.getAsJsonObject().get("frequency").getAsInt());
                result.add(lidar1);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static GPSIMU parseGPSIMU(String filepath){
        GPSIMU gps = null;
        String posepath = "";
        Gson gson = new Gson();
        try (FileReader configReader = new FileReader(filepath)) {
            JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
            posepath = configJson.get("poseJsonFile").getAsString();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Path configPath = Paths.get(filepath).getParent();
        posepath = configPath.resolve(posepath).normalize().toString();
        ConcurrentLinkedQueue<Pose> poseList = new ConcurrentLinkedQueue<Pose>();
        try (FileReader configReader = new FileReader(posepath)) {
            JsonArray objects = gson.fromJson(configReader, JsonArray.class);
            for(JsonElement object : objects) {
                Pose p1 = new Pose(object.getAsJsonObject().get("x").getAsFloat(), object.getAsJsonObject().get("y").getAsFloat(),
                        object.getAsJsonObject().get("yaw").getAsFloat(), object.getAsJsonObject().get("time").getAsInt());
                poseList.add(p1);
            }
            gps = new GPSIMU(0, poseList);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return gps;
    }
    public static void createOutputFile(Path parentPath){
        Path outputJsonPath = parentPath.resolve("simulation_output.json");
        StatisticalFolder instance = StatisticalFolder.getInstance();
        // Prepare data for the JSON file (for example, statistics)
        Map<String, Object> output = new HashMap<>();
        output.put("systemRuntime", instance.getSystemRuntime().intValue());
        output.put("numDetectedObjects", instance.getNumDetectedObjects().intValue());
        output.put("numTrackedObjects", instance.getNumTrackedObjects().intValue());
        output.put("numLandmarks", instance.getNumLandmarks().intValue());

        Map<String, Map<String, Object>> landMarksMap = new HashMap<>();
        for (LandMark landMark : FusionSlam.getInstance().getLandMarks()) {
            Map<String, Object> landmarkDetails = new HashMap<>();
            landmarkDetails.put("id", landMark.getId());
            landmarkDetails.put("description", landMark.getDescription());

            // Convert CloudPoints to a list of coordinate maps
            List<Map<String, Double>> coordinates = landMark.getCoordinates().stream()
                    .map(cloudPoint -> Map.of(
                            "x", cloudPoint.getX(),
                            "y", cloudPoint.getY()
                    ))
                    .toList();

            landmarkDetails.put("coordinates", coordinates);

            // Add the landmark to the main map using the ID as the key
            landMarksMap.put(landMark.getId(), landmarkDetails);
        }
        output.put("landmarks", landMarksMap);

        // Convert the statistics to JSON using Gson
        Gson gson = new Gson();
        String jsonString = gson.toJson(output);

        try {
            // Write the JSON data to the file, creating it if it doesn't exist
            Files.write(outputJsonPath, jsonString.getBytes());
            System.out.println("Statistics successfully written to: " + outputJsonPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createOutputFileWithErrors(Path parentPath, ErrorObject error){
        Path outputJsonPath = parentPath.resolve("error_simulation_output.json");
        StatisticalFolder instance = StatisticalFolder.getInstance();
        // Prepare data for the JSON file (for example, statistics)
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("systemRuntime", instance.getSystemRuntime().intValue());
        statistics.put("numDetectedObjects", instance.getNumDetectedObjects().intValue());
        statistics.put("numTrackedObjects", instance.getNumTrackedObjects().intValue());
        statistics.put("numLandmarks", instance.getNumLandmarks().intValue());

        Map<String, Map<String, Object>> landMarksMap = new HashMap<>();
        for (LandMark landMark : FusionSlam.getInstance().getLandMarks()) {
            Map<String, Object> landmarkDetails = new HashMap<>();
            landmarkDetails.put("id", landMark.getId());
            landmarkDetails.put("description", landMark.getDescription());

            // Convert CloudPoints to a list of coordinate maps
            List<Map<String, Double>> coordinates = landMark.getCoordinates().stream()
                    .map(cloudPoint -> Map.of(
                            "x", cloudPoint.getX(),
                            "y", cloudPoint.getY()
                    ))
                    .toList();

            landmarkDetails.put("coordinates", coordinates);

            // Add the landmark to the main map using the ID as the key
            landMarksMap.put(landMark.getId(), landmarkDetails);
        }
        statistics.put("landmarks", landMarksMap);
        Map<String, Object> output = new HashMap<>();
        CrashedBroadcast crashedBroadcast = error.getCrashedBroadcast();
        if(crashedBroadcast!=null) {
            output.put("error", crashedBroadcast.getSenderId());
            output.put("faultySensor", crashedBroadcast.getFaultySensor());
            Map<String, Object> cameraMap = new HashMap<>();
            for (Camera camera : crashedBroadcast.getLastCamerasFrame().keySet()) {
                StampedDetectedObjects stamped = crashedBroadcast.getLastCamerasFrame().get(camera);
                Map<String, Object> timeMap = new HashMap<>();
                int time = stamped.getTime();
                timeMap.put("time", time);
                List<Map<String, Object>> detectedObjectsArray = new ArrayList<>();
                for (DetectedObject object : stamped.getDetectedObjectsList()) {
                    Map<String, Object> detectedObject = new HashMap<>();
                    detectedObject.put("id", object.getId());
                    detectedObject.put("description", object.getDescription());
                    detectedObjectsArray.add(detectedObject);
                }
                timeMap.put("detectedObjects", detectedObjectsArray);
                cameraMap.put("Camera" + camera.getId(), timeMap);
            }
            output.put("lastCamerasFrame", cameraMap);
            Map<String, Object> lidarsMap = new HashMap<>();
            for (LiDarWorkerTracker lidar : crashedBroadcast.getLastLidarWorkersFrames().keySet()) {
                ConcurrentLinkedQueue<TrackedObject> trackedObjects = crashedBroadcast.getLastLidarWorkersFrames().get(lidar);
                Iterator<TrackedObject> trackedObjectIter = trackedObjects.iterator();
                List<Map<String, Object>> trackersArray = new ArrayList<>();
                while (trackedObjectIter.hasNext()) {
                    TrackedObject current = trackedObjectIter.next();
                    Map<String, Object> objectMap = new HashMap<>();
                    objectMap.put("id", current.getId());
                    objectMap.put("time", current.getTime());
                    objectMap.put("description", current.getDescription());
                    List<Map<String, Object>> coordinatesArray = new ArrayList<>();
                    for (int i = 0; i < current.getCoordinates().length; i++) {
                        Map<String, Object> axes = new HashMap<>();
                        axes.put("x", current.getCoordinates()[i].getX());
                        axes.put("y", current.getCoordinates()[i].getX());
                        coordinatesArray.add(axes);
                    }
                    objectMap.put("coordinates", coordinatesArray);
                    trackersArray.add(objectMap);
                }
                lidarsMap.put("LiDarWorkerTracker" + lidar.getId(), trackersArray);
            }
            output.put("lastLiDarWorkerTrackersFrame", lidarsMap);

            List<Map<String, Object>> posesArray = new ArrayList<>();
            Iterator<Pose> poseIterator = crashedBroadcast.getPoses().iterator();
            while (poseIterator.hasNext()) {
                Pose current = poseIterator.next();
                Map<String, Object> poseMap = new HashMap<>();
                poseMap.put("time", current.getTime());
                poseMap.put("x", current.getX());
                poseMap.put("y", current.getY());
                poseMap.put("yaw", current.getYaw());
                posesArray.add(poseMap);
            }
            output.put("poses", posesArray);
        }
        output.put("statistics",statistics);
        // Convert the statistics to JSON using Gson
        Gson gson = new Gson();
        String jsonString = gson.toJson(output);

        try {
            // Write the JSON data to the file, creating it if it doesn't exist
            Files.write(outputJsonPath, jsonString.getBytes());
            System.out.println("Statistics and Error logs successfully written to: " + outputJsonPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public static void main(String[] args) {
        System.out.println("Hello World!");
        final String CONFIGURATIONFILE = args[0];
        List<MicroService> servicesList = new ArrayList<>();

        //parse lidar JSON
        LiDarDataBase.getInstance(CONFIGURATIONFILE);


        //parse Cameras JSON and run cameras services
        List<Camera> camerasList = parseCamers(CONFIGURATIONFILE);
        for (Camera camera : camerasList){
            CameraService cameraService = new CameraService(camera);
            servicesList.add(cameraService);
        }

        //parseLidar JSON
        List<LiDarWorkerTracker> lidarsList =parseLidar(CONFIGURATIONFILE);
        for (LiDarWorkerTracker lidar : lidarsList){
            LiDarService lidarService = new LiDarService(lidar);
            servicesList.add(lidarService);
        }
        //parse GPSIMU JSON
        GPSIMU gps = parseGPSIMU(CONFIGURATIONFILE);
        PoseService poseService = new PoseService(gps);
        servicesList.add(poseService);

        //FusionSlam service
        FusionSlamService fusionSlamService = new FusionSlamService(FusionSlam.getInstance());
        servicesList.add(fusionSlamService);

        CountDownLatch latch = new CountDownLatch(servicesList.size());
        List<Thread> threadList = new ArrayList<>();
        for (MicroService service : servicesList){
            Thread t = new Thread(() -> {
                service.setLatch(latch);  // Notify when ready
                service.run();
            });
            t.start();
            threadList.add(t);
        }
            //parse Ticktime and Duration to create Timeservice and run time service
        TimeService timeService = parseTimeService(CONFIGURATIONFILE);
        timeService.setLatch(latch);  // Ensure the latch is also set for TimeService
        Thread timeThread = new Thread(() -> {
            try {
                latch.await();  // Wait for all services to be ready
                timeService.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        if (timeThread == null) {
            throw new IllegalStateException("TimeServiceThread is not initialized");
        }
        timeThread.start();


    //code to wait until all threads are done for the output file
        try {
            for (Thread thread : threadList) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    System.err.println("Thread interrupted while waiting for threadList: " + e);
                }
            }

            try {
                timeThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.err.println("Thread interrupted while waiting for timeServiceThread: " + e);
            }

        } catch (Exception e) {
            System.err.println("Unexpected error in main: " + e);
            e.printStackTrace();
        }


            //create output file
        Path outputfileParentPath = Paths.get(CONFIGURATIONFILE).getParent();
            if (!ErrorObject.getInstance().isCrashed()){
            createOutputFile(outputfileParentPath);
        }
        else {
            System.out.println("Error");
           createOutputFileWithErrors(outputfileParentPath , ErrorObject.getInstance());
        }
    }
}
