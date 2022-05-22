package fr.umlv.java.models.context;

import fr.umlv.java.ServerChatFusion;
import fr.umlv.java.models.fusion.InitFusion;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.models.login.User;
import fr.umlv.java.readers.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.logging.Logger;

public class ContextServer {
    static private final Logger logger = Logger.getLogger(ContextServer.class.getName());
    private static final int BUFFER_SIZE = 10000;
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<Message> queue = new ArrayDeque<>();
    private final ServerChatFusion server;
    private boolean closed = false;
    private String name = null;
    private Reader<?> reader;
    private byte currentOpCode = -1;
    private boolean isServer;

    public ContextServer(ServerChatFusion server, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
    }

    public String getName() {
        return name;
    }

    /**
     * Process the content of bufferIn
     *
     * The convention is that bufferIn is in write-mode before the call to process and
     * after the call
     *
     */
    private void processIn() {
        //TODO
        if (currentOpCode == -1) {
            bufferIn.flip();
            currentOpCode = bufferIn.get();
            bufferIn.compact();
            reader = Reader.findReader(currentOpCode);
            if (reader == null) {
                currentOpCode = -1;
                return;
            }
            if (currentOpCode == 8) {
                isServer = true;
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
        if (isServer || (name != null && (currentOpCode != 0 || currentOpCode != 1)) || (name == null && (currentOpCode == 0 || currentOpCode == 1))) { // On s'assure que l'utilisateur utilise la bonne commande
            switch(currentOpCode) {
                case 0:
                    var user = (User)reader.get();
                    if (!server.getClients().containsKey(user.login())) {
                        server.getClients().put(user.login(), null);
                        this.name = user.login();
                        fillValidConnexion();
                        break;
                    }
                    bufferOut.put((byte) 3);
                    break;
                case 1:
                    user = (User)reader.get();
                    if (!server.getClients().containsKey(user.login())) {
                        server.getClients().put(user.login(), user.password());
                        this.name = user.login();
                        fillValidConnexion();
                        break;
                    }
                    bufferOut.put((byte) 3);
                    break;
                case 4: var msg = (Message) reader.get(); server.broadcast(msg); break;
                case 5: break;
                case 8: var initFusion = (InitFusion) reader.get(); break;
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
        if (name == null) { // Not connected : can't see messages
            return;
        }
        queue.add(msg);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bufferOut from the message queue
     *
     */
    private void processOut() {
        var msg = queue.peekFirst();
        if (msg == null) {
            return;
        }
        var login_buffer = UTF8.encode(msg.getLogin());
        var msg_buffer = UTF8.encode(msg.getText());
        var servername_buffer = UTF8.encode(server.getServerName());
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

    private void updateInterestOps() {
        int ops = 0;
        if(!closed && bufferIn.hasRemaining()) ops |= SelectionKey.OP_READ;
        if(bufferOut.position() != 0) ops |= SelectionKey.OP_WRITE;

        if(ops == 0 || !key.isValid()) silentlyClose();
        else key.interestOps(ops);
    }

    private void silentlyClose() {
        try {
            server.getClients().remove(name);
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
        if (sc.read(bufferIn) == -1) {
            logger.warning("Client closed connexion");
            closed = true;
            updateInterestOps();
            return;
        }
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
        //TODO
        processOut();
        bufferOut.flip();
        var length_write = sc.write(bufferOut);
        if (length_write == 0) {
            logger.warning("Selector lied to me!");
            return;
        }
        bufferOut.compact();
        updateInterestOps();
    }

    private void fillValidConnexion() {
        bufferOut.put((byte) 2);
        var serverBuffer = UTF8.encode(server.getServerName());
        bufferOut.putInt(serverBuffer.remaining());
        bufferOut.put(serverBuffer);
    }

    public void doConnect() throws IOException {
        if (!sc.finishConnect())
            return; // the selector gave a bad hint
        fillInitFusion();
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void fillInitFusion() {
        bufferOut.put((byte) 8);
        var server_buffer = UTF8.encode(server.getServerName());
        bufferOut.putInt(server_buffer.remaining());
        bufferOut.put(server_buffer);
        var socket = server.getServerSocketChannel().socket();
        var address = socket.getInetAddress().getAddress();
        var type = address.length == 4 ? 4 : 6;
        bufferOut.put((byte) type);
        for (var o : address) {
            bufferOut.put(o);
        }
        bufferOut.putInt(socket.getLocalPort());
        var members = server.getMembers();
        bufferOut.putInt(members.size());
        for (var member : members) {
            var member_buffer = UTF8.encode(member);
            bufferOut.putInt(member_buffer.remaining());
            bufferOut.put(member_buffer);
        }
    }
}