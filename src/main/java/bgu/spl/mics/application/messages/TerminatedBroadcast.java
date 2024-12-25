package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast {

    private String senderId;

    public TerminatedBroadcast(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}
