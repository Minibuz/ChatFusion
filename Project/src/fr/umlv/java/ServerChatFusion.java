package fr.umlv.java;

import fr.umlv.java.context.ContextServer;
import fr.umlv.java.models.message.Message;
import fr.umlv.java.utils.Helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerChatFusion {
	private static final Logger logger = Logger.getLogger(ServerChatFusion.class.getName());
	
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final String serverName;
	private final ArrayBlockingQueue<String> queue;
	private final Thread console;
	private final Map<String, String> clients = new HashMap<>();
	private SocketChannel fusionSc = SocketChannel.open(); // TODO megaserver key to permit forward
	private boolean isLeader = true;
	private boolean fusionInDoing = false;
	private final List<String> members = new ArrayList<>();

	public ServerChatFusion(String name, int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		var address = new InetSocketAddress(port);
		serverSocketChannel.bind(address);
		serverName = name;
		selector = Selector.open();
		this.queue = new ArrayBlockingQueue<>(10);
		this.console = new Thread(this::consoleRun);
	}

	public String getServerName() {
		return serverName;
	}

	public Map<String, String> getClients() {
		return clients;
	}

	public ServerSocketChannel getServerSocketChannel() { return serverSocketChannel; }

	public List<String> getMembers() { return members; }

	public boolean isLeader() { return isLeader; }

	public void unsetLeader() {
		isLeader = false;
		// Close all connections used as leader
	}

	public SocketChannel getFusionSc() { return fusionSc; }

	public boolean isFusionInDoing() {
		return fusionInDoing;
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		console.start();
		while (!Thread.interrupted()) {
			Helpers.printKeys(selector); // for debug
			System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}
	
    private void consoleRun() {
        try {
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    var msg = scanner.nextLine();
                    sendCommand(msg);
                }
            }
            logger.info("Console thread stopping");
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        }
    }

    private void sendCommand(String msg) throws InterruptedException {
        queue.add(msg);
        selector.wakeup();
    }

    private void processCommands() throws IOException {
        var command = queue.poll();
        if (command != null) {
			var strings = command.split(" ");
        	switch(strings[0]) {
        		case "FUSION" -> {
					if (isLeader) {
						swapFusion(new InetSocketAddress(strings[1], Integer.parseInt(strings[2])));
					} else {
						// Fusion request
						var context = (ContextServer) fusionSc.keyFor(selector).attachment();
						context.fillFusionRequest(new InetSocketAddress(strings[1], Integer.parseInt(strings[2])));
					}
				}
        	}
        }
    }

	public void swapFusion(InetSocketAddress address) throws IOException {
		fusionSc = SocketChannel.open();
		fusionSc.configureBlocking(false);
		var sk = fusionSc.register(selector, SelectionKey.OP_CONNECT);
		sk.attach(new ContextServer(this, sk));
		fusionSc.connect(address);
	}

	private void treatKey(SelectionKey key) {
		Helpers.printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isConnectable()) {
				((ContextServer) key.attachment()).doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				((ContextServer) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ContextServer) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
			silentlyClose(key);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		var ssc = (ServerSocketChannel) key.channel();
		var sc = ssc.accept();
		if (sc == null) {
			logger.warning("Selector lied to me!");
			return;
		}
		sc.configureBlocking(false);
		var sk = sc.register(selector, SelectionKey.OP_READ);
		sk.attach(new ContextServer(this, sk));
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = key.channel();
		try {
			clients.remove(((ContextServer) key.attachment()).getName());
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public void broadcast(Message msg, boolean fromLeaderServer, String server) {
		for (var key : selector.keys()) {
			var context = (ContextServer) key.attachment();
			if (key.attachment() != null) {
				if(!context.isServer()) {
					context.queueMessage(msg);
					continue;
				}
				if(isLeader && !server.equals(context.getName())) {
					context.queueMessage(msg);
					continue;
				}
				if(!isLeader && !fromLeaderServer && context.isMegaServerLeader()) {
					context.queueMessage(msg);
				}
			}
		}
	}

	public void broadcastLeader(InetSocketAddress newLeader, String server) {
		for (var key : selector.keys()) {
			var context = (ContextServer) key.attachment();
			if (key.attachment() != null) {
				if(context.isServer() && !server.equals(context.getName())) {
					((ContextServer) key.attachment()).sendChangingLeader(newLeader);
				}
			}
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		if (args[0].length() > 96) {
			throw new IllegalArgumentException("Name length > 96");
		}
		var port = Integer.parseInt(args[1]);
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Port number is not between 0 and 65535");
		}
		new ServerChatFusion(args[0], port).launch();
	}

	private static void usage() {
		System.out.println("Usage : fr.umlv.java.ServerChatFusion name port");
	}
}
