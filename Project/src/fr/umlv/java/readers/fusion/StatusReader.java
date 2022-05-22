package fr.umlv.java.readers.fusion;

import fr.umlv.java.readers.IntReader;
import fr.umlv.java.readers.Reader;

import java.nio.ByteBuffer;

public class StatusReader implements Reader<Boolean> {

    private boolean statusFusion;
    private ProcessStatus status = ProcessStatus.REFILL;
    private final IntReader reader = new IntReader();

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
        statusFusion = reader.get() != 0;
        status = ProcessStatus.DONE;
        return status;
    }

    public Boolean get() {
        if (status != ProcessStatus.DONE) {
            throw new IllegalStateException();
        }
        return statusFusion;
    }

    @Override
    public void reset() {
        status = ProcessStatus.REFILL;
        reader.reset();
        statusFusion = false;
    }
}
