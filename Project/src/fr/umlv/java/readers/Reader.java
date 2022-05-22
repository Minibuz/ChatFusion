package fr.umlv.java.readers;

import fr.umlv.java.models.OpCode;
import fr.umlv.java.readers.fusion.FusionInitReader;
import fr.umlv.java.readers.login.LoginAcceptedReader;
import fr.umlv.java.readers.login.LoginAnonymousReader;
import fr.umlv.java.readers.message.MessageReader;
import fr.umlv.java.readers.message.PrivateMessageReader;

import java.nio.ByteBuffer;

public interface Reader<T> {

    public static enum ProcessStatus { DONE, REFILL, ERROR };

    public ProcessStatus process(ByteBuffer bb);

    public T get();

    public void reset();

    static Reader<?> findReader(int value) {
        return switch (OpCode.getOpCode(value)) {
            case LOGIN_ANONYMOUS -> new LoginAnonymousReader();
            case LOGIN_PASSWORD -> throw new IllegalArgumentException("Not supported"); // Even if kinda exist
            case LOGIN_ACCEPTED -> new LoginAcceptedReader();
            case LOGIN_REFUSED, FUSION_INIT_KO -> null;
            case MESSAGE -> new MessageReader();
            case MESSAGE_PRIVATE -> new PrivateMessageReader();
            case FILE_PRIVATE -> null; // TODO
            case FUSION_INIT, FUSION_INIT_OK -> new FusionInitReader();
            case FUSION_INIT_FWD, FUSION_REQUEST, FUSION_CHANGE_LEADER -> new SocketReader();
            case FUSION_REQUEST_RESP -> null; // TODO
            case FUSION_MERGE -> new StringReader();
        };
    }
}
