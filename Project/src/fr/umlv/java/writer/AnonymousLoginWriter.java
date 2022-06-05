package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AnonymousLoginWriter implements Writer {

    private static final int SIZE_MAX = Byte.BYTES + Integer.BYTES + 30 * Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public AnonymousLoginWriter(int bufferSize, String login) {
        var bufferLogin = UTF_8.encode(login);
        if(bufferSize < Integer.BYTES + Integer.BYTES + bufferLogin.remaining()) {
            throw new IllegalStateException();
        }
        if(bufferLogin.remaining() > 30) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.LOGIN_ANONYMOUS.getValue())
                .putInt(bufferLogin.remaining())
                .put(bufferLogin);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
