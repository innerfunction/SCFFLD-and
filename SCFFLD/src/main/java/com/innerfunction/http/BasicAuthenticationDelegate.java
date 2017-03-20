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

/**
 * An authentication delegate which performs HTTP Basic authentication.
 * Subclass must implement a method which returns authentication credentials for requests.
 *
 * Created by juliangoacher on 20/03/2017.
 */
public abstract class BasicAuthenticationDelegate implements AuthenticationDelegate {

    /**
     * An array of previously used authentication tokens, keyed by authentication scope.
     * This array is used to perform preemptive authentication, as described in
     * https://tools.ietf.org/html/rfc7617#section-2.2
     * The first column of the array holds the authentication scope, the second column holds
     * the authentication token.
     */
    private String[][] authenticationScopes = new String[0][];

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
        // Search for and remove any previously set auth scopes which are superceded - i.e.
        // contained by - the one.
        int count = authenticationScopes.length, removed = 0;
        for( int i = 0; i < count; i++ ) {
            String authScope = authenticationScopes[i][0];
            if( authScope.startsWith( newAuthScope ) ) {
                authenticationScopes[i] = null;
                removed++;
            }
        }
        // Create a new array, put the new scope at the start and copy the remaining none
        // deleted scopes to the end. Note that this will have the effect of moving shorter
        // - i.e. more general - scopes to the beginning of the array.
        String[][] updatedAuthScopes = new String[count - removed + 1][];
        updatedAuthScopes[0] = new String[]{ newAuthScope, authToken };
        for( int i = 0, j = 1; i < count; i++ ) {
            if( authenticationScopes[i] != null ) {
                updatedAuthScopes[j++] = authenticationScopes[i];
            }
        }
        // Replace scopes list with updated copy.
        authenticationScopes = updatedAuthScopes;
    }

    @Override
    public void prepareRequest(Client client, Request request) {
        // Preemptively set the authorization token if an auth scope is found.
        String requestAuthScope = getAuthenticationScope( request );
        for( int i = 0; i < authenticationScopes.length; i++ ) {
            if( requestAuthScope.startsWith( authenticationScopes[i][0] ) ) {
                request.setHeader("Authorization", authenticationScopes[i][1] );
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
