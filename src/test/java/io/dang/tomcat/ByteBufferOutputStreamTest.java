package io.dang.tomcat;

import io.dang.tomcat.io.ByteBufferOutputStream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.testng.Assert.*;

public class ByteBufferOutputStreamTest {

    @Test
    public void writeTest() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        char[] helloWorld = "Hello World".toCharArray();
        byte[] bytes = new String(helloWorld).getBytes();
        bbos.write(bytes);

        ByteBuffer bb = bbos.getByteBuffer();
        // dont think this is right
        bb.limit(0);

        int i = 0;
        while (bb.hasRemaining()) {
            assertEquals(bb.getChar(), helloWorld[i++]);
        }
    }
}
