package fr.umlv.java.models;

import java.util.Arrays;
import java.util.Optional;

public enum OpCode {
    LOGIN_ANONYMOUS((byte) 0),
    LOGIN_PASSWORD((byte) 1),
    LOGIN_ACCEPTED((byte) 2),
    LOGIN_REFUSED((byte) 3),
    MESSAGE((byte) 4),
    MESSAGE_PRIVATE((byte) 5),
    FILE_PRIVATE((byte) 6),
    FUSION_INIT((byte) 8),
    FUSION_INIT_OK((byte) 9),
    FUSION_INIT_KO((byte) 10),
    FUSION_INIT_FWD((byte) 11),
    FUSION_REQUEST((byte) 12),
    FUSION_ANSWER((byte) 13),
    FUSION_CHANGE_LEADER((byte) 14),
    FUSION_MERGE((byte) 15);

    private final byte value;

    OpCode(final byte newValue) {
        value = newValue;
    }

    public byte getValue() {
        return value;
    }

    public static Optional<OpCode> getOpCode(byte b) {
        return Arrays.stream(OpCode.values()).filter(opCode -> opCode.getValue() == b).findFirst();
    }
}