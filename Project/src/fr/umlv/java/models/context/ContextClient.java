package fr.umlv.java.models.context;

import fr.umlv.java.models.ConnectionStatut;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.readers.login.LoginAcceptedReader;
import fr.umlv.java.readers.message.MessageReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;

public class ContextClient {

    static private final Charset UTF_8 = StandardCharsets.UTF_8;
    static private int BUFFER_SIZE = 10_000;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<Message> queue = new ArrayDeque<>();
    private ConnectionStatut connected = ConnectionStatut.NOT_CONNECTED;
    private boolean closed = false;
    private boolean logged = false;
    private Reader<?> reader;
    private final String login;
    private byte currentOpCode = -1;
    private String serverName;

    public ContextClient(SelectionKey key, Boolean logged, String login) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.logged = logged;
        this.login = login;
    }

    /**
     * Process the content of bufferIn
     *
     * The convention is that bufferIn is in write-mode before the call to process
     * and after the call
     *
     */
    public void processIn() {
        if (currentOpCode == -1) {
            bufferIn.flip();
            currentOpCode = bufferIn.get();
            bufferIn.compact();
            reader = Reader.findReader(currentOpCode);
            if (reader == null) {
                currentOpCode = -1;
                return;
            }
        }
        var status = reader.process(bufferIn);
        if (status == Reader.ProcessStatus.ERROR) {
            currentOpCode = -1;
            return;
        }
        if (status == Reader.ProcessStatus.REFILL) {
            return;
        }
        switch (currentOpCode) {
            case 2 -> {
                serverName = (String) reader.get();
                connected = ConnectionStatut.CONNECTED;
                processOut();
            }
            case 3 -> connected = ConnectionStatut.NOT_CONNECTED;
            case 4 -> {
                var msg = (Message) reader.get();
                System.out.println(msg.getLogin() + " : " + msg.getText());
            }
        }
        currentOpCode = -1;
    }

    /**
     * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
     *
     * @param msg
     */
    public void queueMessage(Message msg) {
        queue.add(msg);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bufferOut from the message queue
     */
    public void processOut() {
        if(connected == ConnectionStatut.NOT_CONNECTED) {
            bufferOut.put((byte) 0);
            var bufferLogin = UTF_8.encode(login);
            bufferOut.putInt(bufferLogin.remaining())
                    .put(bufferLogin);
            connected = ConnectionStatut.CONNECTION;
        }
        if (connected == ConnectionStatut.CONNECTION) {
            return;
        }
        var msg = queue.peekFirst();
        if (msg == null) {
            return;
        }
        var login_buffer = UTF_8.encode(msg.getLogin());
        var msg_buffer = UTF_8.encode(msg.getText());
        var servername_buffer = UTF_8.encode(serverName);
        if (bufferOut.remaining() < login_buffer.remaining() + msg_buffer.remaining() + Integer.BYTES*2) {
            return;
        }
        queue.removeFirst();
        bufferOut.put((byte) 4);
        bufferOut.putInt(servername_buffer.remaining());
        bufferOut.put(servername_buffer);
        bufferOut.putInt(login_buffer.remaining());
        bufferOut.put(login_buffer);
        bufferOut.putInt(msg_buffer.remaining());
        bufferOut.put(msg_buffer);
    }

    /**
     * Update the interestOps of the key looking only at values of the boolean
     * closed and of both ByteBuffers.
     *
     * The convention is that both buffers are in write-mode before the call to
     * updateInterestOps and after the call. Also it is assumed that process has
     * been be called just before updateInterestOps.
     */

    public void updateInterestOps() {
        int ops = 0;
        if(!closed && bufferIn.hasRemaining()) ops |= SelectionKey.OP_READ;
        if(bufferOut.position() != 0) ops |= SelectionKey.OP_WRITE;

        if(ops == 0 || !key.isValid()) silentlyClose();
        else key.interestOps(ops);
    }

    public void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    public void doRead() throws IOException {
        closed = (sc.read(bufferIn) == -1);
        processIn();
        updateInterestOps();
    }

    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doWrite and after the call
     *
     * @throws IOException
     */
    public void doWrite() throws IOException {
        processOut();
        sc.write(bufferOut.flip());
        bufferOut.compact();
        updateInterestOps();
    }

    public void doConnect() throws IOException {
        if (!sc.finishConnect())
            return; // the selector gave a bad hint
        key.interestOps(SelectionKey.OP_WRITE);
    }
}
