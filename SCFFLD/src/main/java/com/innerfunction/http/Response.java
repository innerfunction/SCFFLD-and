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
package com.innerfunction.http;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.innerfunction.util.Regex;

import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An HTTP response.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public class Response {

    static final String Tag = Response.class.getSimpleName();

    /** The request URL. */
    private String url;
    /** The HTTP response code. */
    private int statusCode;
    /** The HTTP response message. */
    private String message;
    /** The response content encoding. */
    private String contentEncoding;
    /** The response content type. */
    private String contentType;
    /** The full set of response headers. */
    private Map<String,List<String>> headers;
    /** The response body. Will be null for file responses. */
    private byte[] body;
    /** A file containing the response. */
    private File dataFile;

    private Response(URL url, HttpURLConnection connection) throws IOException {
        this.url = url.toString();
        this.statusCode = connection.getResponseCode();
        this.message = connection.getResponseMessage();
        this.contentEncoding = connection.getContentEncoding();
        this.contentType = connection.getContentType();
        // Strip any trailing parameters from the content type.
        int idx = contentType.indexOf(';');
        if( idx > -1 ) {
            // If the connection hasn't returned a content encoding then try reading one from the
            // content-type header parameters.
            if( contentEncoding == null ) {
                String parameters = contentType.substring( idx + 1 );
                String[] g = Regex.matches(".*charset=([^\\s]+).*", parameters );
                if( g.length > 1 ) {
                    contentEncoding = g[1];
                }
            }
            contentType = contentType.substring( 0, idx );
        }
        this.headers = connection.getHeaderFields();
    }

    Response(URL url, HttpURLConnection connection, byte[] body) throws IOException {
        this( url, connection );
        this.body = body;
    }

    Response(URL url, HttpURLConnection connection, File dataFile) throws IOException {
        this( url, connection );
        this.dataFile = dataFile;
    }

    private String readContentEncoding(HttpURLConnection connection) {
        String encoding = connection.getContentEncoding();
        return encoding == null ? "utf-8" : encoding;
    }

    public String getRequestURL() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAuthMethod() {
        String[] fields = getAuthFields();
        return fields.length > 0 ? fields[0] : null;
    }

    public String getAuthRealm() {
        String fields[] = getAuthFields();
        if( fields.length > 1 ) {
            String field = fields[1];
            if( field.startsWith("realm=\"") && field.endsWith("\"") ) {
                return field.substring( 7, field.length() - 1 );
            }
        }
        return null;
    }

    public String[] getAuthFields() {
        String[] fields = new String[0];
        List<String> headers = this.headers.get( "WWW-Authenticate" );
        if( headers != null && headers.size() > 0 ) {
            String header = headers.get( 0 );
            fields = header.split( " " );
        }
        return fields;
    }

    public Map<String,List<String>> getHeaders() {
        return headers;
    }

    public File getDataFile() {
        return dataFile;
    }

    public byte[] getRawBody() {
        return body;
    }

    public String getBody() {
        try {
            return new String( body, contentEncoding );
        }
        catch(UnsupportedEncodingException e) {
            Log.e( Tag, String.format( "Bad content encoding when reading response body: %s", contentEncoding ) );
        }
        return null;
    }

    public Object parseBodyData() {
        if( "application/json".equals( contentType ) ) {
            String json = getBody();
            if( json != null ) {
                return JSONValue.parse( json );
            }
        }
        else if( "application/x-www-form-urlencoded".equals( contentType ) ) {
            String data = getBody();
            if( data != null ) {
                Map<String,Object> result = new HashMap<>();
                String[] pairs = TextUtils.split( data, "&" );
                for( String pair : pairs ) {
                    String[] keyValue = TextUtils.split( pair, "=" );
                    String key = Uri.decode( keyValue[0] );
                    String value = Uri.decode( keyValue[1] );
                    result.put( key, value );
                }
                return result;
            }
        }
        return null;
    }
}
