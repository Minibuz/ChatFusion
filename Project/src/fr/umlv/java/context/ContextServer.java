package fr.umlv.java.context;

import fr.umlv.java.ServerChatFusion;
import fr.umlv.java.models.fusion.InitFusion;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.models.login.User;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.writer.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
    private boolean isMegaServerLeader;

    public ContextServer(ServerChatFusion server, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public boolean isServer() { return isServer; }

    public boolean isMegaServerLeader() {
        return isMegaServerLeader;
    }

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
            logger.info("test" + currentOpCode);
            bufferIn.compact();
            reader = Reader.findReader(currentOpCode);
            if (reader == null) {
                currentOpCode = -1;
                return;
            }
            if (name == null && (currentOpCode >= 8)) {
                isServer = true;
            }
        }

        // Process the byteBuffer in entry
        if (processEntry()) return;

        // If a server is messaging us
        if(isServer) {
            processServer();
            currentOpCode = -1;
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
                        bufferOut.put(new AcceptedLoginWriter(bufferOut.remaining(), server.getServerName()).toByteBuffer());
                        break;
                    }
                    bufferOut.put(
                            new RefusedLoginWriter(bufferOut.remaining())
                            .toByteBuffer()
                    );
                    break;
                case 1: // This shouldn't happen
                    user = (User)reader.get();
                    if (!server.getClients().containsKey(user.login())) {
                        server.getClients().put(user.login(), user.password());
                        this.name = user.login();
                        bufferOut.put(new AcceptedLoginWriter(bufferOut.remaining(), server.getServerName()).toByteBuffer());
                        break;
                    }
                    bufferOut.put(
                            new RefusedLoginWriter(bufferOut.remaining())
                                    .toByteBuffer()
                    );
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
                    server.broadcast(msg, false, msg.getServerName());
                    break;
                case 5:
                    break;
                default:
                    // Error
                    break;
            }
        }
        currentOpCode = -1;
        updateInterestOps();
    }

    private void processServer() {
            switch (currentOpCode) {
                case 4 -> {
                    var msg = (Message) reader.get();
                    server.broadcast(msg, true, name);
                }
                case 8 -> {
                    var initFusion = (InitFusion) reader.get();
                    if (server.isLeader()) {
                        if (initFusion.getMembers().stream().noneMatch(m -> server.getMembers().contains(m))) { // Check names in common
                            bufferOut.put(
                                    new FusionInitOkWriter(bufferOut.remaining(), server.getServerName(), server.getServerSocketChannel().socket(), server.getMembers())
                                            .toByteBuffer());
                            changeLeader(initFusion);
                        } else {
                            bufferOut.put(
                                    new FusionInitKoWriter(bufferOut.remaining())
                                            .toByteBuffer()
                            );
                        }
                    } else {
                        bufferOut.put(
                                new FusionInitForwardWriter(bufferOut.remaining(), server.getServerSocketChannel().socket())
                                        .toByteBuffer()
                        ); // Sending OpCode 11
                    }
                }
                case 9 -> {
                    var initFusion = (InitFusion) reader.get();
                    changeLeader(initFusion);
                    System.out.println("Fusion done");
                }
                case 10 -> logger.info("Failed Fusion");
                case 11 -> {
                    var addressServer = (InetSocketAddress) reader.get();
                    try {
                        server.getFusionSc().close(); // Need to close or double redirection from the initial server
                        server.swapFusion(addressServer);
                    } catch (IOException e) {
                        logger.info("SwapFusion broken");
                    }
                }
                case 12 -> {
                    var addressServer = (InetSocketAddress) reader.get();
                    bufferOut.put((byte)13);
                    if(server.isFusionInDoing()) {
                        bufferOut.put((byte)0);
                        return;
                    } else {
                        bufferOut.put((byte)1);
                    }
                    try {
                        server.swapFusion(addressServer);
                    } catch (IOException e) {
                        logger.info("SwapFusion broken");
                    }
                }
                case 13 -> {
                    var status = (byte) reader.get();
                    if (status == (byte)0) {
                        logger.info("Fusion impossible");
                    }
                    if (status == (byte)1) {
                        logger.info("Fusion initiated");
                    }

                }
                case 14 -> {
                    // Receive change leader
                    var adressServer = (InetSocketAddress) reader.get();
                    logger.info("swap fusion");
                    try {
                        server.swapFusion(adressServer);
                    } catch (IOException e) {
                        logger.info("SwapFusion broken");
                        return;
                    }
                    silentlyClose();
                }
            }
    }

    private boolean processEntry() {
        var status = reader.process(bufferIn);
        if (status == Reader.ProcessStatus.ERROR) {
            currentOpCode = -1;
            return true;
        }
        return status == Reader.ProcessStatus.REFILL;
    }

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

    public void doRead() throws IOException {
        if (sc.read(bufferIn) == -1) {
            logger.warning("Client closed connexion");
            closed = true;
            updateInterestOps();
            return;
        }
        while(bufferIn.position() != 0) {
            processIn();
        }
        updateInterestOps();
    }

    public void doWrite() throws IOException {
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

    public void doConnect() throws IOException {
        if (!sc.finishConnect())
            return; // the selector gave a bad hint
        isServer = true;
        // Fusion init
        bufferOut.put(
                new FusionInitWriter(bufferOut.remaining(), server.getServerName(), server.getServerSocketChannel().socket(), server.getMembers())
                    .toByteBuffer());
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void fillFusionRequest(InetSocketAddress socket) {
        bufferOut.put(
                new FusionRequestWriter(bufferOut.remaining(), socket)
                        .toByteBuffer()
        );
        updateInterestOps();
    }

    public void changeLeader(InitFusion initFusion) {
        this.name = initFusion.getServerName();
        isMegaServerLeader = false;
        if (server.getServerName().compareTo(initFusion.getServerName()) > 0) { // Changing leader depending on the names
            isMegaServerLeader = true;
            server.unsetLeader();
            server.broadcastLeader(initFusion.getSocketAddress(), name);
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