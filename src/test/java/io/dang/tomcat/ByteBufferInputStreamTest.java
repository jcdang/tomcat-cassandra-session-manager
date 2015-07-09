package io.dang.tomcat;

import io.dang.tomcat.io.ByteBufferInputStream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.testng.Assert.*;

public class ByteBufferInputStreamTest {

    byte[] alpha = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                                'h', 'i', 'j', 'k', 'l', 'm', 'n',
                                'o', 'p', 'q', 'r', 's', 't', 'u',
                                'v', 'w', 'x', 'y', 'z' };

    @Test
    public void readTest() throws IOException {

        ByteBuffer bb = ByteBuffer.wrap(alpha);

        ByteBufferInputStream bis = new ByteBufferInputStream(bb);
        for (int i = 0; i < alpha.length; ++i) {
            assertEquals(alpha.length - i, bis.available());
            assertEquals(alpha[i], bis.read());
        }
    }

    @Test
    public void skipTest() {
        ByteBuffer bb = ByteBuffer.wrap(alpha);
        ByteBufferInputStream bis = new ByteBufferInputStream(bb);

        bis.
    }
}
