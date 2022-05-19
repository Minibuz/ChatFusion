import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerChatFusion {
	static private class Context {
		private final SelectionKey key;
		private final SocketChannel sc;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final ArrayDeque<Message> queue = new ArrayDeque<>();
		private final ServerChatFusion server;
		private boolean closed = false;
		private String name = null;
		private Reader reader;
		private byte currentOpCode = -1;
		
		private Context(ServerChatFusion server, SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
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
				createReader();
				if (currentOpCode == -1) {
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
			if ((name != null && (currentOpCode != 0 || currentOpCode != 1)) || (name == null && (currentOpCode == 0 || currentOpCode == 1))) { // On s'assure que l'utilisateur utilise la bonne commande
				switch(currentOpCode) {
					case 0: var name = ((List<String>) reader.get()).get(0);
					if (!server.clients.containsKey(name)) {
						server.clients.put(name, null);
						this.name = name;
						fillValidConnexion();
						break;
					} bufferOut.put((byte) 3); break;
					case 1: var strings = (List<String>) reader.get(); name = strings.get(0); var password = strings.get(1);
					if (!server.clients.containsKey(name)) {
						server.clients.put(name, password);
						this.name = name;
						fillValidConnexion();
						break;
					} bufferOut.put((byte) 3); break;
					case 4: strings = (List<String>) reader.get(); server.broadcast(new Message(strings.get(1), strings.get(2))); break;
					case 5: break;
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
			var servername_buffer = UTF8.encode(server.serverName);
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
				server.clients.remove(name);
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
		private void doRead() throws IOException {
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

		private void doWrite() throws IOException {
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

		private void createReader() {
			switch (currentOpCode) {
				case 0 -> reader = new ListStringReader(1);
				case 1 -> reader = new ListStringReader(2);
				case 4 -> reader = new ListStringReader(3);
				case 5 -> reader = new ListStringReader(5);
				default -> currentOpCode = -1;
			}
		}

		private void fillValidConnexion() {
			bufferOut.put((byte) 2);
			var serverBuffer = UTF8.encode(server.serverName);
			bufferOut.putInt(serverBuffer.remaining());
			bufferOut.put(serverBuffer);
		}
	}

	private static final int BUFFER_SIZE = 10000;
	private static final Logger logger = Logger.getLogger(ServerChatFusion.class.getName());
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final String serverName;
	private final ArrayBlockingQueue<String> queue;
	private final Thread console;
	private final Map<String, String> clients = new HashMap<>();

	public ServerChatFusion(String name, int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		serverName = name;
		selector = Selector.open();
		this.queue = new ArrayBlockingQueue<String>(10);
		this.console = new Thread(this::consoleRun);
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
    
    /**
     * Send instructions to the selector via a BlockingQueue and wake it up
     *
     * @param msg
     * @throws InterruptedException
     */

    private void sendCommand(String msg) throws InterruptedException {
        queue.add(msg);
        selector.wakeup();
    }

    /**
     * Processes the command from the BlockingQueue 
     * @throws IOException 
     * @throws InterruptedException 
     */

    private void processCommands() throws IOException {
        var command = queue.poll();
        if (command != null) {
        	switch(command) {
        		// PUT commands here
        	}
        }
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
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
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
		sk.attach(new Context(this, sk));
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			clients.remove(((Context) key.attachment()).name);
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	private void broadcast(Message msg) {
		for (var key : selector.keys()) {
			if (key.attachment() != null) {
				((Context) key.attachment()).queueMessage(msg);
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
		System.out.println("Usage : ServerChatFusion name port");
	}
}

/** Récapitulatif des types donnés pour les différentes requêtes
 *
 *  string
*	string string
*	string
*	string string string
*	string string string string
*
*	string * 5 int int bytes
*	string adresse int string ...
*	adresse
*	byte
 *
*	StringReader(int nbStrings) --> pour lire nbStrings String
**/
