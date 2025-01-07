package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
    private final int TICKTIME;
    private final int DURATION;
    private int tickCounter;
    private ExecutorService executorService;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration) {
        super("TimeService");
        this.TICKTIME = TickTime;
        this.DURATION = Duration;
        this.tickCounter = 0;
        this.executorService = Executors.newSingleThreadExecutor(); // Single-thread executor for ticking


    }

    public int getTickCounter() {
        return tickCounter;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {

        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if (terminate.getSenderId().equals("FusionSlam")){
                System.out.println("TimeService got terminated from "+ terminate.getSenderId());
                terminateService();
            }
        });

        this.subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) ->{
            if (crashed.getSenderId().equals("FusionSlam")){
                System.out.println("TimeService got terminated from "+ crashed.getSenderId());
                terminateService();
            }
        });
        executorService.submit(this::startTick);
    }
    private void startTick(){

        // Guaranteed delay before the first tick broadcast
        try {
            Thread.sleep(3000);  // Sleep for 1 second (adjust as needed)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("TimeService interrupted during startup delay");
        }
        try {
            for (int i = 0; i < this.DURATION; i++) { // && !isTerminated()
            this.tickCounter++;
            System.out.println("Time service sent tick " + this.tickCounter);
            this.sendBroadcast(new TickBroadcast(this.getName(),this.tickCounter));

                Thread.sleep(this.TICKTIME* 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            terminateService(); // Ensure termination after the ticking is complete
        }
    }
    private void terminateService() {
        // Terminate the service and shut down the executor
        this.terminate();
        executorService.shutdownNow(); // Stop the ticking thread
    }
}
