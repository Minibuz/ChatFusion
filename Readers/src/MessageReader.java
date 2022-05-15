import java.nio.ByteBuffer;

public class MessageReader implements Reader<Message> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private String login_string;
	private String msg_string;
	private final StringReader login_reader = new StringReader(1);
	private final StringReader msg_reader = new StringReader(1);
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		if (login_string == null) {
			var login_status = login_reader.process(bb);
			if (login_status == ProcessStatus.ERROR) {
				status = ProcessStatus.ERROR;
				return status;
			}
			if (login_status == ProcessStatus.REFILL) {
				status = ProcessStatus.REFILL;
				return status;
			}
			login_string = login_reader.get();
		}
		var msg_status = msg_reader.process(bb);
		if (msg_status == ProcessStatus.ERROR) {
			status = ProcessStatus.ERROR;
			return status;
		}
		if (msg_status == ProcessStatus.REFILL) {
			status = ProcessStatus.REFILL;
			return status;
		}
		msg_string = msg_reader.get();
		status = ProcessStatus.DONE;
		return status;
	}

	@Override
	public Message get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		System.out.println(login_string);
		System.out.println(msg_string);
		return new Message(login_string, msg_string);
	}

	@Override
	public void reset() {
		login_reader.reset();
		msg_reader.reset();
		status = ProcessStatus.REFILL;
		login_string = null;
		msg_string = null;
	}

}
