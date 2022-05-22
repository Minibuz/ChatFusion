package fr.umlv.java;

import fr.umlv.java.models.context.ContextClient;
import fr.umlv.java.models.Commands;
import fr.umlv.java.models.message.Message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;

public class Client {
    static private final Logger logger = Logger.getLogger(Client.class.getName());

    static private final Charset UTF_8 = StandardCharsets.UTF_8;

    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String login;
    private final String password;
    private final String folder;
    private final Thread console;

    private Commands commands;
    private ContextClient uniqueContextClient;

    // Not used since we don't deal with the password for user
    public Client(String address, int port, String folder, String login, String password) throws IOException {
        this.serverAddress = new InetSocketAddress(address, port);
        this.login = login;
        this.folder = folder;
        this.password = password;
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }
    public Client(String address, int port, String folder, String login) throws IOException {
        this.login = login;
        this.folder = folder;
        this.password = null;

        this.serverAddress = new InetSocketAddress(address, port);
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }

    private void consoleRun() {
        try {
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    var msg = scanner.nextLine();
                    var msgBuffer = UTF_8.encode(msg).remaining();
                    if(msgBuffer > 1024) {
                        logger.info("Non non non, c'est pas bien");
                        continue;
                    }
                    sendCommand(msg);
                }
            }
            logger.info("Console thread stopping");
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        }
    }

    /**
     * Send instructions to the selector via a BlockingQueue and wake it up
     *
     * @param msg
     * @throws InterruptedException
     */
    private void sendCommand(String msg) throws InterruptedException {
        commands.transferMessage(new Message(login, msg));
        selector.wakeup();
    }

    /**
     * Processes the command from the BlockingQueue
     */
    private void processCommands() {
        commands.retrieveCommand();
    }

    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContextClient = new ContextClient(key, this.password != null, login);
        key.attach(uniqueContextClient);
        commands = new Commands(uniqueContextClient);
        sc.connect(serverAddress);

        console.start();

        while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContextClient.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContextClient.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContextClient.doRead();
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            usage();
            return;
        }
        new Client(args[0], Integer.parseInt(args[1]), args[2], args[3]).launch();
    }

    private static void usage() {
        System.out.println("Usage : Client serverIP serverPort folder login");
    }
}
