// Copyright 2017 InnerFunction Ltd.
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
package com.innerfunction.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Random;

/**
 * A class representing the body of a HTTP request.
 * This class encapsulates the logic for sending either a normal x-www-form-urlencoded form
 * post body, or a multipart form post which includes one or more file contents.
 *
 * Created by juliangoacher on 06/06/2017.
 */
public abstract class RequestBody {

    /**
     * Configure the HTTP connection before sending the request body data.
     * Implementations should use this method to set request headers and the content length.
     */
    public abstract void configureConnection(HttpURLConnection connection) throws IOException;

    /** Write the request body data. */
    public abstract void write(OutputStream out) throws IOException;

    /**
     * Make a request body object suitable for the provided data parameters.
     * If any of the parameters is a File object then will return a multipart request body;
     * otherwise returns a URL encoded request body.
     *
     * @param data The request form data.
     * @return A request body instance.
     */
    static RequestBody makeBodyForData(Map<String,Object> data) {
        boolean multipart = false;
        // If any of the data values is a File instance then the data has to be sent as a
        // multipart form request.
        for( Object value : data.values() ) {
            if( value instanceof File ) {
                multipart = true;
                break;
            }
        }
        return multipart ? new Multipart( data ) : new URLEncoded( data );
    }

    /**
     * A class representing a standard x-www-form-urlencoded form post body.
     */
    public static class URLEncoded extends RequestBody {

        /** The encoded request body. */
        private byte[] body;

        public URLEncoded(Map<String,Object> data) {
            this.body = Client.makeQueryString( data ).getBytes();
        }

        public void configureConnection(HttpURLConnection connection) throws IOException {
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // The following is necessary because the body byte array is generated from the body
            // data using the platform default encoding.
            connection.setRequestProperty("charset", Charset.defaultCharset().name() );
            connection.setFixedLengthStreamingMode( body.length );
        }

        public void write(OutputStream out) throws IOException {
            out.write( body );
        }

    }

    /**
     * A class representing a multipart form post body.
     */
    public static class Multipart extends RequestBody {

        /** Valid bytes for use in a multipart boundary string. */
        static final byte[] MultipartBytes =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
        /** The end of line byte sequence. */
        static final byte[] CRLF = new byte[]{ '\r', '\n' };
        /** The end of body byte sequence. */
        static final byte[] Stop = new byte[]{ '-', '-' };

        /** The different parts of the request body. */
        private RequestPart[] parts;
        /** The boundary string used to separate parts. */
        private byte[] boundary;

        public Multipart(Map<String,Object> data) {
            // Make the request parts for each data field.
            parts = new RequestPart[ data.size() ];
            int i = 0;
            for( String name : data.keySet() ) {
                Object value = data.get( name );
                if( value instanceof File ) {
                    parts[i] = new RequestPart.File( name, (File)value );
                }
                else {
                    parts[i] = new RequestPart.Field( name, value );
                }
                i++;
            }
            // Generate a part boundary.
            boundary = new byte[32];
            boundary[0] = '-';
            boundary[1] = '-';
            Random rnd = new Random();
            for( i = 2; i < boundary.length; i++ ) {
                boundary[i] = MultipartBytes[ rnd.nextInt( MultipartBytes.length )];
            }
        }

        public void configureConnection(HttpURLConnection connection) throws IOException {
            String contentType = "multipart/form-data; boundary=".concat( new String( boundary ) );
            connection.setRequestProperty("Content-Type", contentType );
            // Calculate the body length. This is:
            // - the length of the boundary preceding each part + 2 bytes for the trailing CRLF
            // - plus the length of each part + 2 bytes for the trailing CRLF
            // - plus the length of the final boundary, + 2 bytes for the closing --
            long length = 0;
            for( RequestPart part : parts ) {
                length += boundary.length;
                length += CRLF.length;
                length += part.getLength();
                length += CRLF.length;
            }
            length += boundary.length;
            length += Stop.length;
            connection.setFixedLengthStreamingMode( length );
        }

        public void write(OutputStream out) throws IOException {
            for( RequestPart part : parts ) {
                out.write( boundary );
                out.write( CRLF );
                part.write( out );
                out.write( CRLF );
            }
            out.write( boundary );
            out.write( Stop );
        }

    }


}
