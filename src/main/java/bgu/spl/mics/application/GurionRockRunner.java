package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.LiDarDataBase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;

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
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        System.out.println("Hello World!");
        final String CONFIGURATIONFILE = args[0];
        //parse lidar JSON
        LiDarDataBase.getInstance(CONFIGURATIONFILE);

        //parse Cameras JSON
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(CONFIGURATIONFILE)) {
            JsonObject jsonObject = gson.fromJson(reader , JsonObject.class);
            JsonObject name = jsonObject.get("Cameras").getAsJsonObject();
            System.out.println("$$$ " + name.get("CamerasConfigurations").getAsJsonArray().get(0).getAsJsonObject().get("frequency").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //parse GPSIMU JSON

        //parse Ticktime and Duration to create Timeservice

        //parse Frequencies per cameras / Lidar and create them

        //create all services and run them

        //create output file

        // TODO: Parse configuration file.
        // TODO: Initialize system components and services.
        // TODO: Start the simulation.

    }
}
