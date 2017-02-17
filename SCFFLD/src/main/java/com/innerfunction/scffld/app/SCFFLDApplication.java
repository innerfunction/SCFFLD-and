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

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

/**
 * Standard SCFFLD application class.
 *
 * Attached by juliangoacher on 12/07/16.
 */
public class SCFFLDApplication extends Application {

    static final String Tag = SCFFLDApplication.class.getSimpleName();

    static final boolean TraceEnabled = false;

    /**
     * A URI specifying the location of the app container configuration.
     * If not specified then the app will load the standard SCFFLD configuration.
     * This value can be configured within the application declaration in the app manifest by using
     * a meta-data tag with a name of 'configurationURI', e.g.:
     *
     *  <meta-data android:name="SCFFLDApplication.configurationURI" android:value="app:/config.json" />
     *
     * (Note that the property name is prefixed with the name of the application class being used).
     */
    private String configurationURI;

    public SCFFLDApplication() {}

    /**
     * Create a new application instance using the specified configuration URI.
     * Subclasses can use this constructor to specify an alternative location for the app
     * configuration.
     *
     * @param configurationURI An internal URI resolving to the app configuration.
     */
    public SCFFLDApplication(String configurationURI) {
        this.configurationURI = configurationURI;
    }

    public void setConfigurationURI(String uri) {
        this.configurationURI = uri;
    }

    public String getConfigurationURI() {
        return configurationURI;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ManifestMetaData.applyTo( this );
        try {
            // Enable debugging of webviews via titleBarState.
            // Taken from https://developer.chrome.com/devtools/docs/remote-debugging#debugging-webviews
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                if( 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) ) {
                    WebView.setWebContentsDebuggingEnabled( true );
                }
            }
            // Configure and start the app container.
            AppContainer appContainer = AppContainer.getAppContainer( getApplicationContext() );
            if( TraceEnabled) {
                android.os.Debug.startMethodTracing("scffld-trace");
            }
            if( configurationURI != null ) {
                appContainer.loadConfiguration( configurationURI );
            }
            else {
                appContainer.loadStandardConfiguration();
            }
            if( TraceEnabled ) {
                android.os.Debug.stopMethodTracing();
            }
            appContainer.startService();
        }
        catch(Exception e) {
            Log.e(Tag, "Application startup failure", e );
        }
    }
}
