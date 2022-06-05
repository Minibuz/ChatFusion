package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;
import fr.umlv.java.models.message.Message;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MessageWriter implements Writer {

    private static final int SIZE_MAX = Byte.BYTES +
            Integer.BYTES + 100 * Byte.BYTES +
            Integer.BYTES + 30 * Byte.BYTES +
            Integer.BYTES + 1024 * Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public MessageWriter(int bufferSize, Message msg) {
        var bufferServerName = UTF_8.encode(msg.getServerName());
        var bufferLogin = UTF_8.encode(msg.getLogin());
        var bufferMessage = UTF_8.encode(msg.getMessage());
        if(bufferSize < Byte.BYTES + Integer.BYTES + bufferServerName.remaining()
                + Integer.BYTES + bufferLogin.remaining() + Integer.BYTES + bufferMessage.remaining()) {
            throw new IllegalStateException();
        }
        if(bufferServerName.remaining() > 100 || bufferLogin.remaining() > 30 || bufferMessage.remaining() > 1024) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.MESSAGE.getValue())
                .putInt(bufferServerName.remaining())
                .put(bufferServerName)
                .putInt(bufferLogin.remaining())
                .put(bufferLogin)
                .putInt(bufferMessage.remaining())
                .put(bufferMessage);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
