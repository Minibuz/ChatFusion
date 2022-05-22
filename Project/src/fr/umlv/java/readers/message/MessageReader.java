package fr.umlv.java.readers.message;

import fr.umlv.java.models.message.Message;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.readers.StringReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MessageReader implements Reader<Message> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private final StringReader reader = new StringReader();
	private final List<String> list = new ArrayList<>();
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		while (list.size() != 3) {
			var readerStatus = reader.process(bb);
			if (readerStatus == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (readerStatus == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			list.add(reader.get());
			reader.reset();
		}
		status = ProcessStatus.DONE;
		return status;
	}

	@Override
	public Message get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return new Message(list.get(0), list.get(1), list.get(2));
	}

	@Override
	public void reset() {
		reader.reset();
		status = ProcessStatus.REFILL;
		list.clear();
	}

}
