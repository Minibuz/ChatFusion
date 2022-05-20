package fr.umlv.java.readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SocketReader implements Reader<InetSocketAddress> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private InetSocketAddress socket;
	private final IpAddressReader reader = new IpAddressReader();
	private final IntReader intReader = new IntReader();
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		var readerStatus = reader.process(bb);
		if (readerStatus == ProcessStatus.ERROR) {
			status = ProcessStatus.ERROR;
			return status;
		}
		if (readerStatus == ProcessStatus.REFILL) {
			status = ProcessStatus.REFILL;
			return status;
		}
		var intReaderStatus = reader.process(bb);
		if (intReaderStatus == ProcessStatus.ERROR) {
			status = ProcessStatus.ERROR;
			return status;
		}
		if (intReaderStatus == ProcessStatus.REFILL) {
			status = ProcessStatus.REFILL;
			return status;
		}
		socket = new InetSocketAddress(reader.get(), intReader.get());
		status = ProcessStatus.DONE;
		return status;
	}

	@Override
	public InetSocketAddress get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return socket;
	}

	@Override
	public void reset() {
		reader.reset();
		intReader.reset();
		status = ProcessStatus.REFILL;
	}

}
