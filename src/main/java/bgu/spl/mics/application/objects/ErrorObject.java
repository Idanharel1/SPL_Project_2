package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.messages.CrashedBroadcast;

public class ErrorObject {
    private boolean isCrashed = false;
    private CrashedBroadcast crashedBroadcast;



    private static class ErrorHolder {
        private static final ErrorObject instance = new ErrorObject();
    }
    public static ErrorObject getInstance(){
        return ErrorObject.ErrorHolder.instance;
    }

    public boolean isCrashed() {
        return isCrashed;
    }

    public void setCrashed(boolean crashed) {
        isCrashed = crashed;
    }

    public CrashedBroadcast getCrashedBroadcast() {
        return crashedBroadcast;
    }

    public void setCrashedBroadcast(CrashedBroadcast crashedBroadcast) {
        this.crashedBroadcast = crashedBroadcast;
    }
}
