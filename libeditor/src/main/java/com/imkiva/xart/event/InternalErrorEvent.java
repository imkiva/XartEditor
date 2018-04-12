package com.imkiva.xart.event;

public class InternalErrorEvent {
    private Throwable error;

    public InternalErrorEvent(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }
}
