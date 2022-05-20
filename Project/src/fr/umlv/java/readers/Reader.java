package fr.umlv.java.readers;

import fr.umlv.java.models.OpCode;

import java.nio.ByteBuffer;

public interface Reader<T> {

    public static enum ProcessStatus { DONE, REFILL, ERROR };

    public ProcessStatus process(ByteBuffer bb);

    public T get();

    public void reset();

    static Reader<?> findGoodReader(int value) {
        Reader<?> reader = switch (OpCode.getOpCode(value)) {
            case LOGIN_ANONYMOUS -> null;
            case LOGIN_PASSWORD -> null;
            case LOGIN_ACCEPTED -> null;
            case LOGIN_REFUSED -> null;
            case MESSAGE -> null;
            case MESSAGE_PRIVATE -> null;
            case FILE_PRIVATE -> null;
            case FUSION_INIT -> null;
            case FUSION_INIT_OK -> null;
            case FUSION_INIT_KO -> null;
            case FUSION_INIT_FWD -> null;
            case FUSION_REQUEST -> null;
            case FUSION_REQUEST_RESP -> null;
            case FUSION_CHANGE_LEADER -> null;
            case FUSION_MERGE -> null;
        };
        return null;
    }
}
