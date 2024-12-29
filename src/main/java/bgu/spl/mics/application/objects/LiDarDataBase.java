package bgu.spl.mics.application.objects;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {
    private ConcurrentLinkedQueue<StampedCloudPoints> cloudPoints;

    private static class SingletonHolder{
        private static LiDarDataBase instance = new LiDarDataBase();
    }
    private LiDarDataBase(){
        this.cloudPoints = new ConcurrentLinkedQueue<>();
    }
    public static LiDarDataBase getInstance(){
        return SingletonHolder.instance;
    }

    public LiDarDataBase(ConcurrentLinkedQueue<StampedCloudPoints> cloudPoints) {
        this.cloudPoints = cloudPoints;
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


    public static LiDarDataBase getInstance(String filePath) {
        // TODO: Implement this // parseJSON to LIDARDB using filepath
        return null;
    }

}
