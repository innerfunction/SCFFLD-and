// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.util;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * A queue for running tasks on a background thread.
 * A queue must be started before tasks are processed. Normally, the queue is automatically started
 * when created using the the standard constructor; however, queues can also be created in a
 * non-started state, and these queues must be manually started by a call to start() before tasks
 * will be processed.
 *
 * Attached by juliangoacher on 07/05/16.
 */
public class RunQueue extends LinkedBlockingQueue<Runnable> {

    static final String Tag = RunQueue.class.getSimpleName();

    /** An enumeration of queue starting modes. */
    public enum StartMode {
        /** Start mode auto: The queue is automatically started when created. */
        Auto,
        /** Start mode manual: The queue must be explicitly started after creation. */
        Manual
    };

    private Thread runThread;
    private String name;

    /**
     * Create a new queue with the specified name.
     * Creates a started queue which will automatically start processing any tasks added to it.
     * @param name
     */
    public RunQueue(String name) {
        this( name, StartMode.Auto );
    }

    /**
     * Create a new queue with the specified name and start mode.
     * @param name      A name for the queue thread.
     * @param startMode Whether the queue is started when created, or must be manually started after.
     */
    public RunQueue(String name, StartMode startMode) {
        this.name = String.format("RQ:%s", name );
        runThread = new Thread(new Runnable() {
            public void run() {
                while( true ) {
                    try {
                        Runnable next = take();
                        next.run();
                    }
                    catch(Exception e) {
                        Log.e( Tag, "Running task", e );
                    }
                }
            }
        }, this.name );
        if( startMode == StartMode.Auto ) {
            runThread.start();
        }
    }

    /**
     * Start the queue if not already started.
     */
    public void start() {
        if( !runThread.isAlive() ) {
            runThread.start();
        }
    }
    public boolean dispatch(Runnable runnable) {
        boolean ok = true;
        try {
            put( runnable );
        }
        catch(Exception e) {
            Log.w( Tag, "Dispatching task", e );
            ok = false;
        }
        return ok;
    }

    /**
     * Test whether the current thread is the queue's execution thread.
     * @return true if the current thread is the same as the queue's execution thread.
     */
    public boolean isRunningOnQueueThread() {
        return Thread.currentThread() == runThread;
    }

}
