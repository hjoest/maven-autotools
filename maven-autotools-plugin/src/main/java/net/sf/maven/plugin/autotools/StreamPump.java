/*
 * Copyright (C) 2006-2007 Holger Joest <hjoest@users.sourceforge.net>
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
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Copies an input stream to an output stream.
 */
class StreamPump
implements Runnable {

    private static final int BUF_SIZE = 1024;

    /** */
    private Thread pumpThread;

    /** */
    private InputStream in;

    /** */
    private OutputStream out;

    /** */
    private volatile boolean finished;


    /**
     * Creates a stream pump.
     *
     * @param in an input stream
     * @param out an output stream
     */
    public StreamPump(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }


    /**
     * Starts this stream pump.
     */
    public void start() {
        if (pumpThread != null) {
            throw new IllegalStateException("Stream pump already started");
        }
        pumpThread = new Thread(this);
        pumpThread.setDaemon(true);
        pumpThread.start();
    }


    /**
     * Stops this stream pump.
     */
    public void stop() {
        try {
            pumpThread.join();
        } catch (InterruptedException e) {
        }
        try {
            out.flush();
        } catch (IOException e) {
        }
    }


    /**
     * Copies data from the input stream to the output stream.  It stops if
     * the end of the input stream has been reached or an error occurs.
     */
    public synchronized void run() {
        byte[] buf = new byte[BUF_SIZE];

        try {
            int size;
            while ((size = in.read(buf)) > 0) {
                out.write(buf, 0, size);
            }
        } catch (Exception ex) {
        } finally {
            try {
                out.flush();
            } catch (IOException ex) {
            }
            finished = true;
            notifyAll();
        }
    }


    /**
     * Wait until the input stream is exhausted.
     *
     * @throws InterruptedException if the thread stops prematurely
     */
    public synchronized void waitFor()
    throws InterruptedException {
        while (!finished) {
            wait();
        }
    }

}

