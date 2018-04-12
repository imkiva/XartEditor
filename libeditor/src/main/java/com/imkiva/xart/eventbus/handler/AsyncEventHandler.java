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

package com.imkiva.xart.eventbus.handler;

import android.os.Handler;
import android.os.HandlerThread;

import com.imkiva.xart.eventbus.Subscription;


/**
 * @author mrsimple
 */
public class AsyncEventHandler implements EventHandler {

    DispatcherThread mDispatcherThread;

    EventHandler mEventHandler = new DefaultEventHandler();

    public AsyncEventHandler() {
        mDispatcherThread = new DispatcherThread(AsyncEventHandler.class.getSimpleName());
        mDispatcherThread.start();
    }

    public void handleEvent(final Subscription subscription, final Object event) {
        mDispatcherThread.post(() -> mEventHandler.handleEvent(subscription, event));
    }

    /**
     * @author mrsimple
     */
    class DispatcherThread extends HandlerThread {

        protected Handler mAsyncHandler;

        /**
         * @param name
         */
        public DispatcherThread(String name) {
            super(name);
        }

        /**
         * @param runnable
         */
        public void post(Runnable runnable) {
            if (mAsyncHandler == null) {
                throw new NullPointerException("mAsyncHandler == null, please call start() first.");
            }

            mAsyncHandler.post(runnable);
        }

        @Override
        public synchronized void start() {
            super.start();
            mAsyncHandler = new Handler(this.getLooper());
        }

    }

}
