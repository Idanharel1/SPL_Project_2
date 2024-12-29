package bgu.spl.mics.application.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;

public class testJsonRead {
        public static void main(String[] args) {
            System.out.println("Hello");

//            Gson gson = new Gson();
//            try (FileReader reader = new FileReader("example input/configuration_file.json")) {
//                JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
//                JsonObject name = jsonObject.get("Cameras").getAsJsonObject();
//                System.out.println("$$$ " + name.get("CamerasConfigurations").getAsJsonArray().get(0).getAsJsonObject().get("frequency").toString());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            Gson gson = new Gson();
            String camerasPath = "";
            String CONFIGFILEPATH = "example input/configuration_file.json";
            //configuration file argument supposed to be taken from main
            try (FileReader configReader = new FileReader(CONFIGFILEPATH)) {
                JsonObject configJson = gson.fromJson(configReader, JsonObject.class);
                camerasPath = configJson.get("Cameras").getAsJsonObject().get("camera_datas_path").getAsString();

                try (FileReader camerasReader = new FileReader(camerasPath)) {
                    JsonObject camerasJson = gson.fromJson(camerasReader, JsonObject.class);
//                    camerasPath = camerasJson.get().getAsJsonObject().get("camera_datas_path").getAsString();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


}
