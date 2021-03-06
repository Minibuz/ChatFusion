package fr.umlv.java.context;

import fr.umlv.java.models.ConnectionStatut;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.writer.AnonymousLoginWriter;
import fr.umlv.java.writer.MessageWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.logging.Logger;

public class ContextClient {

    static private final Logger logger = Logger.getLogger(ContextClient.class.getName());
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
                System.out.println(msg.getLogin() + " : " + msg.getMessage());
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
        msg.setServerName(this.serverName);
        queue.add(msg);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bufferOut from the message queue
     */
    public void processOut() {
        if(connected == ConnectionStatut.NOT_CONNECTED) {
            bufferOut.put(new AnonymousLoginWriter(bufferOut.remaining(), login).toByteBuffer());
            connected = ConnectionStatut.CONNECTION;
        }
        if (connected == ConnectionStatut.CONNECTION) {
            return;
        }
        var msg = queue.peekFirst();
        if(msg == null) {
            return;
        }
        if(msg.getMessage().isEmpty() || msg.getMessage().isBlank()) {
            logger.info("Empty message");
            return;
        }
        bufferOut.put(new MessageWriter(bufferOut.remaining(), msg).toByteBuffer());
        queue.removeFirst();
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
