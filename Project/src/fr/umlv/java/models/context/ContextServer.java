package fr.umlv.java.models.context;

import fr.umlv.java.ServerChatFusion;
import fr.umlv.java.models.fusion.InitFusion;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.models.login.User;
import fr.umlv.java.readers.Reader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
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
    private boolean fusionInDoing = false;

    public ContextServer(ServerChatFusion server, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public boolean isServer() { return isServer; }
    /**
     * Process the content of bufferIn
     *
     * The convention is that bufferIn is in write-mode before the call to process and
     * after the call
     *
     */
    private void processIn() {
        // OpCode not yet read
        if (currentOpCode == -1) {
            bufferIn.flip();
            currentOpCode = bufferIn.get();
            bufferIn.compact();
            reader = Reader.findReader(currentOpCode);
            if (reader == null) {
                currentOpCode = -1;
                return;
            }
            if (name == null && currentOpCode == 8) {
                isServer = true;
            }
        }

        // Process the byteBuffer in entry
        if (processEntry()) return;

        // If a server is messaging us
        if(isServer) {
            processServer();
            return;
        }

        // First connexion
        if(name == null) {
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
                case 1: // This shouldn't happen
                    user = (User)reader.get();
                    if (!server.getClients().containsKey(user.login())) {
                        server.getClients().put(user.login(), user.password());
                        this.name = user.login();
                        fillValidConnexion();
                        break;
                    }
                    bufferOut.put((byte) 3);
                    break;
                default:
                    // Error
                    break;
            }
        }
        // Already connected
        else {
            switch(currentOpCode) {
                case 4:
                    var msg = (Message) reader.get();
                    server.broadcast(msg, null);
                    break;
                case 5:
                    break;
                default:
                    // Error
                    break;
            }
        }
        currentOpCode = -1;
    }

    private void processServer() {
            switch (currentOpCode) {
                case 4 -> {
                    var msg = (Message) reader.get();
                    server.broadcast(msg, key);
                    System.out.println("test");
                }
                case 8 -> {
                    var initFusion = (InitFusion) reader.get();
                    if (server.isLeader()) {
                        if (initFusion.getMembers().stream().noneMatch(m -> server.getMembers().contains(m))) { // Check names in common
                            // Send OpCode 9
                            bufferOut.put((byte) 9);
                            fillInitFusion();
                            // Sending OpCode 14 if changing leader
                            changeLeader(initFusion);
                        } else {
                            // FUSION INIT KO
                            bufferOut.put((byte) 10);
                        }
                    } else {
                        fillInitFusionFwd(); // Sending OpCode 11
                    }
                }
                case 9 -> {
                    var initFusion = (InitFusion) reader.get();
                    // Sending OpCode 14 if changing leader
                    changeLeader(initFusion);
                    System.out.println("Fusion done");
                }
                case 12 -> {
                    var adressServer = (InetSocketAddress) reader.get();
                    bufferOut.put((byte)13);
                    if(fusionInDoing) {
                        bufferOut.put((byte)0);
                    } else {
                        bufferOut.put((byte)1);
                    }
                    try {
                        server.swapFusion(adressServer);
                    } catch (IOException e) {
                        logger.info("SwapFusion broken");
                        return;
                    }
                }
                case 14 -> {
                    // Receive change leader
                    var adressServer = (InetSocketAddress) reader.get();
                    try {
                        server.swapFusion(adressServer);
                    } catch (IOException e) {
                        logger.info("SwapFusion broken");
                        return;
                    }
                }
            }
            currentOpCode = -1;
    }

    private boolean processEntry() {
        var status = reader.process(bufferIn);
        if (status == Reader.ProcessStatus.ERROR) {
            currentOpCode = -1;
            return true;
        }
        if (status == Reader.ProcessStatus.REFILL) {
            return true;
        }
        return false;
    }

    /**
     * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
     *
     * @param msg
     */
    public void queueMessage(Message msg) {
        if (name == null && !isServer) { // Not connected : can't see messages
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
        if (bufferOut.remaining() < servername_buffer.remaining() + login_buffer.remaining() + msg_buffer.remaining() + Byte.BYTES + Integer.BYTES*3) {
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
        isServer = true;
        if(server.isLeader()) {
            // Fusion init
            bufferOut.put((byte) 8);
            fillInitFusion();
        } else {
            // Fusion request
            bufferOut.put((byte) 12);
            fillFusionRequest();
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * Filling InitFusion request without the OpCode
     */
    public void fillInitFusion() {
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

    public void fillInitFusionFwd() {
        bufferOut.put((byte) 11);
        var socket = server.getFusionSc().socket();
        var address = socket.getInetAddress().getAddress();
        var type = address.length == 4 ? 4 : 6;
        bufferOut.put((byte) type);
        for (var o : address) {
            bufferOut.put(o);
        }
        bufferOut.putInt(socket.getLocalPort());
    }

    public void fillFusionRequest() {
        bufferOut.put((byte) 12);
        var socket = server.getFusionSc().socket();
        var address = socket.getInetAddress().getAddress();
        var type = address.length == 4 ? 4 : 6;
        bufferOut.put((byte) type);
        for (var o : address) {
            bufferOut.put(o);
        }
        bufferOut.putInt(socket.getLocalPort());
    }

    public void changeLeader(InitFusion initFusion) {
        if (server.getServerName().compareTo(initFusion.getServerName()) > 0) {
            server.unsetLeader();
            server.broadcastLeader(initFusion.getSocketAddress(), key);
        }
    }

    public void sendChangingLeader(InetSocketAddress newLeader) {
        bufferOut.put((byte) 14);
        var address = newLeader.getAddress().getAddress();
        var type = address.length == 4 ? 4 : 6;
        bufferOut.put((byte) type);
        for (var o : address) {
            bufferOut.put(o);
        }
        bufferOut.putInt(newLeader.getPort());
        updateInterestOps();
    }
}