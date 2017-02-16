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
package com.innerfunction.scffld.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.innerfunction.scffld.R;

/**
 * A basic activity type for displaying a splashscreen before the main app UI loads.
 *
 * Attached by juliangoacher on 12/07/16.
 * @deprecated
 */
public class SplashScreenActivity extends Activity {

    static final String Tag = SplashScreenActivity.class.getSimpleName();

    /**
     * The minimum time, in milliseconds, for which the splash screen should be displayed.
     * The app's main root view will be displayed once this time has elapsed.
     * This value can be configured within the application declaration in the app manifest by using
     * a meta-data tag with a name of 'splashScreenDelay', e.g.:
     *
     *  <meta-data android:name="splashScreenDelay" android:value="1000" />
     *
     */
    protected int splashDelay = 2000;
    /**
     * The splash-screen layout ID..
     * Defaults to R.layout.splashscreen_layout. Can be configured within the application
     * declaration in the app manifesst by using a meta-data tag with a name of
     * 'splashScreenLayout', e.g:
     *
     *  <meta-data android:name="splashScreenLayout" android:resource="@R.layout.xxx" />
     *
     */
    protected int splashScreenLayout = R.layout.splashscreen_layout;

    public void setSplashDelay(int delay) {
        this.splashDelay = delay;
    }

    public int getSplashDelay() {
        return splashDelay;
    }

    public void setSplashScreenLayout(int layout) {
        this.splashScreenLayout = layout;
    }

    public int getSplashScreenLayout() {
        return splashScreenLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The following is to avoid a blank white screen being briefly displayed as the activity
        // loads; taken from http://stackoverflow.com/a/20560190 but note comment below that at
        // http://stackoverflow.com/a/34960302
        setTheme( android.R.style.Theme_Translucent_NoTitleBar );

        super.onCreate( savedInstanceState );

        setContentView( splashScreenLayout );

        // Create a task to display the app's root view after the splash screen.
        Runnable task = new Runnable() {
            @Override
            public void run() {
                AppContainer appContainer = AppContainer.getAppContainer();
                if( appContainer != null && appContainer.isRunning() ) {
                    appContainer.showRootView();
                    SplashScreenActivity.this.finish();
                }
                else if( !appContainer.isStartFailure() ) {
                    // App container not fully started yet, reschedule the task to try again after
                    // a small additional delay.
                    new Handler().postDelayed( this, 250 );
                }
            }
        };
        // Schedule the task to run.
        new Handler().postDelayed( task, (long)splashDelay );
    }

}
