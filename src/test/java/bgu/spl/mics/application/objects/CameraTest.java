package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for Camera data preparation and error handling
 Class Invariants:
 * - camera instance is never null
 * - camera.id is positive number
 * - camera.frequency is >= 0
 * - camera.detectedObjectsList is never null
 * - All StampedDetectedObjects in detectedObjectsList have non-null DetectedObject lists
 */

public class CameraTest {
    private Camera camera;
    private StampedDetectedObjects stampedObjects;

    @BeforeEach
    public void setUp() {
        camera = new Camera(1 , 2);
        // Create test detected objects
        ConcurrentLinkedQueue<DetectedObject> objects = new ConcurrentLinkedQueue<>();
        objects.add(new DetectedObject("test-id", "Test Object"));
        stampedObjects = new StampedDetectedObjects(1);
        stampedObjects.setDetectedObjectsList(objects);
    }
    // @PRE-CONDITION: Camera exists with empty detected objects list
    // @POST-CONDITION: Test data successfully added and retrievable by timestamp
    @Test
    public void testPrepareDataForSending() {
        // Add test data to camera
        camera.addDetectedObjectsList(stampedObjects);

        // Test getting stamped objects at specific time
        StampedDetectedObjects result = camera.getStampedByTime(1);

        assertNotNull(result, "Should return stamped objects for valid time");
        assertEquals(1, result.getTime(), "Time stamp should match");
        assertFalse(result.getDetectedObjectsList().isEmpty(), "Should contain detected objects");

        DetectedObject firstObject = result.getDetectedObjectsList().peek();
        assertEquals("test-id", firstObject.getId(), "Object ID should match");
        assertEquals("Test Object", firstObject.getDescription(), "Description should match");
    }

    // @PRE-CONDITION: Camera exists with UP status
    // @POST-CONDITION: Error object successfully stored and retrievable
    @Test
    public void testErrorDetection() {
        // Create error object
        ConcurrentLinkedQueue<DetectedObject> objects = new ConcurrentLinkedQueue<>();
        objects.add(new DetectedObject("ERROR", "Camera Error"));
        StampedDetectedObjects errorStamp = new StampedDetectedObjects(2);
        errorStamp.setDetectedObjectsList(objects);

        camera.addDetectedObjectsList(errorStamp);

        // Verify time-stamped error object
        StampedDetectedObjects result = camera.getStampedByTime(2);
        assertNotNull(result, "Should return error stamped objects");
        DetectedObject errorObj = result.getDetectedObjectsList().peek();
        assertEquals("ERROR", errorObj.getId(), "Should identify as error object");

        // Verify initial camera status
        assertEquals(STATUS.UP, camera.getStatus(), "Camera should initially be UP");
    }

    // @PRE-CONDITION: Camera exists with frequency of 2
    // @POST-CONDITION: Objects only retrievable at frequency-aligned timestamps
    @Test
    public void testFrequencyHandling() {
        // Test that camera respects its frequency setting
        Camera frequencyCamera = new Camera(1 , 2);
        assertEquals(2, frequencyCamera.getFrequency(), "Should have correct frequency");

        // Add objects at different times
        StampedDetectedObjects objects1 = new StampedDetectedObjects(1);
        objects1.setDetectedObjectsList(new ConcurrentLinkedQueue<>());
        frequencyCamera.addDetectedObjectsList(objects1);

        // Should return null for time not matching frequency
        assertNull(frequencyCamera.getStampedByTime(2),
                "Should not return objects for non-frequency-aligned time");

        // Should return objects for time matching frequency
        StampedDetectedObjects objects3 = new StampedDetectedObjects(3);
        objects3.setDetectedObjectsList(new ConcurrentLinkedQueue<>());
        frequencyCamera.addDetectedObjectsList(objects3);
        assertNotNull(frequencyCamera.getStampedByTime(3),
                "Should return objects for frequency-aligned time");
    }
}