package io.dang.tomcat.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {

    public static int INITIAL_CAPACITY = 16;

    private ByteBuffer byteBuffer;

    public ByteBufferOutputStream() {
        byteBuffer = ByteBuffer.allocate(INITIAL_CAPACITY);
    }

    public ByteBufferOutputStream(ByteBuffer buffer) {
        byteBuffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        ensureCapacity(byteBuffer.position() + 1);
        byteBuffer.putInt(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        ensureCapacity(byteBuffer.position() + b.length);
        byteBuffer.put(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureCapacity(byteBuffer.position() + len);
        byteBuffer.put(b, off, len);
    }

    @Override
    public void close() throws IOException {
        byteBuffer = null;
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer copy = byteBuffer.asReadOnlyBuffer();
        copy.flip();

        return copy;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= byteBuffer.capacity()) {
            return;
        }
        int oldCapacity = byteBuffer.capacity();
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        ByteBuffer original = byteBuffer;
        byteBuffer = ByteBuffer.allocate(newCapacity);

        original.flip();
        byteBuffer.put(original);
    }
}
