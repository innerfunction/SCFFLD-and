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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import static com.innerfunction.util.DataLiterals.*;

import com.innerfunction.scffld.Configuration;
import com.innerfunction.scffld.Container;
import com.innerfunction.scffld.IOCProxyLookup;
import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.MessageReceiver;
import com.innerfunction.scffld.MessageRouter;
import com.innerfunction.scffld.ui.NavigationViewController;
import com.innerfunction.scffld.ui.SlideViewController;
import com.innerfunction.scffld.ui.TextViewIOCProxy;
import com.innerfunction.scffld.ui.WebViewController;
import com.innerfunction.scffld.ui.table.TableViewController;
import com.innerfunction.uri.AnRBasedScheme;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.StandardURIHandler;
import com.innerfunction.uri.URIScheme;
import com.innerfunction.uri.URIValueFormatter;
import com.innerfunction.util.I18nMap;
import com.innerfunction.util.UserDefaults;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * An IOC container encapsulating an app's UI and functionality.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class AppContainer extends Container {

    static final String Tag = AppContainer.class.getSimpleName();

    /**
     * The standard core type mappings.
     */
    static final Map<String,Object> CoreTypes = m(
        // Types are coded this way so that (1) class references can be checked at compile
        // time and (2) refactor tools can update the mapping in the case of class renames.
        kv("View",              ViewController.class.getCanonicalName() ),
        kv("NavigationView",    NavigationViewController.class.getCanonicalName() ),
        kv("SlideView",         SlideViewController.class.getCanonicalName() ),
        kv("WebView",           WebViewController.class.getCanonicalName() ),
        kv("ListView",          TableViewController.class.getCanonicalName() )
    );

    /**
     * Global values available to the container's configuration. Can be referenced from within
     * templated configuration values.
     * Available values include the following:
     * - *platform*: Information about the container platform. Has the following values:
     *   - name: Always "ios" on iOS systems.
     *   - display: The display scale, e.g. 2x, 3x.
     * - *locale*: Information about the device's default locale. Has the following values:
     *   - id: The locale identifier, e.g. en_US
     *   - lang: The language code, e.g. en
     *   - variant: The locale variant, e.g. US
     */
    private Map<String,Object> globals;
    // TODO Local storage
    /** The app's default background colour. */
    private int appBackgroundColor;
    /**
     * A map of Android class configurations, keyed by fully qualified class name.
     * This provides a mechanism for performing IOC configuration of Android system class
     * instances, such as Activities and Receivers, which can't be directly instantiated by
     * the container.
     */
    private Map<String,Configuration> andSystemClassConfigs;
    /** Map of additional scheme configurations. */
    private Map<String,URIScheme> schemes;
    /** Make configuration patterns. */
    private Configuration patterns;
    /**
     * The currently active activity.
     * Normally corresponds to the currently visible activity within an app. However, if the app
     * opens an activity _external_ to the app container (i.e. an activity which doesn't implement
     * SCFFLDActivity) then this will be null.
     */
    private SCFFLDActivity currentActivity;
    /** A flag indicating a start failure. */
    private boolean startFailure;

    public AppContainer(Context context) {
        super( context, StandardURIHandler.getInstance( context ) );
        setPriorityNames("types", "formats", "schemes", "aliases", "patterns");
    }

    public Map<String,URIScheme> getSchemes() {
        return schemes;
    }

    public void setSchemes(Map<String,URIScheme> schemes) {
        this.schemes = schemes;
    }

    public Configuration getPatterns() {
        return patterns;
    }

    public void setPatterns(Configuration patterns) {
        this.patterns = patterns;
    }

    public Map<String,URIValueFormatter> getFormats() {
        return uriHandler.getFormats();
    }

    public void setFormats(Map<String,URIValueFormatter> formats) {
        uriHandler.setFormats( formats );
    }

    public Map<String,String> getAliases() {
        return uriHandler.getAliases();
    }

    public void setAliases(Map<String,String> aliases) {
        uriHandler.setAliases( aliases );
    }

    public void setAppBackgroundColor(int color) {
        this.appBackgroundColor = color;
    }

    public int getAppBackgroundColor() {
        return appBackgroundColor;
    }

    /**
     * Load the app configuration.
     */
    public void loadConfiguration(Object configSource) {
        try {
            Configuration configuration = null;
            if( configSource instanceof Configuration ) {
                // Configuration source is already a configuration.
                configuration = (Configuration)configSource;
            }
            else {
                // Test if config source specifies a URI.
                CompoundURI uri = null;
                if( configSource instanceof CompoundURI ) {
                    uri = (CompoundURI)configSource;
                }
                else if( configSource instanceof String ) {
                    try {
                        uri = CompoundURI.parse( (String)configSource );
                    }
                    catch(URISyntaxException e) {
                        Log.e( Tag, "Error parsing app container configuration URI", e );
                        return;
                    }
                }
                Object configData = null;
                if( uri != null ) {
                    // If a configuration source URI has been resolved then attempt loading the
                    // configuration from the URI.
                    Log.i( Tag, String.format( "Attempting to load app container configuration from %s", uri ) );
                    configData = uriHandler.dereference( uri );
                }
                else {
                    configData = configSource;
                }
                // Create configuration from data.
                if( configData instanceof Resource ) {
                    configuration = makeConfiguration( configData );
                    // Use the configuration's URI handler instead from this point on, to ensure
                    // relative URI's resolve properly and also so that additional URI schemes added
                    // to this container are available within the configuration.
                    uriHandler = (StandardURIHandler)configuration.getURIHandler();
                }
                else {
                    configuration = makeConfiguration( configSource );
                }
            }
            if( configuration != null ) {
                configureWith( configuration );
            }
            else {
                Log.w( Tag, String.format( "Unable to resolve configuration from %s", configSource ) );
            }
        }
        catch(RuntimeException e) {
            startFailure = true;
            throw e;
        }
    }

    @Override
    public void configureWith(Configuration configuration) {

        // Setup template context.
        this.globals = makeDefaultGlobalModelValues( configuration );
        configuration.setContext( globals );

        // Set object type mappings.
        addTypes( configuration.getValueAsConfiguration("types") );

        // Add additional schemes to the resolver/dispatcher.
        uriHandler.addHandlerForScheme("new",     new NewScheme( this ) );
        uriHandler.addHandlerForScheme("make",    new MakeScheme( this ) );
        uriHandler.addHandlerForScheme("named",   new NamedScheme( this ) );
        uriHandler.addHandlerForScheme("post",    new PostScheme() );
        uriHandler.addHandlerForScheme("pattern", new AnRBasedScheme( androidContext, "SCFFLD/patterns", "json") );

        // Default local settings.
        // TODO locals + settings

        nameds.put("uriHandler",    uriHandler );
        nameds.put("globals",       globals );
        //nameds.put("locals", locals );
        nameds.put("container",     this );
        nameds.put("app",           this );

        // Copy any configurations defined in the /nameds directory over the container configuration.
        Configuration namedsConfig = configuration.getValueAsConfiguration("nameds");
        if( namedsConfig != null ) {
            configuration = configuration.configurationWithKeysExcluded("nameds");
            configuration = configuration.mixinConfiguration( namedsConfig );
        }

        // Perform default container configuration.
        super.configureWith( configuration );

        // Map any additional schemes to the URI handler.
        if( schemes != null ) {
            for( String schemeName : schemes.keySet() ) {
                URIScheme scheme = schemes.get( schemeName );
                uriHandler.addHandlerForScheme( schemeName, scheme );
            }
        }
    }

    /**
     * Make the set of global values.
     * @param configuration
     * @return
     */
    protected Map<String,Object> makeDefaultGlobalModelValues(Configuration configuration) {

        Resources r = androidContext.getResources();

        Map<String,Object> values = new HashMap<>();
        DisplayMetrics dm = r.getDisplayMetrics();
        String density;
        switch( dm.densityDpi ) {
        case DisplayMetrics.DENSITY_LOW:    density = "ldpi"; break;
        case DisplayMetrics.DENSITY_MEDIUM: density = "mdpi"; break;
        case DisplayMetrics.DENSITY_HIGH:   density = "hdpi"; break;
        case DisplayMetrics.DENSITY_XHIGH:  density = "xhdpi"; break;
        case DisplayMetrics.DENSITY_XXHIGH: density = "xxhdpi"; break;
        default:                            density = "hdpi";
        }
        Map<String,Object> platformValues = new HashMap<>();
        platformValues.put("name", "and");
        platformValues.put("display", density );
        platformValues.put("defaultDisplay", "hdpi");
        platformValues.put("full", "and-"+density);
        values.put("platform", platformValues );

        String mode = configuration.getValueAsString("mode", "LIVE" );
        Log.i( Tag, String.format( "Configuration mode: %s", mode ) );
        values.put("mode", mode );

        Locale locale = r.getConfiguration().locale;
        // The 'supportedLocales' setting can be used to declare a list of the locales that app assets are
        // available in. If the platform's default locale (above) isn't on this list then the code below
        // will attempt to find a supported locale that uses the same language; if no match is found then
        // the first locale on the list is used as the default.
        if( configuration.hasValue("supportedLocales") ) {
            @SuppressWarnings("unchecked")
            List<String> assetLocales = (List<String>)configuration.getRawValue("supportedLocales");
            if( assetLocales.size() > 0 && !assetLocales.contains( locale.toString() ) ) {
                // Attempt to find a matching locale.
                // Always assigns the first item on the list (as the default option); if a later
                // item has a matching language then that is assigned and the loop is exited.
                String lang = locale.getLanguage();
                boolean langMatch = false, assignDefault;
                for( int i = 0; i < assetLocales.size() && !langMatch; i++ ) {
                    String[] localeParts = assetLocales.get( 0 ).split("_");
                    assignDefault = (i == 0);
                    langMatch = localeParts[0].equals( lang );
                    if( assignDefault || langMatch ) {
                        switch( localeParts.length ) {
                        case 1: locale = new Locale( localeParts[0] ); break;
                        case 2: locale = new Locale( localeParts[0], localeParts[1] ); break;
                        case 3: locale = new Locale( localeParts[0], localeParts[1], localeParts[3] ); break;
                        default:
                            Log.w(Tag,String.format("Bad locale identifier: %s", assetLocales.get( 0 )));
                        }
                    }
                }
            }
        }

        Map<String,Object> localeValues = new HashMap<>();
        localeValues.put("id", locale.toString());
        localeValues.put("lang", locale.getLanguage() );
        localeValues.put("variant", locale.getVariant() );
        values.put("locale", localeValues );

        // Access to localized resources through a Map interface.
        values.put("i18n", new I18nMap( androidContext ) );

        return values;
    }

    /**
     * Perform IOC configuration on an Android system class instance.
     * @param instance  An Android class instance.
     */
    public void configureAndroid(Object instance) {
        if( andSystemClassConfigs != null ) {
            String className = instance.getClass().getName();
            Configuration config = andSystemClassConfigs.get( className );
            if( config != null ) {
                configureObject( instance, config, className );
            }
        }
    }

    /**
     * Set the current activity.
     * Should be called by a SCFFLDActivity instance from within its onResume method.
     */
    public void setCurrentActivity(SCFFLDActivity activity) {
        currentActivity = activity;
    }

    /** Get the current activity. */
    public SCFFLDActivity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * Clear the current activity.
     * Called by the active activity when it is paused (i.e. as the user navigates away).
     */
    public void clearCurrentActivity(SCFFLDActivity activity) {
        if( currentActivity == activity ) {
            currentActivity = null;
        }
    }

    /** Test whether there was a failure during container start. */
    public boolean isStartFailure() {
        return startFailure;
    }

    @Override
    public void startService() {
        try {
            super.startService();
        }
        catch(RuntimeException e) {
            startFailure = true;
            throw e;
        }
    }

    /**
     * Get an object for reading and writing shared preferences.
     */
    public UserDefaults getUserDefaults() {
        return new UserDefaults( androidContext );
    }

    /**
     * Post a message.
     * @param message   A string containing a message URI. Normally this is specified using a
     *                  post: URI, but the method will attempt to promote other URIs to messages.
     *                  A plain non-URI string is interpreted as the message name.
     * @param sender    The component sending the message.
     */
    public void postMessage(String message, final Object sender) {
        // Try parsing the action URI.
        CompoundURI messageURI = CompoundURI.tryParsing( message );
        // If URI doesn't parse then it may be a bare message, try prepending post: and parsing again.
        if( messageURI == null ) {
            message = String.format("post:%s", message );
            messageURI = CompoundURI.tryParsing( message );
        }
        if( messageURI != null ) {
            final CompoundURI uri = messageURI;
            // Process the message on the main thread. This is because the URI may dereference to a
            // view and some views (e.g. web views) have to be instantiated on the UI thread.
            new Handler( Looper.getMainLooper() ).post( new Runnable() {
                @Override
                public void run() {
                    // See if the URI resolves to a post message object.
                    Object messageObj = uriHandler.dereference( uri );
                    if( !(messageObj instanceof Message) ) {
                        // Automatically promote views to 'show' messages.
                        if( messageObj instanceof View || messageObj instanceof Fragment ) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("view", messageObj );
                            messageObj = new Message("show", params );
                        }
                        else if( messageObj instanceof String ) {
                            // Assume a simple name only message with no parameters.
                            messageObj = new Message( (String) messageObj, null );
                        }
                        else return; // Can't promote the message, so can't dispatch it.
                    }
                    // messageObj is always a Message instance by this point.
                    routeMessage( (Message) messageObj, sender );
                }
            } );
        }
    }

    /** Test whether a URI scheme name belongs to an internal URI scheme. */
    public boolean isInternalURISchemeName(String schemeName) {
        return uriHandler.hasHandlerForURIScheme( schemeName );
    }

    /** Open a public URL. */
    public boolean openURL(String url) {
        if( url == null ) {
            return false;
        }
        int idx = url.indexOf(':');
        String scheme = idx > 0 ? url.substring( 0, idx ) : "";
        Intent intent = null;
        if( "tel".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_DIAL, Uri.parse( url ));
        }
        if( "https".equalsIgnoreCase( scheme ) || "http".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ));
        }
        if( "mailto".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_SENDTO, Uri.parse( url ));
        }
        if( intent != null ) {
            ((Activity)currentActivity).startActivity( intent );
            return true;
        }
        return false;
    }

    // MessageRouter interface
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        // If the sender is within the UI then search the view hierarchy for a message handler.
        Object target = sender;
        // Evaluate messages with relative target paths against the sender.
        while( target != null && !routed ) {
            // See if the current handler can take the message.
            if( message.hasEmptyTarget() ) {
                // Message has no target info so looking for a message handler.
                if( target instanceof MessageReceiver ) {
                    routed = ((MessageReceiver)target).receiveMessage( message, sender );
                }
            }
            else if( target instanceof MessageRouter ) {
                // Message does have target info so looking for a message router.
                routed = ((MessageRouter)target).routeMessage( message, sender );
            }
            if( !routed ) {
                // Message not routed, so try moving up the view hierarchy.
                if( target instanceof ViewController ) {
                    target = ((ViewController)target).getParentViewController();
                }
                else if( target instanceof Fragment ) {
                    // If target is a fragment then bubble the message up through the fragment
                    // display hierarchy until a receiver is found.
                    Fragment parentFragment = ((Fragment)target).getParentFragment();
                    if( parentFragment == null ) {
                        target = ((Fragment)target).getActivity();
                    }
                    else {
                        target = parentFragment;
                    }
                }
                else if( target instanceof View ) {
                    // If target is a view then move up to the view parent. This may at some point
                    // resolve to a MessageRouter, MessageReceiver or ViewController.
                    target = ((View)target).getParent();
                }
                else {
                    // Can't process the message any further, so leave the loop.
                    break;
                }
            }
        }
        // If message not dispatched then let this container try handling it.
        if( !routed ) {
            routed = super.routeMessage( message, sender );
        }
        return routed;
    }

    // MessageReceiver interface
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("open-url") ) {
            String url = (String)message.getParameter("url");
            // Open URLs by dispatching to the default intent (e.g. the browser)
            Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ) );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            androidContext.startActivity( intent );
        }
        else if( message.hasName("show") ) {
            Object view = message.getParameter("view");
            if( view != null ) {
                showView( view );
            }
            else {
                Log.w(Tag,"'show' message missing 'view' parameter.");
            }
        }
        return true;
    }

    /**
     * Start a new activity to display the app's root view.
     */
    public void showRootView() {
        Object rootView = uriHandler.dereference("make:RootView");
        if( rootView == null ) {
            Log.e( Tag, "No root view pattern found" );
        }
        else if( !(rootView instanceof ViewController || rootView instanceof Fragment || rootView instanceof Intent) ) {
            if( rootView instanceof View ) {
                // Package the view into a view controller.
                rootView = new ViewController( androidContext, (View)rootView );
            }
            else {
                // Root isn't something that can be displayed; replace with a web view displaying
                // an explanatory message.
                WebViewController webView = new WebViewController( androidContext );
                webView.setContent( "<p>Root view not found, check that a RootView pattern exists and defines a view instance</p>" );
                rootView = webView;
            }
        }
        showView( rootView );
    }

    /**
     * Display a view. Starts a new activity if necessary.
     */
    public void showView(Object view) {

        // NOTE on supported view types: SCFFLD supports a number of different view types. The
        // main type is ViewController, and corresponds most closely to the UIViewController
        // class on iOS. Views can also be represented using Fragments (as the natural native
        // Android view type) but use of Fragments is discouraged where possible, due to the
        // baroque fragment life-cycle, and problems that occur when fragments are nested.
        // If a view is represented by an Activity then an Intent instance should be used at
        // this point to represent the configured view. (This is because of Android mechanics
        // - the OS is responsible for instantiating activities, through intents, so it doesn't
        // make sense for the container to instantiate an activity). Intents, if needed, can
        // be instantiated for the container by an object factory.

        if( view instanceof ViewController ) {
            showViewUsingActivityType( view, ViewControllerActivity.class );
        }
        else if( view instanceof ViewFragment ) {
            showViewUsingActivityType( view, ViewFragmentActivity.class );
        }
        else if( view instanceof Fragment ) {
            showViewUsingActivityType( view, FragmentActivity.class );
        }
        else if( view instanceof Intent ) {
            androidContext.startActivity( (Intent)view );
        }
        else {
            Log.w(Tag, String.format("Unable to display view of type %s", view.getClass() ) );
        }
        /* Following code left here as an example of why it doesn't make sense for the
           container to instantiate Activities as view instances - the instance is discarded
           at this point, an intent is created instead which requests the OS to start the
           activity.
        else if( view instanceof Activity ) {
            Intent intent = new Intent( androidContext, Activity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            androidContext.startActivity( intent );
        }
        */
    }

    /**
     * Show a view using the specified activity type.
     * If the current activity is of the specified type then its showView(...) is called (note that
     * the current activity is an instance of SCFFLDActivity). If the current activity can't be used
     * then an intent is raised to start a new activity of the specified type, which will then
     * display the view instance once it has launched.
     *
     * @param view          The view to display.
     * @param activityType  The type (i.e. class) of the activity to use.
     */
    private void showViewUsingActivityType(Object view, Class activityType) {
        if( activityType.isInstance( currentActivity ) ) {
            currentActivity.showView( view );
        }
        else {
            // TODO: Consider whether to support launching a new activity of the appropriate type.
            /*
            String uuid = registerPendingView( view );
            Intent intent = new Intent( androidContext, activityType );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            intent.putExtra( SCFFLDActivity.IntentActions.ViewUUID.name(), uuid );
            androidContext.startActivity( intent );
            */
            Log.w(Tag, String.format("Can't open view of type %s with current activity", activityType ) );
        }
    }

    /**
     * Find the root app container in a container heirarchy.
     * Checks the container argument, and then its parent and so on until the root
     * app container is found. Returns null if no app container is found.
     */
    static AppContainer findAppContainer(Container container) {
        while( container != null ) {
            if( container instanceof AppContainer ) {
                return (AppContainer)container;
            }
            container = container.getParentContainer();
        }
        return null;
    }

    /** The app container's singleton instance. */
    static AppContainer Instance;

    public static synchronized AppContainer getAppContainer(Context context) {
        if( Instance == null ) {
            Instance = new AppContainer( context );
            Instance.addTypes( CoreTypes );
            Instance.loadConfiguration( m(
                kv("types",     "@app:/SCFFLD/types.json"),
                kv("schemes",   "@dirmap:/SCFFLD/schemes"),
                kv("patterns",  "@dirmap:/SCFFLD/patterns"),
                kv("nameds",    "@dirmap:/SCFFLD/nameds")
            ));
        }
        return Instance;
    }

    public static AppContainer getAppContainer() {
        return Instance;
    }
    
    static {
        // Register standard configuration proxies.
        // TODO: Is there a better place to put this code? Creates a two way dependency between scffld.app and scffld.ui.
        IOCProxyLookup.registerProxyClass( TextViewIOCProxy.class, android.widget.TextView.class );
    }

}
