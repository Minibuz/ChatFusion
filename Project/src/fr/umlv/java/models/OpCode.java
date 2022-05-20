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
            case 0 -> LOGIN_ANONYMOUS;
            case 1 -> LOGIN_PASSWORD;
            case 2 -> LOGIN_ACCEPTED;
            case 3 -> LOGIN_REFUSED;
            case 4 -> MESSAGE;
            case 5 -> MESSAGE_PRIVATE;
            case 6 -> FILE_PRIVATE;
            case 8 -> FUSION_INIT;
            case 9 -> FUSION_INIT_OK;
            case 10 -> FUSION_INIT_KO;
            case 11 -> FUSION_INIT_FWD;
            case 12 -> FUSION_REQUEST;
            case 13 -> FUSION_REQUEST_RESP;
            case 14 -> FUSION_CHANGE_LEADER;
            case 15 -> FUSION_MERGE;
            default -> throw new IllegalArgumentException("Unexpected value: " + opCodeValue);
        };
    }
}
