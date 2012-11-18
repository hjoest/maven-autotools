/*
 * Copyright (C) 2006-2012 Holger Joest <holger@joest.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.maven.plugin.autotools;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;


public abstract class LineBreakingOutputStream
extends OutputStream {

    private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    private int pending;

    private ByteBuffer bytes = ByteBuffer.wrap(new byte[4]);

    private CharBuffer chars = CharBuffer.allocate(1);

    private StringBuffer line = new StringBuffer();


    public abstract void writeLine(String line)
    throws IOException;


    @Override
    public void write(int b)
    throws IOException {
        bytes.position(pending);
        bytes.put((byte) b);
        pending++;
        bytes.position(0);
        CoderResult result = decoder.decode(bytes, chars, false);
        if (result.isMalformed()) {
            if (pending > 3) {
                encodeFallback();
            }
        } else {
            chars.rewind();
            char ch = chars.get();
            switch (ch) {
            case 10:
            case 13:
                pending = 0;
                flush();
                break;
            default:
                line.append(ch);
            }
            resetDecoder();
        }
    }


    @Override
    public void close()
    throws IOException {
        flush();
        super.close();
    }


    @Override
    public void flush()
    throws IOException {
        if (pending > 0) {
            encodeFallback();
        }
        if (line.length() > 0) {
            writeLine(line.toString());
            line.setLength(0);
        }
        super.flush();
    }


    private void resetDecoder() {
        bytes.clear();
        bytes.put(new byte[4]);
        chars.clear();
        pending = 0;
    }


    private void encodeFallback() {
        for (int p = 0; p < pending; ++p) {
            line.append((char) bytes.get(p));
        }
        resetDecoder();
    }

}

