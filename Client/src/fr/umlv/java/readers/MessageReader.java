package fr.umlv.java.readers;

import fr.umlv.java.models.Message;

import java.nio.ByteBuffer;

public class MessageReader implements Reader<Message> {

    private enum State {
        DONE, WAITING, ERROR
    };
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(1024); // write-mode
    private State state = State.WAITING;
    private Message message = null;
    private IntReader intReader = new IntReader();
    private StringReader stringReader = new StringReader();

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(message == null) {
            message = new Message();
        }

        if(message.getLogin() == null) {
            var statut = stringReader.process(bb);
            switch (statut) {
                case DONE:
                    message.setLogin(stringReader.get());
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    return ProcessStatus.ERROR;
            }
            stringReader.reset();
        }

        if(message.getTexte() == null) {
            var statut = stringReader.process(bb);
            switch (statut) {
                case DONE:
                    message.setTexte(stringReader.get());
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    return ProcessStatus.ERROR;
            }
            stringReader.reset();
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public Message get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return message;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        message = null;
        internalBuffer.clear();
    }
}
