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
package com.innerfunction.scffld;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Lookup map of configuration proxies keyed by class name.
 * Classes without a registered proxy get an IOCProxyLookup.NullEntry entry.
 * Attached by juliangoacher on 29/03/16.
 */
public class IOCProxyLookup {

    static final String Tag = IOCProxyLookup.class.getSimpleName();

    /**
     * An entry in the lookup representing a proxy class.
     */
    static class Entry {
        /** The proxy class. */
        Class proxyClass;

        Entry(Class c) {
            this.proxyClass = c;
        }

        /** Instantiate a new proxy instance. */
        public IOCProxy instantiateProxy(Context context) {
            // First check for a constructor accepting a Context arg.
            try {
                Constructor constructor = proxyClass.getConstructor( Context.class );
                return (IOCProxy)constructor.newInstance( context );
            }
            catch(NoSuchMethodException e) {
            }
            catch(Exception e) {
                Log.w( Tag, "Error instantiating proxy instance with Context", e );
            }
            // Otherwise try instantiating proxy with a no-args constructor.
            try {
                return (IOCProxy)proxyClass.newInstance();
            }
            catch(Exception e) {
                Log.e( Tag, "Error instantiating proxy instance", e );
            }
            return null;
        }

        /**
         * Instantiate a new proxy instance with the specified value.
         * @param value An in-place value.
         */
        public IOCProxy instantiateProxyWithValue(Object value) {
            try {
                // Try to find a single-arg constructor accepting value as its argument.
                Constructor<?>[] constructors = proxyClass.getConstructors();
                Class<?> valueClass = value.getClass();
                for( Constructor<?> constructor : constructors ) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if( paramTypes.length == 1 && paramTypes[0].isAssignableFrom( valueClass ) ) {
                        return (IOCProxy)constructor.newInstance( value );
                    }
                }
            }
            catch(Exception e) {
                Log.e( Tag, "Error instantiating proxy instance", e );
            }
            return null;
        }
    }

    /**
     * An object representing a null classname - proxy mapping.
     */
    static final Entry NullEntry = new Entry( null );

    /**
     * The lookup map
     * Maps the class name of the object being proxied to the proxy class.
     */
    static final Map<String,Entry> Proxies = new HashMap<>();

    /**
     * Register a new proxy class.
     * @param proxyClass        The proxy class.
     * @param proxiedClassName  The classname of the object being proxied.
     */
    public static void registerProxyClass(Class proxyClass, String proxiedClassName) {
        if( proxyClass == null ) {
            Proxies.put( proxiedClassName, NullEntry );
        }
        else {
            Proxies.put( proxiedClassName, new Entry( proxyClass ) );
        }
    }

    /**
     * Register a new proxy class.
     * @param proxyClass        The proxy class.
     * @param proxiedClass      The class being proxied.
     */
    public static void registerProxyClass(Class proxyClass, Class proxiedClass) {
        registerProxyClass( proxyClass, proxiedClass.getCanonicalName() );
    }

    /**
     * Lookup the proxy for an object's class.
     * @param object    An object.
     * @return A proxy instance appropriate for the object, or null if no proxy class is mapped for
     * the object's class.
     */
    static Entry lookupConfigurationProxyForObject(Object object) {
        Class objClass = object.getClass();
        String className = objClass.getCanonicalName();
        return lookupConfigurationProxy( objClass, className );
    }

    /**
     * Lookup the proxy for an object's class.
     * @param className An object's classname.
     * @return A proxy instance appropriate for the object, or null if no proxy class is mapped for
     * the object's class.
     */
    static Entry lookupConfigurationProxyForClassName(String className) {
        try {
            Class objClass = Class.forName( className );
            return lookupConfigurationProxy( objClass, className );
        }
        catch(ClassNotFoundException e) {
            Log.w( Tag, String.format("Class not found: %s", className ) );
        }
        return null;
    }

    /**
     * Lookup the proxy for an object's class.
     * @param objClass  The class of the object being proxied.
     * @param className The canonical name of the object class.
     * @return A proxy instance appropriate for the object, or null if no proxy class is mapped for
     * the object's class.
     */
    static Entry lookupConfigurationProxy(Class objClass, String className) {
        // First check for an entry under the current object's specific class name.
        Entry entry = Proxies.get( className );
        if( entry != null ) {
            // NullEntry at this stage indicates no proxy available for the specific object class.
            return entry == NullEntry ? null : entry;
        }
        // No entry found for the specific class, search for the closest superclass proxy.
        String specificClassName = className;
        objClass = objClass.getSuperclass();
        while( objClass != null ) {
            className = objClass.getCanonicalName();
            entry = Proxies.get( className );
            if( entry != null ) {
                // Proxy found, record the same proxy for the specific class and return the result.
                Proxies.put( specificClassName, entry );
                return entry == NullEntry ? null : entry;
            }
            // Nothing found yet, continue to the next superclass.
            objClass = objClass.getSuperclass();
        }
        // If we get to here then there is no registered proxy available for the object's class or
        // any of its superclasses; register an NullEntry in the map so that future lookups can
        // complete quicker.
        Proxies.put( specificClassName, NullEntry );
        return null;
    }

    /**
     * Check whether a configuration proxy is registered for an object's class, and if so then return an instance of
     * the proxy initialized with the object, otherwise return the object unchanged.
     */
    static Object applyConfigurationProxyWrapper(Object object) {
        if( object != null ) {
            Entry proxyEntry = lookupConfigurationProxyForObject( object );
            if( proxyEntry != null ) {
                object = proxyEntry.instantiateProxyWithValue( object );
            }
        }
        return object;
    }

}
