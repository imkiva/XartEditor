package com.imkiva.xart.eventbus;

import com.imkiva.xart.eventbus.api.IEventManager;

public class EventManager implements IEventManager {


    @Override
    public void sendEvent(Object event) {
        EventBus.getDefault().post(event);
    }

    @Override
    public void registerObserver(Object observer) {
        EventBus.getDefault().register(observer);
    }

    @Override
    public void unregisterObserver(Object observer) {
        EventBus.getDefault().unregister(observer);
    }
}
