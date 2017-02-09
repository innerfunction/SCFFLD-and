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
package com.innerfunction.scffld.app;

import com.innerfunction.scffld.Message;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI scheme handler for the post: scheme.
 * The post: scheme allows messages to be posted using a URI string description. For example,
 * the URI post:app#open+view@make:WebView, specifies a message named 'open' which is posted
 * to a target named 'app' with a single parameter named 'view'.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class PostScheme implements URIScheme {

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Message message = new Message( uri.getName(), uri.getFragment(), params );
        return message;
    }

}
