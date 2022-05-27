package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

public class RefusedLoginWriter {

    private static final int SIZE_MAX = Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public RefusedLoginWriter() {
        if(bufferOut.remaining() < Byte.BYTES) {
            throw new IllegalStateException();
        }

        bufferOut.put(OpCode.LOGIN_REFUSED.getValue());
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
