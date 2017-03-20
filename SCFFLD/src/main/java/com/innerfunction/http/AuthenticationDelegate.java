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

import com.innerfunction.q.Q;

/**
 * An interface to be implemented by classes providing HTTP authentication related functionality.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public interface AuthenticationDelegate {

    /**
     * Test whether a response represents an authentication challenge.
     *
     * @param client    The HTTP client being used.
     * @param request   The request submitted to the server.
     * @param response  The response returned by the server.
     */
    boolean isAuthenticationChallenge(Client client, Request request, Response response);

    /**
     * Perform an authentication.
     * This method should submit appropriate authentication credentials to the server before
     * continuing. This may be done by modifying the current request (e.g. by appending an
     * authentication header), or by submitting a new request.
     *
     * @param client    The HTTP client being used.
     * @param request   The request submitted to the server. The same request will be resubmitted
     *                  after authentication.
     * @param response  The response returned by the server.
     */
    Q.Promise<Response> authenticate(Client client, Request request, Response response);

}
