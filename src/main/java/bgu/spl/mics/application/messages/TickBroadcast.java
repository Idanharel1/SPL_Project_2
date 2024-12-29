package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {

    private String senderId;
    private final int tickCounter;

    public TickBroadcast(String senderId, int tickCounter) {

        this.senderId = senderId;
        this.tickCounter = tickCounter;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}
