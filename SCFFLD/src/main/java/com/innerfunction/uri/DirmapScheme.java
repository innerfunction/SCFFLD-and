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
package com.innerfunction.uri;

import android.util.LruCache;

import java.util.Map;
import java.util.Set;

import com.innerfunction.util.Assets;

/**
 * The handler for the dirmap: URI scheme.
 * The scheme exists primarily as a way to map JSON files in a directory into
 * a configuration structure.
 */
public class DirmapScheme implements URIScheme {

    /** Object constant representing a null (not found) directory map. */
    static final Dirmap NullDirmap = new Dirmap( null );

    /** A cache of previously resolved dirmaps, keyed by directory path. */
    private LruCache<String, Dirmap> dirmapCache = new LruCache<>( 10 );
    /**
     * A base URI scheme handler, used to dereference requested resources.
     * A directory map may be loaded either from the app's assets filesystem, or
     * from another filesystem on the device. Each of these requires a slightly
     * different mechanism for reading resources, see the class constructors.
     */
    private URIScheme baseScheme;

    /** Create a scheme mapped to a directory on one of the device's filesystems. */
    public DirmapScheme(Context context, String path) {
        this.baseScheme = new FileBasedScheme( context, path );
    }

    /** Create a scheme mapped to a directory on the app's assets filesystem. */
    public DirmapScheme(Context context, Assets assets, String path) {
        this.baseScheme = new AnRBasedScheme( context, path, assets );
    }

    @Override
    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        // First check for a previously cached result.
        String cacheKey = uri.getName();
        Dirmap dirmap = dirmapCache.get( cacheKey );
        if( dirmap == NullDirmap ) {
            // Previous directory miss.
            dirmap = null;
        }
        else if( dirmap == null ) {
            // Result not found, try reading the resource for the named directory.
            Object rsc = baseScheme.dereference( uri, params );
            if( rsc instanceof DirectoryResource ) {
                dirmap = new Dirmap( (DirectoryResource)rsc );
                dirmapCache.put( cacheKey, dirmap );
            }
            else {
                // Named directory not found; store NullDirmap to indicate miss.
                dirmapCache.put( cacheKey, NullDirmap );
            }
        }
        return dirmap;
    }
    
    /**
     * A Map interface wrapper for a directory resource.
     */
    static final class Dirmap implements Map<String,Object> {

        private DirectoryResource dirResource;

        Dirmap(DirectoryResource dirResource) {
            this.dirResource = dirResource;
        }

        private FileResource resourceForKey(Object key) {
            String path = key.concat(".json");
            return dirResource.resourceForPath( path );
        }

        @Override
        public void clear() {
            // Noop.
        }

        @Override
        public boolean containsKey(Object key) {
            return resourceForKey( key ) != null;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            return null;
        }

        @Override
        public String get(Object key) {
            // Given any key, attempt to load the contents of a file named <key>.json
            // and return its parsed contents as the result.
            FileResource fileRsc = resourceForKey( key );
            if( fileRsc != null ) {
                return fileRsc.asJSONData();
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Set<String> keySet() {
            return null;
        }

        @Override
        public String put(String key, Object value) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> all) {
            // Noop.
        }

        @Override
        public String remove(Object key) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Collection<String> values() {
            return null;
        }
    }
    
}

