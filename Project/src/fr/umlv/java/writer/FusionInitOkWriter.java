package fr.umlv.java.writer;

import fr.umlv.java.models.OpCode;

import java.net.ServerSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FusionInitOkWriter implements Writer {

    private ByteBuffer bufferOut;

    public FusionInitOkWriter(int bufferSize, String serverName, ServerSocket serverSocket, List<String> servers) {
        bufferOut = ByteBuffer.allocate(
                Byte.BYTES + Integer.BYTES + 100 * Byte.BYTES +
                        Byte.BYTES + 8 * 4 * 4 * Byte.BYTES +
                        Integer.BYTES + servers.size() * 100 * Byte.BYTES
        );

        var bufferServerName = UTF_8.encode(serverName);
        var address = serverSocket.getInetAddress().getAddress();
        System.out.println(Arrays.toString(address));
        var bufferPort = serverSocket.getLocalPort();
        var type = address.length == 4 ? (byte)4 : (byte)6;
        var bufferAddress = ByteBuffer.allocate(8 * 4 * 4 * Byte.BYTES);
        for (var o : address) {
            bufferAddress.put(o);
        }
        var sizeServers = 0;
        var listBufferMember = new ArrayList<ByteBuffer>();
        for (var member : servers) {
            var member_buffer = UTF_8.encode(member);
            listBufferMember.add(member_buffer.flip());
            sizeServers += member_buffer.remaining();
        }

        // VERIFICATION ICI
        if(bufferSize < Byte.BYTES +
                Integer.BYTES + bufferServerName.remaining() +
                Byte.BYTES + address.length * Byte.BYTES +
                Integer.BYTES + sizeServers * Byte.BYTES) {
            throw new IllegalStateException();
        }
        if(bufferServerName.remaining() > 100 ||
                listBufferMember.stream().map(Buffer::remaining).anyMatch(element -> element > 100)) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(OpCode.FUSION_INIT_OK.getValue())
                .putInt(bufferServerName.remaining())
                .put(bufferServerName)
                .put(type)
                .put(bufferAddress.flip())
                .putInt(bufferPort)
                .putInt(servers.size());
        for(var bufferMember : listBufferMember) {
            bufferOut.putInt(bufferMember.remaining());
            bufferOut.put(bufferMember);
        }
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut.flip();
    }
}
