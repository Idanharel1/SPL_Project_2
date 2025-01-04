package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private AtomicInteger systemRuntime;
    private AtomicInteger numDetectedObjects;
    private AtomicInteger numTrackedObjects;
    private AtomicInteger numLandmarks;

    private static class SingletonHolder{
        private static final StatisticalFolder instance = new StatisticalFolder();
    }
    public static StatisticalFolder getInstance(){
        return StatisticalFolder.SingletonHolder.instance;
    }

    public AtomicInteger getSystemRuntime() {
        return systemRuntime;
    }

    public void setSystemRuntime(AtomicInteger systemRuntime) {
        this.systemRuntime = systemRuntime;
    }

    public AtomicInteger getNumDetectedObjects() {
        return numDetectedObjects;
    }

    public void addDetectedObjects(int addition) { this.numDetectedObjects.addAndGet(addition); }

    public AtomicInteger getNumTrackedObjects() {
        return numTrackedObjects;
    }

    public void addTrackedObjects(int addition) {
        this.numTrackedObjects.addAndGet(addition);
    }

    public AtomicInteger getNumLandmarks() {
        return numLandmarks;
    }

    public void compareAndSetNumLandmarks(int oldVal, int newVal) {
        this.numLandmarks.compareAndSet(oldVal, newVal);
    }
}
