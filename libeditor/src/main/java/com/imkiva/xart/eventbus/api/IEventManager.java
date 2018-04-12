package com.imkiva.xart.eventbus.api;

public interface IEventManager {
    /**
     * 发送一个事件
     *
     * @param event 事件
     */
    void sendEvent(Object event);

    /**
     * 注册一个事件接受者
     *
     * @param observer 事件接受者
     */
    void registerObserver(Object observer);

    /**
     * 反注册一个事件接受者
     *
     * @param observer 事件接受者
     */
    void unregisterObserver(Object observer);
}
