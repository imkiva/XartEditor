/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Umeng, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.imkiva.xart.eventbus;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * the subscriber method hunter, find all of the subscriber's methods which
 * annotated with @Subcriber.
 *
 * @author mrsimple
 */
public class SubsciberMethodHunter {

    /**
     * the event bus's subscriber's map
     */
    Map<EventType, CopyOnWriteArrayList<Subscription>> mSubcriberMap;

    /**
     * @param subscriberMap
     */
    public SubsciberMethodHunter(Map<EventType, CopyOnWriteArrayList<Subscription>> subscriberMap) {
        mSubcriberMap = subscriberMap;
    }

    public void findSubcribeMethods(Object subscriber) {
        if (mSubcriberMap == null) {
            throw new NullPointerException("the mSubcriberMap is null. ");
        }
        Class<?> clazz = subscriber.getClass();
        while (clazz != null && !isSystemCalss(clazz.getName())) {
            final Method[] allMethods = clazz.getDeclaredMethods();
            for (int i = 0; i < allMethods.length; i++) {
                Method method = allMethods[i];
                Subscriber annotation = method.getAnnotation(Subscriber.class);
                if (annotation != null) {
                    Class<?>[] paramsTypeClass = method.getParameterTypes();
                    if (paramsTypeClass != null && paramsTypeClass.length == 1) {
                        Class<?> paramType = convertType(paramsTypeClass[0]);
                        EventType eventType = new EventType(paramType, annotation.tag());
                        TargetMethod subscribeMethod = new TargetMethod(method, eventType,
                                annotation.mode());
                        subscribe(eventType, subscribeMethod, subscriber);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void subscribe(EventType event, TargetMethod method, Object subscriber) {
        CopyOnWriteArrayList<Subscription> subscriptionLists = mSubcriberMap.get(event);
        if (subscriptionLists == null) {
            subscriptionLists = new CopyOnWriteArrayList<>();
        }

        Subscription newSubscription = new Subscription(subscriber, method);
        if (subscriptionLists.contains(newSubscription)) {
            return;
        }

        subscriptionLists.add(newSubscription);
        mSubcriberMap.put(event, subscriptionLists);
    }

    /**
     * remove subscriber methods from map
     *
     * @param subscriber
     */
    public void removeMethodsFromMap(Object subscriber) {
        Iterator<CopyOnWriteArrayList<Subscription>> iterator = mSubcriberMap
                .values().iterator();
        while (iterator.hasNext()) {
            CopyOnWriteArrayList<Subscription> subscriptions = iterator.next();
            if (subscriptions != null) {
                List<Subscription> foundSubscriptions = new
                        LinkedList<>();
                Iterator<Subscription> subIterator = subscriptions.iterator();
                while (subIterator.hasNext()) {
                    Subscription subscription = subIterator.next();
                    Object cacheObject = subscription.subscriber.get();
                    if (isObjectsEqual(cacheObject, subscriber)
                            || cacheObject == null) {
                        foundSubscriptions.add(subscription);
                    }
                }

                subscriptions.removeAll(foundSubscriptions);
            }

            if (subscriptions == null || subscriptions.size() == 0) {
                iterator.remove();
            }
        }
    }

    private boolean isObjectsEqual(Object cachedObj, Object subscriber) {
        return cachedObj != null
                && cachedObj.equals(subscriber);
    }

    /**
     * if the subscriber method's type is primitive, convert it to corresponding
     * Object type. for example, int to Integer.
     *
     * @param eventType origin Event Type
     * @return
     */
    private Class<?> convertType(Class<?> eventType) {
        Class<?> returnClass = eventType;
        if (eventType.equals(boolean.class)) {
            returnClass = Boolean.class;
        } else if (eventType.equals(int.class)) {
            returnClass = Integer.class;
        } else if (eventType.equals(float.class)) {
            returnClass = Float.class;
        } else if (eventType.equals(double.class)) {
            returnClass = Double.class;
        }

        return returnClass;
    }

    private boolean isSystemCalss(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.");
    }

}
