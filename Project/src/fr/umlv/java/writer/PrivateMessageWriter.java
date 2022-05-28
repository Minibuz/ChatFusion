package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;
import fr.umlv.java.models.message.PrivateMessage;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PrivateMessageWriter implements Writer {

    private static final int SIZE_MAX = Byte.BYTES +
            Integer.BYTES + 100 * Byte.BYTES +
            Integer.BYTES + 30 * Byte.BYTES +
            Integer.BYTES + 100 * Byte.BYTES +
            Integer.BYTES + 30 * Byte.BYTES +
            Integer.BYTES + 1024 * Byte.BYTES;
    private final ByteBuffer bufferOut = ByteBuffer.allocate(SIZE_MAX);

    public PrivateMessageWriter(PrivateMessage privateMessage) {
        var bufferServerSrc = UTF_8.encode(privateMessage.getServerSrc());
        var bufferLoginSrc = UTF_8.encode(privateMessage.getLoginSrc());
        var bufferServerDst = UTF_8.encode(privateMessage.getServerDst());
        var bufferLoginDst = UTF_8.encode(privateMessage.getLoginDst());
        var bufferMessage = UTF_8.encode(privateMessage.getMessage());

        if(bufferOut.remaining() < Byte.BYTES + Integer.BYTES + bufferServerSrc.remaining()
                + Integer.BYTES + bufferLoginSrc.remaining()
                + Integer.BYTES + bufferServerDst.remaining()
                + Integer.BYTES + bufferLoginDst.remaining()
                + Integer.BYTES + bufferMessage.remaining()) {
            throw new IllegalStateException();
        }
        if(bufferServerSrc.remaining() > 100 || bufferLoginSrc.remaining() > 30
                || bufferServerDst.remaining() > 100 || bufferLoginDst.remaining() > 30
                || bufferMessage.remaining() > 1024) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.MESSAGE_PRIVATE.getValue())
                .putInt(bufferServerSrc.remaining())
                .put(bufferServerSrc)
                .putInt(bufferLoginSrc.remaining())
                .put(bufferLoginSrc)
                .putInt(bufferServerDst.remaining())
                .put(bufferServerDst)
                .putInt(bufferLoginDst.remaining())
                .put(bufferLoginDst)
                .putInt(bufferMessage.remaining())
                .put(bufferMessage);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
