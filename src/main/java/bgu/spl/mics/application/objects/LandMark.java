package bgu.spl.mics.application.objects;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private String id;
    private String description;
    private ConcurrentLinkedQueue<CloudPoint> Coordinates;

    public LandMark(String id, String description, ConcurrentLinkedQueue<CloudPoint> coordinates) {
        this.id = id;
        this.description = description;
        Coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConcurrentLinkedQueue<CloudPoint> getCoordinates() {
        return Coordinates;
    }

    public void setCoordinates(ConcurrentLinkedQueue<CloudPoint> coordinates) {
        Coordinates = coordinates;
    }

}
