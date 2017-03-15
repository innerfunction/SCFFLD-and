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

import static com.innerfunction.util.DataLiterals.*;

/**
 * Standard SCFFLD application class.
 *
 * Attached by juliangoacher on 12/07/16.
 */
public class SCFFLDApplication extends Application {

    static final String Tag = SCFFLDApplication.class.getSimpleName();

    static final boolean TraceEnabled = false;

    public SCFFLDApplication() {}

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
            if( TraceEnabled) {
                android.os.Debug.startMethodTracing("scffld-trace");
            }
            // Configure and start the app container.
            Object configuration = m(
                kv("types",     "@app:/SCFFLD/types.json"),
                kv("schemes",   "@dirmap:/SCFFLD/schemes"),
                kv("patterns",  "@dirmap:/SCFFLD/patterns"),
                kv("nameds",    "@dirmap:/SCFFLD/nameds")
            );
            AppContainer appContainer = AppContainer.initialize( getApplicationContext(), configuration );
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
