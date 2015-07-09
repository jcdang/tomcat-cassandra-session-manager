package io.dang.tomcat.io;

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
        // Method contract states the return is a byte even though the signature is returning int
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
    public long skip(long n) throws IOException {
        long minInput = Math.min(n, (long)Integer.MAX_VALUE);
        int offset = (int) minInput;

        byteBuffer.position(byteBuffer.position() + offset);

        return byteBuffer.position();
    }

    @Override
    public synchronized void reset() throws IOException {
        byteBuffer.reset();
    }

    @Override
    public synchronized void mark(int readlimit) {
        byteBuffer.mark();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() throws IOException {
        byteBuffer = null;
    }
}