package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
    private final int TICKTIME;
    private final int DURATION;
    private int tickCounter;
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
        /*
        this.subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminate) ->{
            if (terminate.getSenderId().equals("FusionSlam")){
                System.out.println("TimeService got terminated from "+ terminate.getSenderId());
                terminate();
            }
        });*/
        // Guaranteed delay before the first tick broadcast
        try {
            Thread.sleep(3000);  // Sleep for 1 second (adjust as needed)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("TimeService interrupted during startup delay");
        }
        for (int i = 0; i < this.DURATION; i++) { // && !isTerminated()
            this.tickCounter++;
            System.out.println("Time service sent tick " + this.tickCounter);
            this.sendBroadcast(new TickBroadcast(this.getName(),this.tickCounter));
            try {
                Thread.sleep(this.TICKTIME* 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if(!isTerminated()) {
            this.terminate();
            Thread.currentThread().interrupt();
        }
    }
}
