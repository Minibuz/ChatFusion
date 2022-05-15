import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {
	private int nbStrings;
	private String msg;
	private ProcessStatus status = ProcessStatus.REFILL;
	private final ByteBuffer size_buffer = ByteBuffer.allocate(Integer.BYTES);
	private ByteBuffer msg_buffer;
	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public StringReader(int nbStrings) {
		this.nbStrings = nbStrings;
	}
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
        fillBuffer(bb, size_buffer);
	    if (size_buffer.hasRemaining()) {
		    status = ProcessStatus.REFILL;
			return status;
	    }
	    if (msg_buffer == null) {
	    	size_buffer.flip();
	    	var size = size_buffer.getInt();
	    	if (size < 0 || size > 1024) {
	    		status = ProcessStatus.ERROR;
	    		return status;
	    	}
	    	msg_buffer = ByteBuffer.allocate(size*Byte.BYTES);
	    }
	    fillBuffer(bb, msg_buffer);
	    if (msg_buffer.hasRemaining()) {
	    	return ProcessStatus.REFILL;
	    }
	    status = ProcessStatus.DONE;
	    msg_buffer.flip();
	    msg = UTF8.decode(msg_buffer).toString();
	    return status;
	}
	
	private void fillBuffer(ByteBuffer buffer, ByteBuffer toFill) {
		buffer.flip();
		try {
	        if (buffer.remaining() <= toFill.remaining()) {
	            toFill.put(buffer);
	        } else {
	        	var oldLimit = buffer.limit();
	            buffer.limit(toFill.remaining());
	            toFill.put(buffer);
	            buffer.limit(oldLimit);
	        }
		} finally {
			buffer.compact();
		}
	}

	@Override
	public String get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return msg;
	}

	@Override
	public void reset() {
		status = ProcessStatus.REFILL;
		size_buffer.clear();
		msg_buffer = null;
	}

}
