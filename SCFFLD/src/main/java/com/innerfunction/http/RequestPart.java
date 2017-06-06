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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A class used to represent a single part of a HTTP multipart form request.
 *
 * Created by juliangoacher on 06/06/2017.
 */
public abstract class RequestPart {

    /** The end of line byte sequence. */
    static final byte[] CRLF = new byte[]{ '\r', '\n' };

    /** Return the total byte length of this request part. */
    public abstract long getLength();

    /** Write the request part. */
    public abstract void write(OutputStream out) throws IOException;

    /**
     * A request part representing a simple name=value field.
     */
    public static class Field extends RequestPart {

        /** The part's encoded byte value. */
        private byte[] data;

        public Field(String name, Object value) {
            StringBuilder sb = new StringBuilder();
            sb.append("Content-Disposition: form-data; name=\"");
            sb.append( name );
            sb.append("\"");
            sb.append( CRLF );
            sb.append( CRLF );
            sb.append( value );
            sb.append( CRLF );
            data = sb.toString().getBytes();
        }

        public long getLength() {
            return data.length;
        }

        public void write(OutputStream out) throws IOException {
            out.write( data );
        }
    }

    /**
     * A request part representing a file's contents.
     */
    public static class File extends RequestPart {

        /** Size of the buffer used when streaming the file's contents. */
        static final int BufferSize = 4096;

        /** The prelude that appears before the file contents. */
        private byte[] prelude;
        /** The file being represented. */
        private java.io.File data;

        public File(String name, java.io.File value) {
            StringBuilder sb = new StringBuilder();
            sb.append("Content-Disposition: form-data; name=\"");
            sb.append( name );
            sb.append("\"; filename=\"");
            sb.append( value.getName() );
            sb.append("\"");
            sb.append( CRLF );
            sb.append("Content-Type: application/octet-stream");
            sb.append( CRLF );
            sb.append( CRLF );
            prelude = sb.toString().getBytes();
            data = value;
        }

        public long getLength() {
            return prelude.length + data.length();
        }

        public void write(OutputStream out) throws IOException {
            InputStream in = new FileInputStream( data );
            try {
                byte[] buffer = new byte[ BufferSize ];
                while( true ) {
                    int read = in.read( buffer );
                    if( read < 0 ) break;
                    out.write( buffer, 0, read );
                }
            }
            finally {
                try {
                    in.close();
                }
                catch(Exception e) {}
            }
        }
    }
}
