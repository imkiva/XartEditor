package com.imkiva.xart.eventbus.api;

public interface IEventManager {
    void sendEvent(Object event);

    void registerObserver(Object observer);

    void unregisterObserver(Object observer);
}
