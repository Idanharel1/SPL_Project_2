package bgu.spl.mics;

import bgu.spl.mics.application.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FusionSlam transformation logic
 */
public class FusionSlamTest {
    private FusionSlam fusionSlam;
    private TrackedObject trackedObject;
    private Pose pose;

    @BeforeEach
    public void setUp() {
        fusionSlam = FusionSlam.getInstance();
        // Create test data
        CloudPoint[] coordinates = new CloudPoint[]{
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        };
        trackedObject = new TrackedObject("test-object", 1, "Test Object", coordinates);
        pose = new Pose(0.0f, 0.0f, 90.0f, 1); // 90 degree rotation
    }

    @Test
    public void testTransformTrackedObjectToLandmark() {
        // Add test pose and tracked object
        fusionSlam.addPose(pose);
        ConcurrentLinkedQueue<TrackedObject> objects = new ConcurrentLinkedQueue<>();
        objects.add(trackedObject);
        fusionSlam.addTrackedObject(objects);

        // Get the transformed landmark
        LandMark landmark = null;
        for (LandMark lm : fusionSlam.getLandMarks()) {
            if (lm.getId().equals(trackedObject.getId())) {
                landmark = lm;
                break;
            }
        }

        // Verify transformation
        assertNotNull(landmark, "Landmark should be created");
        assertEquals(trackedObject.getId(), landmark.getId(), "Landmark ID should match tracked object ID");
        assertEquals(trackedObject.getDescription(), landmark.getDescription(), "Description should match");

        // Check coordinate transformation (90 degree rotation should swap x,y)
        CloudPoint firstPoint = landmark.getCoordinates().peek();
        assertEquals(-1.0, firstPoint.getY(), 0.01, "Y coordinate should be transformed correctly");
        assertEquals(1.0, firstPoint.getX(), 0.01, "X coordinate should be transformed correctly");
    }

    @Test
    public void testLandmarkAveraging() {
        // Test that when the same object is detected multiple times, coordinates are averaged
        fusionSlam.addPose(pose);

        // First detection
        ConcurrentLinkedQueue<TrackedObject> objects1 = new ConcurrentLinkedQueue<>();
        objects1.add(trackedObject);
        fusionSlam.addTrackedObject(objects1);

        // Second detection with slightly different coordinates
        CloudPoint[] coordinates2 = new CloudPoint[]{
                new CloudPoint(1.2, 1.2),
                new CloudPoint(2.2, 2.2)
        };
        TrackedObject trackedObject2 = new TrackedObject("test-object", 2, "Test Object", coordinates2);
        ConcurrentLinkedQueue<TrackedObject> objects2 = new ConcurrentLinkedQueue<>();
        objects2.add(trackedObject2);
        fusionSlam.addTrackedObject(objects2);

        // Verify the coordinates were averaged
        LandMark landmark = null;
        for (LandMark lm : fusionSlam.getLandMarks()) {
            if (lm.getId().equals(trackedObject.getId())) {
                landmark = lm;
                break;
            }
        }

        assertNotNull(landmark);
        CloudPoint avgPoint = landmark.getCoordinates().peek();
        assertEquals(-1.1, avgPoint.getY(), 0.01, "Y coordinate should be averaged");
        assertEquals(1.1, avgPoint.getX(), 0.01, "X coordinate should be averaged");
    }
}