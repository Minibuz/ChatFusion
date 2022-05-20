package fr.umlv.java.readers.login;

import fr.umlv.java.models.login.User;
import fr.umlv.java.readers.Reader;
import fr.umlv.java.readers.StringReader;

import java.nio.ByteBuffer;

public class LoginAnonymousReader implements Reader<User> {

    private String login;
    private ProcessStatus status = ProcessStatus.REFILL;
    private final StringReader reader = new StringReader();
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
        status = ProcessStatus.DONE;
        return status;
    }

    @Override
    public User get() {
        if (status != ProcessStatus.DONE) {
            throw new IllegalStateException("Not right process status.");
        }
        return new User(login);
    }

    @Override
    public void reset() {
        status = ProcessStatus.REFILL;
        reader.reset();
        login = null;
    }
}
