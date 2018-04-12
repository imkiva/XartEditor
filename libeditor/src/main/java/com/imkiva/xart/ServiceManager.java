package com.imkiva.xart;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager {

    private Map<Class<?>, Object> SERVICE_CACHE = new ConcurrentHashMap<Class<?>, Object>();
    private static final boolean THROW_EXCEPTION_ON_NOT_FOUND_SERVICE = true;

    private static class ServiceManagerHolder {
        static final ServiceManager INSTANCE = new ServiceManager();
    }

    public static ServiceManager get() {
        return ServiceManagerHolder.INSTANCE;
    }

    public void registerService(Class<?> serviceInterface, Object service) {
        if (serviceInterface != null && service != null && serviceInterface.isInterface()) {
            SERVICE_CACHE.put(serviceInterface, service);
        }
    }

    public void unregisterService(Class<?> serviceInterface) {
        SERVICE_CACHE.remove(serviceInterface);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<?> serviceInterface) {
        Object service = null;
        if (serviceInterface != null && serviceInterface.isInterface()) {
            service = SERVICE_CACHE.get(serviceInterface);
        }
        if (THROW_EXCEPTION_ON_NOT_FOUND_SERVICE) {
            if (service == null) {
                throw new RuntimeException("Cannot get this Service: " + serviceInterface);
            }
        }
        return (T) service;
    }
}
