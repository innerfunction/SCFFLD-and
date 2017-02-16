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

import java.io.File;

import com.innerfunction.util.Paths;

/**
 * A resource type used to represent directories on the file system.
 */
public class DirectoryResource extends FileResource {

    /** Create a new resource for the specified directory. */
    public DirectoryResource(Context context, File dir, CompoundURI uri) {
        super( context, dir, uri );
    }

    /**
     * Return a resource for the file at the specified path relative to the current directory.
     */
    public FileResource resourceForPath(String path) {
        File file = new File( super.file, path );
        if( file.exists() && !file.isDirectory() ) {
            // Create a URI for the file resource by appending the file path to this resource's path.
            String name = Paths.join( super.uri.getName(), path );
            CompoundURI uri = super.uri.copyOfWithName( name );
            return new FileResource( super.context, file, uri );
        }
        // File not found.
        return null;
    }

    /** List the files contained by the directory. */
    public String[] list() {
        return super.file.list();
    }

}

