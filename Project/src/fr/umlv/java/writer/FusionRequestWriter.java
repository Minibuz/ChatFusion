package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FusionRequestWriter implements Writer {

    private final ByteBuffer bufferOut;

    public FusionRequestWriter(int bufferSize, InetSocketAddress socketAddress) {
        this.bufferOut = ByteBuffer.allocate(
                Byte.BYTES + Byte.BYTES + 8 * 4 * 4 * Byte.BYTES
        );

        var address = socketAddress.getAddress().getAddress();
        var bufferPort = socketAddress.getPort();
        var type = address.length == 4 ? (byte)4 : (byte)6;
        var bufferAddress = ByteBuffer.allocate(8 * 4 * 4 * Byte.BYTES);
        for (var o : address) {
            bufferAddress.put(o);
        }

        if(bufferSize < Byte.BYTES +
                Byte.BYTES + address.length * Byte.BYTES ) {
            throw new IllegalStateException();
        }

        bufferOut.put(OpCode.FUSION_REQUEST.getValue())
                .put(type)
                .put(bufferAddress.flip())
                .putInt(bufferPort);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
