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

import com.innerfunction.scffld.Configuration;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI handler for the make: scheme.
 * The make: scheme allows objects to be instantiated from pre-defined configuration
 * patterns, or directly from configuration files.
 * When used to instantiate a pattern, the name of the pattern must be specified in the name
 * part of the URI. The URI's parameters are then passed to the pattern as configuration
 * parameters.
 * When used to instantiate an object from a configuration file, the name part of the URI
 * is left empty, and a single config parameter must be given, resolving to the configuration
 * to be instantiated.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class MakeScheme implements URIScheme {

    private AppContainer container;

    public MakeScheme(AppContainer container) {
        this.container = container;
    }

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Object result = null;
        Configuration config = null;
        String name = uri.getName();
        if( name != null && name.length() > 0 ) {
            // Build a pattern.
            Configuration patterns = container.getPatterns();
            config = patterns.getValueAsConfiguration( name );
        }
        else {
            // Build a configuration.
            Object configParam = params.get("config");
            if( configParam instanceof Configuration ) {
                config = (Configuration)configParam;
            }
            else if( configParam instanceof Resource ) {
                config = new Configuration( (Resource)configParam );
            }
        }
        if( config != null ) {
            result = container.buildObjectWithData( config, params, uri.toString() );
        }

        return result;
    }

}
