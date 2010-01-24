package net.sf.maven.plugin.autotools;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class LineBreakingOutputStreamTest {

    @Test
    public void simpleByteSequence()
    throws Exception {
        final List<String> result = new ArrayList<String>();
        LineBreakingOutputStream lbos = new LineBreakingOutputStream() {
            @Override
            public void writeLine(String line)
            throws IOException {
                result.add(line);
            }
        };
        String[] expected = { "a\u00dfcd\u00e4fgh", "\u00b5@\u20ac" };
        lbos.write(expected[0].getBytes("UTF-8"));
        lbos.write(10);
        lbos.write(expected[1].getBytes("UTF-8"));
        lbos.close();
        String[] actual = result.toArray(new String[result.size()]);
        assertArrayEquals(expected, actual);
    }


    @Test
    public void writeFallbackForInvalidByteSequence()
    throws Exception {
        final List<String> result = new ArrayList<String>();
        LineBreakingOutputStream lbos = new LineBreakingOutputStream() {
            @Override
            public void writeLine(String line)
            throws IOException {
                result.add(line);
            }
        };
        lbos.write(new byte[] { -61, -20 });
        lbos.close();
        String[] expected = { "\uffc3\uffec" };
        String[] actual = result.toArray(new String[result.size()]);
        assertArrayEquals(expected, actual);
    }

}

