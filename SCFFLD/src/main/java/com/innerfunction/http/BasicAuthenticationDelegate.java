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

import android.net.Uri;
import android.util.Base64;

import com.innerfunction.q.Q;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An authentication delegate which performs HTTP Basic authentication.
 * Subclass must implement a method which returns authentication credentials for requests.
 *
 * Created by juliangoacher on 20/03/2017.
 */
public abstract class BasicAuthenticationDelegate implements AuthenticationDelegate {

    /**
     * A map of previously used authentication tokens, keyed by authentication scope.
     * This map is used to perform preemptive authentication, as described in
     * https://tools.ietf.org/html/rfc7617#section-2.2
     */
    private Map<String,String> authTokensByAuthenticationScope = new HashMap<>();

    /**
     * Get the authentication scope of a request.
     * Authentication scope is defined in RFC 7617, see link above.
     */
    private String getAuthenticationScope(Request request) {
        String url = request.getURL().toString();
        int idx = url.lastIndexOf('/');
        return idx > -1 ? url.substring( 0, idx ) : url;
    }

    /**
     * Add a newly used authentication token to the list of previously used tokens.
     * This method removes any previously used entries which have the current URL as a
     * prefix.
     *
     * @param request       The current request.
     * @param authToken     The authentication token; the full Authorization header value.
     */
    private void addAuthTokenForURL(Request request, String authToken) {
        String newAuthScope = getAuthenticationScope( request );
        Set<String> keySet = authTokensByAuthenticationScope.keySet();
        for( String key : keySet ) {
            if( key.startsWith( newAuthScope ) ) {
                keySet.remove( key );
            }
        }
        authTokensByAuthenticationScope.put( newAuthScope, authToken );
    }

    @Override
    public void prepareRequest(Client client, Request request) {
        // Preemptively set the authorization token if an auth scope is found.
        String requestAuthScope = getAuthenticationScope( request );
        for( String authScope : authTokensByAuthenticationScope.keySet() ) {
            if( requestAuthScope.startsWith( authScope ) ) {
                request.setHeader("Authorization", authTokensByAuthenticationScope.get( authScope ) );
                break;
            }
        }
    }

    @Override
    public boolean isAuthenticationChallenge(Client client, Request request, Response response) {
        return response.getStatusCode() == 401;
    }

    @Override
    public Q.Promise<Request> authenticate(Client client, Request request, Response response) {
        String method = response.getAuthMethod();
        // Check that we're dealing with basic authentication.
        if( "Basic".equals( method ) ) {
            // Try to fetch credentials for the authentication realm.
            String realm = response.getAuthRealm();
            URL url = request.getURL();
            PasswordAuthentication credentials = getCredentials( realm, url );
            if( credentials != null ) {
                // Generate the authentication header. Note that the Smokestack server
                // allows the fields in the auth token to be URI encoded - this is so
                // that colons in either field don't cause a problem when it is parsed.
                String username = Uri.encode( credentials.getUserName() );
                String password = Uri.encode( new String( credentials.getPassword() ) );
                byte[] authToken = (username + ":" + password).getBytes();
                String header = "Basic " + Base64.encodeToString( authToken, Base64.NO_WRAP );
                // Set the authentication header and return; the request will be resent
                // with the additional header.
                addAuthTokenForURL( request, header );
                request.setHeader("Authorization", header );
                return Q.resolve( request );
            }
            // No credentials found.
            return Q.reject( "No credentials found for HTTP authentication realm: " + realm );
        }
        // Authentication method not supported.
        return Q.reject( "Unsupported HTTP authentication method: " + method );
    }

    public abstract PasswordAuthentication getCredentials(String realm, URL url);

}
