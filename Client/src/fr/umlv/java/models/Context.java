package fr.umlv.java.models;

import fr.umlv.java.readers.MessageReader;
import fr.umlv.java.readers.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Context {
    static private int BUFFER_SIZE = 10_000;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<Message> queue = new ArrayDeque<>();
    private ConnectionStatut connected = ConnectionStatut.NOT_CONNECTED;
    private boolean closed = false;
    private boolean logged = false;
    private MessageReader messageReader = new MessageReader();

    public Context(SelectionKey key, Boolean logged) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.logged = logged;
    }

    /**
     * Process the content of bufferIn
     *
     * The convention is that bufferIn is in write-mode before the call to process
     * and after the call
     *
     */
    public void processIn() {
        for (;;) {
            Reader.ProcessStatus status = messageReader.process(bufferIn);
            switch (status) {
                case DONE:
                    var value = messageReader.get();
                    System.out.println(value.getLogin() + " : " + value.getTexte());
                    messageReader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }
        }
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
        if(connected != ConnectionStatut.CONNECTED && connected != ConnectionStatut.CONNECTION) {
            //bufferOut.putInt(1);
        }

        while(!queue.isEmpty()) {
            var message = queue.peek();
            var bufferLogin = UTF_8.encode(message.getLogin());
            var bufferTexte = UTF_8.encode(message.getTexte());
            if(bufferOut.remaining() < Integer.BYTES * 2 + bufferLogin.remaining() + bufferTexte.remaining()) {
                break;
            }
            bufferOut.putInt(bufferLogin.remaining())
                    .put(bufferLogin);
            bufferOut.putInt(bufferTexte.remaining())
                    .put(bufferTexte);
            queue.poll();
        }
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
        key.interestOps(SelectionKey.OP_READ);
    }
}
