package io.dang.tomcat.bytebuffer;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

    ByteBuffer byteBuffer;

    public ByteBufferInputStream(ByteBuffer bb){
        this.byteBuffer = bb;
    }

    @Override
    public synchronized int read() throws IOException {
        if (!byteBuffer.hasRemaining()) {
            return -1;
        }
        return byteBuffer.get();
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }

        len = Math.min(len, byteBuffer.remaining());
        byteBuffer.get(b, off, len);

        if (len == 0) {
            return -1;
        }

        return len;
    }

    @Override
    public int available() throws IOException {
        return byteBuffer.remaining();
    }

    @Override
    public void close() throws IOException {
        byteBuffer = null;
    }
}