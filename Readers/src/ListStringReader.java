import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ListStringReader implements Reader<List<String>> {
	private ProcessStatus status = ProcessStatus.REFILL;
	private final int nbStrings;
	private final List<String> list = new ArrayList<>();
	private final StringReader reader = new StringReader();

	public ListStringReader(int nbStrings) {
		this.nbStrings = nbStrings;
	}
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
		while (list.size() != nbStrings) {
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
	public List<String> get() {
		if (status != ProcessStatus.DONE) {
			throw new IllegalStateException("Not right process status.");
		}
		return list;
	}

	@Override
	public void reset() {
		reader.reset();
		status = ProcessStatus.REFILL;
		list.clear();
	}

}
