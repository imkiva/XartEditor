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

/**
 * @author mrsimple
 */
public final class EventType {
    public static final String DEFAULT_TAG = "default_tag";

    Class<?> paramClass;

    public String tag = DEFAULT_TAG;

    public Object event;

    /**
     * @param aClass
     */
    public EventType(Class<?> aClass) {
        this(aClass, DEFAULT_TAG);
    }

    public EventType(Class<?> aClass, String aTag) {
        paramClass = aClass;
        tag = aTag;
    }

    @Override
    public String toString() {
        return "EventType [paramClass=" + paramClass.getName() + ", tag=" + tag + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((paramClass == null) ? 0 : paramClass.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventType other = (EventType) obj;
        if (paramClass == null) {
            if (other.paramClass != null)
                return false;
        } else if (!paramClass.equals(other.paramClass))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

}
