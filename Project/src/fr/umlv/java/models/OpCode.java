package fr.umlv.java.models;

public enum OpCode {
    LOGIN_ANONYMOUS(0),
    LOGIN_PASSWORD(1),
    LOGIN_ACCEPTED(2),
    LOGIN_REFUSED(3),
    MESSAGE(4),
    MESSAGE_PRIVATE(5),
    FILE_PRIVATE(6),
    FUSION_INIT(8),
    FUSION_INIT_OK(9),
    FUSION_INIT_KO(10),
    FUSION_INIT_FWD(11),
    FUSION_REQUEST(12),
    FUSION_REQUEST_RESP(13),
    FUSION_CHANGE_LEADER(14),
    FUSION_MERGE(15);

    OpCode(int i) {
    }

    public static OpCode getOpCode(int opCodeValue) {
        return switch (opCodeValue) {
            case 0 -> LOGIN_ANONYMOUS; // Done : LoginAnonymousReader
            case 1 -> LOGIN_PASSWORD; // Done : LoginPasswordReader
            case 2 -> LOGIN_ACCEPTED; // Done : LoginAcceptedReader
            case 3 -> LOGIN_REFUSED; // No reader
            case 4 -> MESSAGE; // Done : MessageReader
            case 5 -> MESSAGE_PRIVATE; // Done : PrivateMessageReader
            case 6 -> FILE_PRIVATE; // TODO
            case 8 -> FUSION_INIT; // Done : FusionInitReader
            case 9 -> FUSION_INIT_OK; // Done : FusionInitReader
            case 10 -> FUSION_INIT_KO; // No reader
            case 11 -> FUSION_INIT_FWD; // Done : SocketReader
            case 12 -> FUSION_REQUEST; // Done : SocketReader
            case 13 -> FUSION_REQUEST_RESP; // Done : StatusReader
            case 14 -> FUSION_CHANGE_LEADER; // Done : SocketReader
            case 15 -> FUSION_MERGE; // Done : StringReader
            default -> throw new IllegalArgumentException("Unexpected value: " + opCodeValue);
        };
    }
}
