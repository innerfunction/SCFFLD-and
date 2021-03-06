// Copyright 2017 InnerFunction Ltd.
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
package com.innerfunction.scffld.app;

import android.app.Activity;
import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for automatically matching activity results to waiting requests.
 * To use the class, client code must implement the Callback interface and then
 * register a callback instance in exchange for a request code, before launching
 * an activity via startActivityForResult(..). This class will then match the
 * request code to the callback when an activity result is received, and then
 * call a method on the callback interface appopriate for the result.
 */
public class ActivityResult {

    /** The singleton instance of this class. */
    static final ActivityResult Manager = new ActivityResult();

    /**
     * An interface to be implemented by activity result handlers.
     */
    public interface Callback {
        /**
         * Handle a successful activity result.
         * @param code      The result code; normally RESULT_OK unless custom codes are being used.
         * @param result    The result returned by the child activity.
         */
        void onOk(int code, Intent result);
        /**
         * Handle a cancelled child activity.
         * Called when the activity result code is RESULT_CANCELED.
         */
        void onCancelled();

    }

    /**
     * A map of registered callbacks which are still waiting for a result.
     * The map is keyed by request code.
     */
    private Map<Integer,Callback> pendingCallbacks = new HashMap<>();

    private ActivityResult() {}

    /**
     * Register a callback.
     * This method should be called before launching a child activity via
     * startActivityForResult. The value returned by ths method should be
     * used as the requestCode parameter when launching the child activity.
     * @param callback  An activity result handler.
     * @return A request code value.
     */
    public int registerCallback(Callback callback) {
        // Generate a request code from the callback object's identity.
        // Note that this is possibly a complete over-engineering of the
        // solution, as there is presumably only ever one pending callback
        // per app, and possibly using a fixed value request code (e.g.
        // '1') would work just as well; the answer ultimately depends on
        // the purpose of the request code in the activity result design.
        int requestCode = System.identityHashCode( callback );
        pendingCallbacks.put( requestCode, callback );
        return requestCode;
    }

    /**
     * Deregister a callback.
     * @param callback  The callback to deregister.
     */
    public void deregisterCallback(Callback callback) {
        int requestCode = System.identityHashCode( callback );
        pendingCallbacks.remove( requestCode );
    }

    /** Handle an activity result. */
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        Callback callback = pendingCallbacks.remove( requestCode );
        if( callback != null ) {
            if( resultCode == Activity.RESULT_CANCELED ) {
                callback.onCancelled();
            }
            else {
                callback.onOk( resultCode, result );
            }
        }
    }

    /** The the class instance. */
    public static ActivityResult getManager() {
        return Manager;
    }

}
