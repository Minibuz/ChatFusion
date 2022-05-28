package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AcceptedLoginWriter implements Writer {

    private static final int SIZE_MAX = Byte.BYTES + Integer.BYTES + 100 * Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public AcceptedLoginWriter(String serverName) {
        var bufferServerName = UTF_8.encode(serverName);
        if(SIZE_MAX < Byte.BYTES + Integer.BYTES + bufferServerName.remaining()) {
            throw new IllegalStateException();
        }
        if(bufferServerName.remaining() > 100) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.LOGIN_ACCEPTED.getValue())
                .putInt(bufferServerName.remaining())
                .put(bufferServerName);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
