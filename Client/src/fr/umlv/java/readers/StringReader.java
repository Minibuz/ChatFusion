package fr.umlv.java.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private enum State {
        DONE, WAITING, ERROR
    };
    private final Charset UTF_8 = StandardCharsets.UTF_8;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(1024); // write-mode
    private State state = State.WAITING;
    private int size = 0;
    private String value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        System.out.println(bb);
        bb.flip();
        try {
            if (bb.remaining() <= internalBuffer.remaining()) {
                internalBuffer.put(bb);
            } else {
                var oldLimit = bb.limit();
                bb.limit(internalBuffer.remaining());
                internalBuffer.put(bb);
                bb.limit(oldLimit);
            }
        } finally {
            bb.compact();
        }

        internalBuffer.flip();
        if(size == 0) {
            if(internalBuffer.remaining() < Integer.BYTES) {
                internalBuffer.compact();
                return ProcessStatus.REFILL;
            }
            size = internalBuffer.getInt();
            if(size <= 0 || size > 1024) {
                return ProcessStatus.ERROR;
            }
        }
        if(internalBuffer.remaining() >= size) {
            state = State.DONE;

            var oldLimit = internalBuffer.limit();
            internalBuffer.limit(internalBuffer.position() + size);
            value = UTF_8.decode(internalBuffer).toString();
            internalBuffer.limit(oldLimit);
            bb.put(internalBuffer);

            internalBuffer.compact();
            return ProcessStatus.DONE;
        }

        internalBuffer.compact();
        return ProcessStatus.REFILL;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        size = 0;
        internalBuffer.clear();
    }
}
