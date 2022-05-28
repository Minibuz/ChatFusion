package fr.umlv.java.writer;

import java.nio.ByteBuffer;

public class PrivateFileWriter implements Writer {

    private static final int SIZE_MAX = 0;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }

    //Not implemented yet
}
