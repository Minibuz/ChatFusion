package fr.umlv.java.readers.fusion;

import fr.umlv.java.models.fusion.InitFusion;
import fr.umlv.java.readers.IntReader;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.readers.SocketReader;
import fr.umlv.java.readers.StringReader;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FusionInitReader implements Reader<InitFusion> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private int nbMembers = -1;
	private String name;
	private SocketAddress address;
	private final List<String> members = new ArrayList<>();
	private final StringReader nameReader = new StringReader();
	private final SocketReader socketReader = new SocketReader();
	private final IntReader intReader = new IntReader();
	private final StringReader memberReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		if (name == null) {
			var readerStatus = nameReader.process(bb);
			if (readerStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (readerStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			name = nameReader.get();
		}
		if (address == null) {
			var readerStatus = socketReader.process(bb);
			if (readerStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (readerStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			address = socketReader.get();
		}
		if (nbMembers == -1) {
			var readerStatus = intReader.process(bb);
			if (readerStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (readerStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			nbMembers = intReader.get();
		}
		while (members.size() != nbMembers) {
			var readerStatus = memberReader.process(bb);
			if (readerStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (readerStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			members.add(memberReader.get());
			memberReader.reset();
		}
		status = ProcessStatus.DONE;
		return status;
	}

	@Override
	public InitFusion get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return new InitFusion(name, address, members);
	}

	@Override
	public void reset() {
		nameReader.reset();
		socketReader.reset();
		intReader.reset();
		memberReader.reset();
		members.clear();
		nbMembers = -1;
		status = ProcessStatus.REFILL;
	}

}
