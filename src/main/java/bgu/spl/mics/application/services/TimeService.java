package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TickBroadcast;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
    private final int TICKTIME;
    private final int DURATION;
    private int tickCounter;
    private TickBroadcast tickBroadcast;
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
        this.tickBroadcast = new TickBroadcast(this.getName());
    }

    public int getTickCounter() {
        return tickCounter;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() throws InterruptedException {
        for (int i = 0; i < this.DURATION; i++) {
            this.sendBroadcast(this.tickBroadcast);
            this.tickCounter++;
            Thread.sleep(this.TICKTIME);
            }
        this.terminate();
        Thread.currentThread().interrupt();
    }
}
