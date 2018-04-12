/*
 * Copyright (C) 2015 Mr.Simple <bboyfeiyu@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imkiva.xart.eventbus;

import android.util.Log;

import com.imkiva.xart.eventbus.handler.AsyncEventHandler;
import com.imkiva.xart.eventbus.handler.DefaultEventHandler;
import com.imkiva.xart.eventbus.handler.EventHandler;
import com.imkiva.xart.eventbus.handler.UIThreadEventHandler;
import com.imkiva.xart.eventbus.matchpolicy.DefaultMatchPolicy;
import com.imkiva.xart.eventbus.matchpolicy.MatchPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author mrsimple
 */
public final class EventBus {

    /**
     * default descriptor
     */
    private static final String DESCRIPTOR = EventBus.class.getSimpleName();

    private String mDesc = DESCRIPTOR;

    /**
     * EventType-Subcriptions map
     */
    private final Map<EventType, CopyOnWriteArrayList<Subscription>> mSubcriberMap = new ConcurrentHashMap<EventType, CopyOnWriteArrayList<Subscription>>();
    /**
     *
     */
    private List<EventType> mStickyEvents = Collections
            .synchronizedList(new LinkedList<EventType>());
    /**
     * the thread local event queue, every single thread has it's own queue.
     */
    ThreadLocal<Queue<EventType>> mLocalEvents = new ThreadLocal<Queue<EventType>>() {
        protected Queue<EventType> initialValue() {
            return new ConcurrentLinkedQueue<EventType>();
        }

        ;
    };

    /**
     * the event dispatcher
     */
    EventDispatcher mDispatcher = new EventDispatcher();

    /**
     * the subscriber method hunter, find all of the subscriber's methods
     * annotated with @Subcriber
     */
    SubsciberMethodHunter mMethodHunter = new SubsciberMethodHunter(mSubcriberMap);

    /**
     * The Default EventBus instance
     */
    private static EventBus sDefaultBus;

    /**
     * private Constructor
     */
    private EventBus() {
        this(DESCRIPTOR);
    }

    /**
     * constructor with desc
     *
     * @param desc the descriptor of eventbus
     */
    public EventBus(String desc) {
        mDesc = desc;
    }

    /**
     * @return Default EventBUS
     */
    public static EventBus getDefault() {
        if (sDefaultBus == null) {
            synchronized (EventBus.class) {
                if (sDefaultBus == null) {
                    sDefaultBus = new EventBus();
                }
            }
        }
        return sDefaultBus;
    }

    /**
     * register a subscriber into the mSubcriberMap, the key is subscriber's
     * method's name and tag which annotated with {@see Subcriber}, the value is
     * a list of Subscription.
     *
     * @param subscriber the target subscriber
     */
    public void register(Object subscriber) {
        if (subscriber == null) {
            return;
        }

        synchronized (this) {
            mMethodHunter.findSubcribeMethods(subscriber);
        }
    }

    /**
     * register as sticky
     *
     * @param subscriber
     */
    public void registerSticky(Object subscriber) {
        this.register(subscriber);
        mDispatcher.dispatchStickyEvents(subscriber);
    }

    /**
     * @param subscriber
     */
    public void unregister(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        synchronized (this) {
            mMethodHunter.removeMethodsFromMap(subscriber);
        }
    }

    /**
     * post a event
     *
     * @param event
     */
    public void post(Object event) {
        post(event, EventType.DEFAULT_TAG);
    }

    /**
     * send an event
     *
     * @param event
     * @param tag
     */
    public void post(Object event, String tag) {
        if (event == null) {
            Log.e(this.getClass().getSimpleName(), "The event object is null");
            return;
        }
        mLocalEvents.get().offer(new EventType(event.getClass(), tag));
        mDispatcher.dispatchEvents(event);
    }

    /**
     * send a sticky event whose type is EventType.DEFAULT_TAG
     *
     * @param event
     */
    public void postSticky(Object event) {
        postSticky(event, EventType.DEFAULT_TAG);
    }

    public void postSticky(Object event, String tag) {
        if (event == null) {
            Log.e(this.getClass().getSimpleName(), "The event object is null");
            return;
        }
        EventType eventType = new EventType(event.getClass(), tag);
        eventType.event = event;
        mStickyEvents.add(eventType);
    }

    public void removeStickyEvent(Class<?> eventClass) {
        removeStickyEvent(eventClass, EventType.DEFAULT_TAG);
    }

    /**
     * remove sticky event
     */
    public void removeStickyEvent(Class<?> eventClass, String tag) {
        Iterator<EventType> iterator = mStickyEvents.iterator();
        while (iterator.hasNext()) {
            EventType eventType = iterator.next();
            if (eventType.paramClass.equals(eventClass)
                    && eventType.tag.equals(tag)) {
                iterator.remove();
            }
        }
    }

    public List<EventType> getStickyEvents() {
        return mStickyEvents;
    }

    public void setMatchPolicy(MatchPolicy policy) {
        mDispatcher.mMatchPolicy = policy;
    }

    public void setUIThreadEventHandler(EventHandler handler) {
        mDispatcher.mUIThreadEventHandler = handler;
    }

    public void setPostThreadHandler(EventHandler handler) {
        mDispatcher.mPostThreadHandler = handler;
    }

    public void setAsyncEventHandler(EventHandler handler) {
        mDispatcher.mAsyncEventHandler = handler;
    }

    public Map<EventType, CopyOnWriteArrayList<Subscription>> getSubscriberMap() {
        return mSubcriberMap;
    }

    public Queue<EventType> getEventQueue() {
        return mLocalEvents.get();
    }

    public synchronized void clear() {
        mLocalEvents.get().clear();
        mSubcriberMap.clear();
    }

    public String getDescriptor() {
        return mDesc;
    }

    public EventDispatcher getDispatcher() {
        return mDispatcher;
    }

    private class EventDispatcher {
        EventHandler mUIThreadEventHandler = new UIThreadEventHandler();

        EventHandler mPostThreadHandler = new DefaultEventHandler();

        EventHandler mAsyncEventHandler = new AsyncEventHandler();

        private Map<EventType, List<EventType>> mCacheEventTypes = new ConcurrentHashMap<EventType, List<EventType>>();
        MatchPolicy mMatchPolicy = new DefaultMatchPolicy();

        void dispatchEvents(Object aEvent) {
            Queue<EventType> eventsQueue = mLocalEvents.get();
            while (eventsQueue.size() > 0) {
                deliveryEvent(eventsQueue.poll(), aEvent);
            }
        }

        private void deliveryEvent(EventType type, Object aEvent) {
            List<EventType> eventTypes = getMatchedEventTypes(type, aEvent);
            for (EventType eventType : eventTypes) {
                handleEvent(eventType, aEvent);
            }
        }

        private void handleEvent(EventType eventType, Object aEvent) {
            List<Subscription> subscriptions = mSubcriberMap.get(eventType);
            if (subscriptions == null) {
                return;
            }

            for (Subscription subscription : subscriptions) {
                final ThreadMode mode = subscription.threadMode;
                EventHandler eventHandler = getEventHandler(mode);
                eventHandler.handleEvent(subscription, aEvent);
            }
        }

        private List<EventType> getMatchedEventTypes(EventType type, Object aEvent) {
            List<EventType> eventTypes = null;
            if (mCacheEventTypes.containsKey(type)) {
                eventTypes = mCacheEventTypes.get(type);
            } else {
                eventTypes = mMatchPolicy.findMatchEventTypes(type, aEvent);
                mCacheEventTypes.put(type, eventTypes);
            }

            return eventTypes != null ? eventTypes : new ArrayList<EventType>();
        }

        void dispatchStickyEvents(Object subscriber) {
            for (EventType eventType : mStickyEvents) {
                handleStickyEvent(eventType, subscriber);
            }
        }

        private void handleStickyEvent(EventType eventType, Object subscriber) {
            List<EventType> eventTypes = getMatchedEventTypes(eventType, eventType.event);
            Object event = eventType.event;
            for (EventType foundEventType : eventTypes) {
                final List<Subscription> subscriptions = mSubcriberMap.get(foundEventType);
                if (subscriptions == null) {
                    continue;
                }
                for (Subscription subItem : subscriptions) {
                    final ThreadMode mode = subItem.threadMode;
                    EventHandler eventHandler = getEventHandler(mode);
                    if (isTarget(subItem, subscriber)
                            && (subItem.eventType.equals(foundEventType)
                            || subItem.eventType.paramClass
                            .isAssignableFrom(foundEventType.paramClass))) {
                        eventHandler.handleEvent(subItem, event);
                    }
                }
            }
        }

        private boolean isTarget(Subscription item, Object subscriber) {
            Object cacheObject = item.subscriber != null ? item.subscriber.get() : null;
            return subscriber == null || (cacheObject != null && cacheObject.equals(subscriber));
        }

        private EventHandler getEventHandler(ThreadMode mode) {
            if (mode == ThreadMode.ASYNC) {
                return mAsyncEventHandler;
            }
            if (mode == ThreadMode.POST) {
                return mPostThreadHandler;
            }
            return mUIThreadEventHandler;
        }
    }
}
