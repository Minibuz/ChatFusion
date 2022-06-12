package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FusionInitForwardWriter implements Writer {
    private final ByteBuffer bufferOut;

    public FusionInitForwardWriter(int bufferSize, Socket serverSocket) {
        this.bufferOut = ByteBuffer.allocate(
                Byte.BYTES + Byte.BYTES + 8 * 4 * 4 * Byte.BYTES
        );

        var address = serverSocket.getInetAddress().getAddress();
        var bufferPort = serverSocket.getPort();
        var type = address.length == 4 ? (byte)4 : (byte)6;
        var bufferAddress = ByteBuffer.allocate(8 * 4 * 4 * Byte.BYTES);
        for (var o : address) {
            bufferAddress.put(o);
        }

        if(bufferSize < Byte.BYTES +
                Byte.BYTES + address.length * Byte.BYTES ) {
            throw new IllegalStateException();
        }

        bufferOut.put(OpCode.FUSION_INIT_FWD.getValue())
                .put(type)
                .put(bufferAddress.flip())
                .putInt(bufferPort);
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
