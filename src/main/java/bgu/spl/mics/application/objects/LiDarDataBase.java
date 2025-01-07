package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {
    private ConcurrentLinkedQueue<StampedCloudPoints> cloudPoints;
    private final boolean initialized; //to make sure lidar database is initialized only once

    private static class SingletonHolder{
        private static final LiDarDataBase instance = new LiDarDataBase();
    }

    private LiDarDataBase() {
        this.cloudPoints = new ConcurrentLinkedQueue<>();
        this.initialized = false;
    }

    public static LiDarDataBase getInstance(){
        return SingletonHolder.instance;
    }

    public ConcurrentLinkedQueue<StampedCloudPoints> getCloudPoints() {
        return cloudPoints;
    }

    public void setCloudPoints(ConcurrentLinkedQueue<StampedCloudPoints> cloudPoints) {
        this.cloudPoints = cloudPoints;
    }

    public StampedCloudPoints getCloudPointById (String id){
        StampedCloudPoints result = null;
        for(StampedCloudPoints object : this.getCloudPoints()){
            if (object.getId().equals(id)) {
                result = object;
                break;
            }
        }
        return  result;
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */

    public boolean isFinishedReading (int currentTimeMinusFrequency){
        boolean isFInishedReading = true;
        for (StampedCloudPoints stampedCloudPoint : this.getCloudPoints() ){
            if (stampedCloudPoint.getTime() >= currentTimeMinusFrequency){
                isFInishedReading = false;
            }
        }
        return isFInishedReading;
    }
    public boolean isErrorInTime (int currentTimeMinusFrequency) {
        boolean isErrorInTime = false;
        for (StampedCloudPoints stampedCloudPoint : this.getCloudPoints() ){
            if ((stampedCloudPoint.getTime() == currentTimeMinusFrequency) && (stampedCloudPoint.getId() == "ERROR")){
                isErrorInTime = true;
            }
        }
        return isErrorInTime;
    }
        public static LiDarDataBase getInstance(String filePath) {
        LiDarDataBase instance = getInstance();
        if (instance.initialized) {
            throw new IllegalStateException("LiDarDataBase has already been initialized.");
        }
        Gson gson = new Gson();
        String lidarPath = "";
        try (FileReader configReader = new FileReader(filePath)) {
            JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
            lidarPath = configJson.get("LiDarWorkers").getAsJsonObject().get("lidars_data_path").getAsString();
            Path configPath = Paths.get(filePath).getParent();
            lidarPath = configPath.resolve(lidarPath).toString();
            try (FileReader lidarReader = new FileReader(lidarPath)) {
                JsonArray lidarJson = gson.fromJson(lidarReader, JsonArray.class);

                Iterator jsonIter = lidarJson.iterator();
                while (jsonIter.hasNext()){
                    JsonObject object = (JsonObject) jsonIter.next();


                    ArrayList<CloudPoint> cloudPointsList = new ArrayList<CloudPoint>();
                    for (JsonElement cloudPointArray : object.get("cloudPoints").getAsJsonArray()){
                        double x = cloudPointArray.getAsJsonArray().get(0).getAsDouble();
                        double y = cloudPointArray.getAsJsonArray().get(1).getAsDouble();
                        cloudPointsList.add(new CloudPoint(x,y));
                    }
                    CloudPoint[] cloudPointsArray = new CloudPoint[cloudPointsList.size()];
                    for (int i = 0; i < cloudPointsArray.length; i++) {
                        cloudPointsArray[i] = cloudPointsList.get(i);
                    }
                    instance.cloudPoints.add(new StampedCloudPoints(object.get("id").getAsString(), object.get("time").getAsInt(), cloudPointsArray));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instance;
    }

}
