package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FusionSlam transformation logic
 * FusionSlamTest Class
 * Class Invariants:
 * - fusionSlam instance is never null and is a singleton
 * - trackedObject coordinates are never null
 * - pose values (x,y,yaw) are valid numbers
 * - landMarks collection in fusionSlam is never null
 * - trackedObject.time >= 0
 * - All CloudPoints have valid coordinates
 */


public class FusionSlamTest {
    private FusionSlam fusionSlam;
    private TrackedObject trackedObject;
    private TrackedObject trackedObject2;
    private TrackedObject trackedObject3;

    private Pose pose1;
    private Pose pose2;
    private Pose pose3;


    @BeforeEach
    public void setUp() {
        fusionSlam = FusionSlam.getInstance();
        CloudPoint[] coordinates = new CloudPoint[]{
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        };

        CloudPoint[] coordinates2 = new CloudPoint[]{
                new CloudPoint(1.2, 1.2),
                new CloudPoint(2.2, 2.2)
        };

        trackedObject = new TrackedObject("test-object1", 1, "Test Object", coordinates);
        trackedObject2 = new TrackedObject("test-object", 2, "Test Object", coordinates);
        trackedObject3 = new TrackedObject("test-object", 3, "Test Object", coordinates2);

        pose1 = new Pose(4.0f, 0.0f, 90.0f, 1); // 90 degree rotation
        pose2 = new Pose(4.0f, 0.0f, 90.0f, 2); // 90 degree rotation
        pose3 = new Pose(4.0f, 0.0f, 90.0f, 3); // 90 degree rotation


    }

    // @PRE-CONDITION: Good TrackedObject and Pose while no landmark exists for test object ID
    // @POST-CONDITION: New landmark exists with the correct coordinates
    @Test
    public void testTransformTrackedObjectToLandmark() {
        fusionSlam.addPose(pose1);
        ConcurrentLinkedQueue<TrackedObject> objects = new ConcurrentLinkedQueue<>();
        objects.add(trackedObject);
        fusionSlam.addTrackedObject(objects);

        LandMark landmark = null;
        for (LandMark lm : fusionSlam.getLandMarks()) {
            if (lm.getId().equals(trackedObject.getId())) {
                landmark = lm;
                break;
            }
        }

        assertNotNull(landmark, "Landmark should be created");
        assertEquals(trackedObject.getId(), landmark.getId(), "Landmark ID should match tracked object ID");
        assertEquals(trackedObject.getDescription(), landmark.getDescription(), "Description should match");

        CloudPoint firstPoint = landmark.getCoordinates().peek();
        assertEquals(1.0, firstPoint.getY(), 0.01, "Y coordinate should be transformed correctly");
        assertEquals(3.0, firstPoint.getX(), 0.01, "X coordinate should be transformed correctly");
    }

    // @PRE-CONDITION: Two TrackedObjects exist with same ID but different coordinates
    // @POST-CONDITION: Single landmark exists with averaged coordinates
    @Test
    public void testLandmarkAveraging() {
        fusionSlam.addPose(pose2);
        fusionSlam.addPose(pose3);

        ConcurrentLinkedQueue<TrackedObject> objects1 = new ConcurrentLinkedQueue<>();
        objects1.add(trackedObject2);
        fusionSlam.addTrackedObject(objects1);

        ConcurrentLinkedQueue<TrackedObject> objects2 = new ConcurrentLinkedQueue<>();
        objects2.add(trackedObject3);
        fusionSlam.addTrackedObject(objects2);

        LandMark landmark = null;
        for (LandMark lm : fusionSlam.getLandMarks()) {
            if (lm.getId().equals(trackedObject2.getId())) {
                landmark = lm;
                break;
            }
        }

        assertNotNull(landmark);
        CloudPoint avgPoint = landmark.getCoordinates().peek();
        assertEquals(1.1, avgPoint.getY(), 0.01, "Y coordinate should be averaged");
        assertEquals(2.9, avgPoint.getX(), 0.01, "X coordinate should be averaged");
    }
}