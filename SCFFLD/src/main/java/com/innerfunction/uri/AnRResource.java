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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.innerfunction.util.Assets;
import com.innerfunction.util.Files;
import com.innerfunction.util.Paths;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class representing a resource in the app's assets or res directories.
 */
public class AnRResource extends FileResource {

    private static final String LogTag = AnRResource.class.getSimpleName();

    /** The asset name. */
    private String assetName;

    public AnRResource(Context context, String assetName, CompoundURI uri) {
        super( context, assetName, uri );
        this.assetName = assetName;
    }

    @Override
    public String getAssetName() {
        return assetName;
    }

    @Override
    public boolean exists() {
        return true; // This resource object won't be created unless the asset exists.
    }

    @Override
    public byte[] asData() {
        try {
            return Files.readData( openInputStream(), assetName );
        }
        catch(IOException e) {
            Log.e(LogTag, String.format("Reading data from %s", assetName ), e );
        }
        return null;
    }

    @Override
    public String toString() {
        return assetName;
    }

    /**
     * A resource in the app's res directory.
     */
    public static class Res extends AnRResource {

        /** Application resources. */
        private Resources r;
        /** The resource ID. */
        private int resourceID;

        public Res(Context context, int resourceID, CompoundURI uri) {
            super( context, Integer.toString( resourceID ), uri );
            this.r = context.getResources();
            this.resourceID = resourceID;
        }

        /**
         * Test if this resource is a directory.
         * App resources can't represent directories, so always returns null.
         * @return
         */
        @Override
        public boolean isDirectory() {
            return false;
        }

        /**
         * Return a resource for the asset at the specified path relative to the current asset.
         * App resources aren't arranged hierarchically, so relative paths don't apply and this
         * method will always return null.
         */
        @Override
        public FileResource resourceForPath(String path) {
            return null;
        }

        /**
         * List the files contained by the asset directory.
         * App resources aren't arranged hierarchically, so always returns null.
         */
        @Override
        public String[] list() {
            return null;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return r.openRawResource( resourceID );
        }

        @Override
        public String asString() {
            return r.getString( resourceID );
        }

        @Override
        public URI asURL() {
            // NOTE resources can only be loaded by URL from the assets folder.
            return null;
        }

        /**
         * Fetch this resource's image representation.
         * Assumes that the resource's string representation is the assetName of an image to be
         * found in the app's resource bundle.
         */
        @Override
        public Drawable asImage() {
            return r.getDrawable( resourceID );
        }

    }

    /**
     * A resource under the app's assets dir.
     */
    public static class Asset extends AnRResource {

        /** Information about the app's assets.. */
        private Assets assets;

        public Asset(Context context, String name, Assets assets, CompoundURI uri) {
            super( context, name, uri );
            this.assets = assets;
        }

        /**
         * Test whether this resource represents a directory (i.e. folder) under the app's assets.
         */
        @Override
        public boolean isDirectory() {
            // There is no simple, performant method for testing whether an asset is a directory or
            // a file; simplest hack around this is to decide that all assets are directories.
            return true;
        }

        /**
         * Return a resource for the asset at the specified path relative to the current asset.
         * Returns null if no file exists at the specified path. Assumes that the current asset is
         * a directory; if not, then no asset will be found at the requested path anyway and so
         * null will be returned.
         */
        @Override
        public FileResource resourceForPath(String path) {
            String assetPath = Paths.join( getAssetName(), path );
            if( assets.assetExists( assetPath ) ) {
                CompoundURI uri = super.uri.copyOfWithName( assetPath );
                return new Asset( getContext(), assetPath, assets, uri );
            }
            // Asset not found.
            return null;
        }

        /**
         * List the files contained by the asset directory.
         * Assumes that the current asset resource does represent a directory.
         */
        @Override
        public String[] list() {
            return assets.getAssetNamesUnderPath( getAssetName() );
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return assets.openInputStream( getAssetName() );
        }

        @Override
        public String asString() {
            String s = null;
            try {
                s = Files.readString( openInputStream(), getAssetName() );
            }
            catch(IOException e) {
                Log.e( LogTag, String.format("Reading string from %s", getAssetName()), e );
            }
            return s;
        }

        /** Return the file URL. */
        @Override
        public URI asURL() {
            URI url = null;
            try {
                String path = Paths.join("android_asset", getAssetName() );
                url = new URI( String.format("file:///%s", path ) );
            }
            catch(URISyntaxException e) {
                Log.e( LogTag, "Parsing URL", e );
            }
            return url;
        }

        /**
         * Fetch this resource's image representation.
         * Assumes that the resource's string representation is the assetName of an image to be found in the
         * app's resource bundle.
         */
        @Override
        public Drawable asImage() {
            Drawable image = null;
            try {
                image = Drawable.createFromStream( openInputStream(), getAssetName() );
            }
            catch(FileNotFoundException fnfe) {
                Log.e( LogTag, String.format("Asset not found: %s", getAssetName() ));
            }
            catch(IOException ioe) {
                Log.e( LogTag, String.format("Reading image from %s", getAssetName() ), ioe );
            }
            return image;
        }

    }

}
