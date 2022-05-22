package fr.umlv.java.readers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SocketReader implements Reader<InetSocketAddress> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private InetAddress address;
	private final IpAddressReader ipReader = new IpAddressReader();
	private final IntReader intReader = new IntReader();
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		if (address == null) {
			var ipReaderStatus = ipReader.process(bb);
			if (ipReaderStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (ipReaderStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			address = ipReader.get();
		}
		var intReaderStatus = intReader.process(bb);
		if (intReaderStatus == ProcessStatus.ERROR) {
			status = ProcessStatus.ERROR;
			return status;
		}
		if (intReaderStatus == ProcessStatus.REFILL) {
			status = ProcessStatus.REFILL;
			return status;
		}
		status = ProcessStatus.DONE;
		return status;
	}

	@Override
	public InetSocketAddress get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return new InetSocketAddress(address, intReader.get());
	}

	@Override
	public void reset() {
		address = null;
		ipReader.reset();
		intReader.reset();
		status = ProcessStatus.REFILL;
	}

}
