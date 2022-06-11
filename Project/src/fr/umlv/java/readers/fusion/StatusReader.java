package fr.umlv.java.readers.fusion;

import fr.umlv.java.readers.IntReader;
import fr.umlv.java.readers.Reader;

import java.nio.ByteBuffer;

public class StatusReader implements Reader<Byte> {

    private byte statusFusion = -1;
    private ProcessStatus status = ProcessStatus.REFILL;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (status == ProcessStatus.DONE || status == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }
        bb.flip();
        try {
            if (bb.remaining() >= 1) {
                statusFusion = bb.get();
                status = ProcessStatus.DONE;
            }
        } finally {
            bb.compact();
        }
        return status;
    }

    public Byte get() {
        if (status != ProcessStatus.DONE) {
            throw new IllegalStateException();
        }
        return statusFusion;
    }

    @Override
    public void reset() {
        status = ProcessStatus.REFILL;
        statusFusion = -1;
    }
}
