package fr.umlv.java.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class IpAddressReader implements Reader<String> {
	private String address;
	private ProcessStatus status = ProcessStatus.REFILL;
	private final ByteBuffer size_buffer = ByteBuffer.allocate(Byte.BYTES);
	private ByteBuffer address_buffer;
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
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
	    if (address_buffer == null) {
	    	size_buffer.flip();
	    	var type = size_buffer.get();
	    	if (type != 4 && type != 6) {
	    		status = ProcessStatus.ERROR;
	    		return status;
	    	}
			var size = type == 4 ? 4 : 16;
	    	address_buffer = ByteBuffer.allocate(size*Byte.BYTES);
	    }
	    fillBuffer(bb, address_buffer);
	    if (address_buffer.hasRemaining()) {
	    	return ProcessStatus.REFILL;
	    }
	    status = ProcessStatus.DONE;
	    address = decodeAddress();
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

	private String decodeAddress() {
		address_buffer.flip();
		var number = address_buffer.remaining();
		var sb = new StringBuilder();
		for (var i = 0; i<number; i++) {
			sb.append(address_buffer.get());
			if (i!=number-1) {
				sb.append(".");
			}
		}
		return sb.toString();
	}

	@Override
	public String get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return address;
	}

	@Override
	public void reset() {
		status = ProcessStatus.REFILL;
		size_buffer.clear();
		address_buffer = null;
	}

}
