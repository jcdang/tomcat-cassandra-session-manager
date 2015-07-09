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

    @Test
    public void readTest() throws IOException {
        byte[] bytes = new byte[] {'a', 'b', 'c', 'd', 'e', 'f', 'g',
                                   'h', 'i', 'j', 'k', 'l', 'm', 'n',
                                   'o', 'p', 'q', 'r', 's', 't', 'u',
                                   'v', 'w', 'x', 'y', 'z'};
        ByteBuffer bb = ByteBuffer.wrap(bytes);

        ByteBufferInputStream bis = new ByteBufferInputStream(bb);
        for (int i = 0; i < bytes.length; ++i) {
            assertEquals(bytes.length - i, bis.available());
            assertEquals(bytes[i], bis.read());
        }

    }
}
