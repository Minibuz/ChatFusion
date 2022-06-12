package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

public class FusionInitKoWriter {

    private static final int SIZE_MAX = Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public FusionInitKoWriter(int bufferSize) {
        if(bufferSize < Byte.BYTES) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.FUSION_INIT_KO.getValue());
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
