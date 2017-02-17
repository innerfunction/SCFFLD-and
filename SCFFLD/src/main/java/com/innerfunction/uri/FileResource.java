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
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.innerfunction.util.Files;
import com.innerfunction.util.Paths;

/**
 * An object for representing file resources.
 */
@SuppressWarnings("unused")
public class FileResource extends Resource {

    private static final String LogTag = FileResource.class.getSimpleName();

    /** The file being represented. */
    protected File file;

    public FileResource(Context context, File file, CompoundURI uri) {
        super( context, file, uri );
        this.file = file;
    }

    protected FileResource(Context context, Object resource, CompoundURI uri) {
        super( context, resource, uri );
    }

    public File asFile() {
        return file;
    }

    public InputStream openInputStream() throws IOException {
        return new FileInputStream( this.file );
    }

    public String getAssetName() {
        return this.file.getAbsolutePath();
    }

    /** Test whether the file represented by this resource exists. */
    public boolean exists() {
        return this.file.exists();
    }

    /** Test whether the file represents a directory on the file system. */
    public boolean isDirectory() {
        return this.file.isDirectory();
    }

    /**
     * Return a resource for the file at the specified path relative to the current directory.
     * Returns null if no file exists at the specified path.
     */
    public FileResource resourceForPath(String path) {
        File rscFile = new File( file, path );
        if( rscFile.exists() ) {
            // Create a URI for the file resource by appending the file path to this resource's path.
            String name = Paths.join( super.uri.getName(), path );
            CompoundURI uri = super.uri.copyOfWithName( name );
            return new FileResource( super.context, rscFile, uri );
        }
        // File not found.
        return null;
    }

    /**
     * List the files contained by the directory.
     * Returns null if the current file resource doesn't represent a directory.
     */
    public String[] list() {
        return file.list();
    }

    /** Return the string contents of the file resource. */
    public String asString() {
        return Files.readString( this.file );
    }

    /** Return the file URL. */
    public URI asURL() {
        return this.file.toURI();
    }

    /** Return the byte contents of the file resource. */
    public byte[] asData() {
        try {
            return Files.readData( this.file );
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

    /** Return the file's contents as parsed JSON data. */
    public Object asJSONData() {
        return getTypeConversions().asJSONData( asString() );
    }

    /** Return the contents of the file resource as an image. */
    public Drawable asImage() {
        return Drawable.createFromPath( this.file.getAbsolutePath() );
    }

    @Override
    public Object asRepresentation(String representation) {
        if( "filepath".equals( representation ) ) {
            return this.file.getAbsolutePath();
        }
        return super.asRepresentation( representation );
    }

}
