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

import android.content.Context;
import android.util.LruCache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.innerfunction.util.Assets;
import com.innerfunction.scffld.Configuration;

/**
 * The handler for the dirmap: URI scheme.
 * The scheme exists primarily as a way to map JSON files in a directory into
 * a configuration structure.
 *
 */
public class DirmapScheme implements URIScheme {

    /** Object constant representing a null (not found) directory map. */
    static final Dirmap NullDirmap = new Dirmap();

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
            FileResource fileRsc = null;
            if( rsc instanceof FileResource ) {
                fileRsc = (FileResource)rsc;
            }
            if( fileRsc != null && fileRsc.isDirectory() ) {
                dirmap = new Dirmap( (FileResource)rsc, params );
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
     * A directory map.
     * This object is used to map JSON files on the app: file system into the configuration space.
     * Note that the URI scheme allows directory maps to be initialized with static resources
     * which are specified as parameters to the dirmap: URI. This allows some of the directory
     * map entries to be specified in the URI, rather than on the file system.
     */
    static final class Dirmap extends HashMap<String,Object> implements URIHandlerAware {

        private FileResource dirResource;

        protected Dirmap() {}
        
        Dirmap(FileResource dirResource, Map<String,Object> staticResources) {
            this.dirResource = dirResource;
            // Copy all static resources into the directory map. Note that the actual file resources
            // are populated later, in the setURIHandler() method; this means that static resources
            // can be overridden by providing a JSON file with the same key in the mapped directory.
            // This behaviour is consistent with the iOS implementation.
            putAll( staticResources );
        }

        @Override
        public void setURIHandler(URIHandler uriHandler) {
            dirResource.setURIHandler( uriHandler );
            // The following code populates the dirmap with its entries. Putting the initialization
            // code here is bad design (because the map is unpopulated if this method isn't called
            // before accessing its entries), but it ensures that the file resources used to
            // initialize this map's configuration entries all have the correct URI scheme context.
            String[] files = dirResource.list();
            for( String filename : files ) {
                if( filename.endsWith(".json") ) {
                    FileResource fileRsc = dirResource.resourceForPath( filename );
                    if( fileRsc != null ) {
                        String key = filename.substring( 0, filename.length() - 5 );
                        put( key, new Configuration( fileRsc ) );
                    }
                }
            }
        }

    }
    
}

