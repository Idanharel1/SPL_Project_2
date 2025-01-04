package bgu.spl.mics.application.services;

import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.StampedCloudPoints;
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

public class testJsonRead {
        public static void main(String[] args) {
            System.out.println("Hello");

            Gson gson = new Gson();
            try (FileReader reader = new FileReader("example input/configuration_file.json")) {
                JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
                JsonObject name = jsonObject.get("Cameras").getAsJsonObject();
                System.out.println("$$$ " + name.get("CamerasConfigurations").getAsJsonArray().get(0).getAsJsonObject().get("frequency").toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Gson gson = new Gson();
            String lidarPath = "";
            String camerasPath = "";
            String CONFIGFILEPATH = "example input/configuration_file.json";

            //configuration file argument supposed to be taken from main
//            try (FileReader configReader = new FileReader(CONFIGFILEPATH)) {
//                JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
//                camerasPath = configJson.get("Cameras").getAsJsonObject().get("camera_datas_path").getAsString();
//                Path configPath = Paths.get(CONFIGFILEPATH).getParent();
//                camerasPath = configPath.resolve(camerasPath).toString();
////                camerasPath = Paths.get(camerasPath).normalize().toAbsolutePath().toString();
//
//                try (FileReader camerasReader = new FileReader(camerasPath)) {
//                    JsonObject camerasJson = gson.fromJson(camerasReader, JsonObject.class);
////                    camerasPath = camerasJson.get().getAsJsonObject().get("camera_datas_path").getAsString();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

//        }
//            ConcurrentLinkedQueue<StampedCloudPoints> cloudPoints = new ConcurrentLinkedQueue<>();
//            try (FileReader configReader = new FileReader(CONFIGFILEPATH)) {
//                JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
//                lidarPath = configJson.get("LiDarWorkers").getAsJsonObject().get("lidars_data_path").getAsString();
//                Path configPath = Paths.get(CONFIGFILEPATH).getParent();
//                lidarPath = configPath.resolve(lidarPath).toString();
//                try (FileReader lidarReader = new FileReader(lidarPath)) {
//                    JsonArray lidarJson = gson.fromJson(lidarReader, JsonArray.class);
//
//                    Iterator jsonIter = lidarJson.iterator();
//                    System.out.println(lidarJson.toString());
//                    while (jsonIter.hasNext()){
//                        JsonObject object = (JsonObject) jsonIter.next();
//
//
//                        ArrayList<CloudPoint> cloudPointsList = new ArrayList<CloudPoint>();
//                        for (JsonElement cloudPointArray : object.get("cloudPoints").getAsJsonArray()){
//                            double x = cloudPointArray.getAsJsonArray().get(0).getAsDouble();
//                            double y = cloudPointArray.getAsJsonArray().get(1).getAsDouble();
//                            cloudPointsList.add(new CloudPoint(x,y));
//                        }
//                        CloudPoint[] cloudPointsArray = new CloudPoint[cloudPointsList.size()];
//                        for (int i = 0; i < cloudPointsArray.length; i++) {
//                            cloudPointsArray[i] = cloudPointsList.get(i);
//                        }
//                      cloudPoints.add(new StampedCloudPoints(object.get("id").getAsString(), object.get("time").getAsInt(), cloudPointsArray));
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            StampedCloudPoints s = cloudPoints.peek();
//            System.out.println(s.getId() + " " + s.getTime() + " " + s.getCloudPoints()[0].getX() + " " + s.getCloudPoints()[0].getY());
//
            LiDarDataBase lid = LiDarDataBase.getInstance(CONFIGFILEPATH);
            System.out.println(lid.getCloudPointById("Wall_3").getCloudPoints()[1].getY());
        }


}
