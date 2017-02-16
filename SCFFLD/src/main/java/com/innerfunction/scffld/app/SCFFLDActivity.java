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

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.innerfunction.scffld.R;

/**
 * Attached by juliangoacher on 26/04/16.
 */
public abstract class SCFFLDActivity<T> extends AppCompatActivity {

    static final String Tag = SCFFLDActivity.class.getSimpleName();

    /** A layout for flipping between the splash screen and main app views. */
    private ViewFlipper layoutViewFlipper;
    /** The time the splashscreen started being displayed at. */
    private long splashScreenDisplayTime = 0;
    /** A flag indicating whether the root view has been loaded. */
    private boolean rootViewLoaded = false;
    /**
     * The minimum time, in milliseconds, for which the splash screen should be displayed.
     * The app's main root view will be displayed once this time has elapsed.
     * This value can be configured within the application declaration in the app manifest by using
     * a meta-data tag with a name of 'splashScreenDelay', e.g.:
     *
     *  <meta-data android:name="splashScreenDelay" android:value="1000" />
     *
     * Set this value to 0 or less to disable the splashscreen.
     */
    protected int splashScreenDelay = 2000;
    /**
     * The splash screen layout ID..
     * Defaults to R.layout.splashscreen_layout. Can be configured within the application
     * declaration in the app manifest by using a meta-data tag with a name of
     * 'splashScreenLayout', e.g:
     *
     *  <meta-data android:name="splashScreenLayout" android:resource="@R.layout.xxx" />
     *
     */
    protected int splashScreenLayout = R.layout.splashscreen_layout;

    public void setSplashDelay(int delay) {
        this.splashScreenDelay = delay;
    }

    public int getSplashDelay() {
        return splashScreenDelay;
    }

    public void setSplashScreenLayout(int id) {
        this.splashScreenLayout = id;
    }

    public int setSplashScreenLayout() {
        return splashScreenLayout;
    }

    /**
     * The app's default background color.
     */
    protected int appBackgroundColor = Color.LTGRAY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The following is to avoid a blank white screen being briefly displayed as the activity
        // loads; taken from http://stackoverflow.com/a/20560190 but note comment below that at
        // http://stackoverflow.com/a/34960302
        setTheme( android.R.style.Theme_Translucent_NoTitleBar );

        super.onCreate( savedInstanceState );

        setContentView( R.layout.view_activity_layout );

        AppContainer appContainer = AppContainer.getAppContainer();
        appBackgroundColor = appContainer.getAppBackgroundColor();

        this.layoutViewFlipper = (ViewFlipper)findViewById( R.id.viewflipper );

        if( splashScreenDelay > 0 && splashScreenLayout != -1 ) {
            // Add the splash screen layout to this activity's layout.
            View splashScreenView = getLayoutInflater().inflate( splashScreenLayout, layoutViewFlipper );
            ViewGroup splashScreenContainer = (ViewGroup)layoutViewFlipper.findViewById( R.id.splashscreen_container );
            splashScreenContainer.addView( splashScreenView );
            // Display the splash screen layout and record the time.
            layoutViewFlipper.setDisplayedChild( 0 );
            this.splashScreenDisplayTime = System.currentTimeMillis();
        }
        else {
            // No splash screen, display the main content child view.
            layoutViewFlipper.setDisplayedChild( 1 );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final AppContainer appContainer = AppContainer.getAppContainer();
        appContainer.setCurrentActivity( this );
        if( !rootViewLoaded ) {
            // Create a task to display the app's root view.
            Runnable showRootViewTask = new Runnable() {
                @Override
                public void run() {
                    if( appContainer.isRunning() ) {
                        appContainer.showRootView();
                        layoutViewFlipper.setDisplayedChild( 1 );
                        splashScreenDisplayTime = 0;
                        rootViewLoaded = true;
                    }
                    else if( !appContainer.isStartFailure() ) {
                        // App container not fully started yet, reschedule the task to try again
                        // after a small additional delay.
                        new Handler().postDelayed( this, 250 );
                    }
                }
            };
            // Decide whether to delay before displaying the root view. If a splash screen
            // has been displayed then 'delay' below will probably be positive; otherwise, no
            // splash screen is shown OR there has already been a sufficiently long delay
            // between when it was shown and now.
            long delay = splashScreenDelay - (System.currentTimeMillis() - splashScreenDisplayTime);
            if( delay > 0 ) {
                new Handler().postDelayed( showRootViewTask, delay );
            }
            else {
                // No delay, display root view immediately.
                showRootViewTask.run();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppContainer.getAppContainer().clearCurrentActivity( this );
    }

    public abstract void showView(T view);

}
