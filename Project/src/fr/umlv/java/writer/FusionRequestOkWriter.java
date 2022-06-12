package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

public class FusionRequestOkWriter implements Writer {

    private static final int SIZE_MAX = Byte.BYTES + Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public FusionRequestOkWriter(int bufferSize, Byte statut) {
        if(bufferSize < Byte.BYTES + Byte.BYTES) {
            throw new IllegalStateException();
        }

        bufferOut.put(OpCode.FUSION_ANSWER.getValue())
                .put(statut);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
