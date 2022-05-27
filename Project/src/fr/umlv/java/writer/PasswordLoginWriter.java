package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PasswordLoginWriter {

    private static final int SIZE_MAX = Byte.BYTES +
            Integer.BYTES + 30 * Byte.BYTES +
            Integer.BYTES + 30 * Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public PasswordLoginWriter(String login, String password) {
        var bufferLogin = UTF_8.encode(login);
        var bufferPassword = UTF_8.encode(password);
        if(SIZE_MAX < Byte.BYTES +
                Integer.BYTES + bufferLogin.remaining() +
                Integer.BYTES + bufferPassword.remaining()) {
            throw new IllegalStateException();
        }
        if(bufferLogin.remaining() > 30 || bufferPassword.remaining() > 30) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.LOGIN_PASSWORD.getValue())
                .putInt(bufferLogin.remaining())
                .put(bufferLogin)
                .putInt(bufferPassword.remaining())
                .put(bufferPassword);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
