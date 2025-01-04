package bgu.spl.mics.application;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public static void parseCamers(String filePath){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
            JsonArray camerasToCreate = jsonObject.get("Cameras").getAsJsonObject().get("CamerasConfigurations").getAsJsonArray();
            Iterator<JsonElement> camerasJsonIter = camerasToCreate.iterator();
            while (camerasJsonIter.hasNext()){
                JsonElement currentCameraJson = camerasJsonIter.next();
                System.out.println(currentCameraJson.getAsJsonObject().get("id").getAsInt() + " " + currentCameraJson.getAsJsonObject().get("frequency").getAsInt());
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
                    CameraService cameraService = new CameraService(currentCamera);
                    cameraService.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void parseTimeService(String filePath){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
            TimeService timeService = new TimeService(jsonObject.get("TickTime").getAsInt() ,jsonObject.get("Duration").getAsInt() );
            timeService.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void parseLidar(String filepath){
        Gson gson = new Gson();
        try (FileReader configReader = new FileReader(filepath)) {
            JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
            JsonArray lidarObjects = configJson.get("LiDarWorkers").getAsJsonObject().get("LidarConfigurations").getAsJsonArray();
            for(JsonElement object : lidarObjects) {
                LiDarWorkerTracker lidar1 = new LiDarWorkerTracker(object.getAsJsonObject().get("id").getAsInt(), object.getAsJsonObject().get("frequency").getAsInt());
                LiDarService s1 = new LiDarService(lidar1);
                s1.run();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parseGPSIMU(String filepath){
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
        System.out.println(posepath);
        ConcurrentLinkedQueue<Pose> poseList = new ConcurrentLinkedQueue<Pose>();
        try (FileReader configReader = new FileReader(posepath)) {
            System.out.println(configReader.toString());
            JsonArray objects = gson.fromJson(configReader, JsonArray.class);
            for(JsonElement object : objects) {
                System.out.println(object.getAsJsonObject().get("time").getAsInt());
                Pose p1 = new Pose(object.getAsJsonObject().get("x").getAsFloat(), object.getAsJsonObject().get("y").getAsFloat(),
                        object.getAsJsonObject().get("yaw").getAsFloat(), object.getAsJsonObject().get("time").getAsInt());
                poseList.add(p1);
            }
            GPSIMU gps = new GPSIMU(0, poseList);
            PoseService p1 = new PoseService(gps);
            p1.run();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createOutputFile(Path parentPath){
        Path outputJsonPath = parentPath.resolve("output2.json");
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
    public static void main(String[] args) {
        System.out.println("Hello World!");
        final String CONFIGURATIONFILE = args[0];
        //parse lidar JSON
        LiDarDataBase.getInstance(CONFIGURATIONFILE);


        //parse Cameras JSON and run cameras services
        parseCamers(CONFIGURATIONFILE);
        //parseLidar JSON
        parseLidar(CONFIGURATIONFILE);
        //parse GPSIMU JSON
        parseGPSIMU(CONFIGURATIONFILE);
        //run FusionSlam service
        FusionSlamService fusionSlamService = new FusionSlamService(FusionSlam.getInstance());
        fusionSlamService.run();
        //parse Ticktime and Duration to create Timeservice and run time service
        parseTimeService(CONFIGURATIONFILE);

          //create output file

        try {
            while(!fusionSlamService.isTerminated()){
                Thread.sleep(3000);
            }
            Path outputfileParentPath = Paths.get(CONFIGURATIONFILE).getParent();
            createOutputFile(outputfileParentPath);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
