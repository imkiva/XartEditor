package com.imkiva.xart.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mrsimple
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscriber {
    String tag() default EventType.DEFAULT_TAG;

    ThreadMode mode() default ThreadMode.MAIN;
}
