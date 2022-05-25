package fr.umlv.java.models;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BufferMessage {

    private static final int BUFFER_SIZE = 10000;

    private final Byte opcode;
//    private int loginSize;
//    private ByteBuffer login;
//    private int passwordSize;
//    private ByteBuffer password;
//    private int serverNameSize;
//    private ByteBuffer serverName;
//    private int sizeMsg;
//    private ByteBuffer msg;
//    private int serverSrcSize;
//    private ByteBuffer serverSrc;
//    private int loginSrcSize;
//    private ByteBuffer loginSrc;
//    private int serverDstSize;
//    private ByteBuffer serverDst;
//    private int loginDstSize;
//    private ByteBuffer loginDst;
//    private int filenameSize;
//    private ByteBuffer filename;
//    private int nbBlocks;
//    private int blockSize;
//    private Byte[] block;
//    private int nameSize;
//    private ByteBuffer name;
//    private ByteBuffer socketAddress;
//    private int nbMembers;
//    private int[] nameMemberSize;
//    private ByteBuffer[] nameMember;
//    private ByteBuffer leaderAddress;
//    private Byte status;
    private ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);

    private BufferMessage(BufferMessageBuilder builder) {
        this.opcode = builder.opcode;
        switch (opcode) {
            case 0 -> anonymousLogin(builder);
            case 1 -> passwordLogin(builder);
            case 2 -> acceptedLogin(builder);
            case 3 -> refusedLogin(builder);
            case 4 -> message(builder);
            case 5 -> privateMessage(builder);
            case 6 -> privateFile(builder);
            case 8 -> fusionInit(builder);
            case 9 -> fusionInitOk(builder);
            case 10 -> fusionInitKO(builder);
            case 11 -> fusionInitFW(builder);
            case 12 -> fusionRequest(builder);
            case 13 -> fusionRequestResp(builder);
            case 14 -> fusionChangeLeader(builder);
            case 15 -> fusionMerge(builder);
            default -> throw new IllegalArgumentException("Unexpected value: " + opcode);
        }
    }

    private void anonymousLogin(BufferMessageBuilder builder) {
        var login = UTF_8.encode(builder.login);
        if(bufferOut.remaining() >= Integer.BYTES + Integer.BYTES + login.remaining()) {
            throw new IllegalStateException();
        }
        if(login.remaining() > 30) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(opcode).putInt(login.remaining()).put(login);
    }

    private void passwordLogin(BufferMessageBuilder builder) {
        var login = UTF_8.encode(builder.login);
        var password = UTF_8.encode(builder.password);
        if(bufferOut.remaining() >= Integer.BYTES + Integer.BYTES + login.remaining() + Integer.BYTES + password.remaining()) {
            throw new IllegalStateException();
        }
        if(login.remaining() > 30 || password.remaining() > 30) {
            throw new IllegalArgumentException();
        }

        bufferOut.put(opcode).putInt(login.remaining()).put(login).putInt(password.remaining()).put(password);
    }

    private void acceptedLogin(BufferMessageBuilder builder) {
    }

    public ByteBuffer toByteBuffer() {
        return bufferOut;
    }

    public static class BufferMessageBuilder {
        private final Byte opcode;
        private String login;
        private String password;
        private String serverName;
        private String msg;
        private String serverSrc;
        private String loginSrc;
        private String serverDst;
        private String loginDst;
        private String filename;
        private int nbBlocks;
        private int blockSize;
        private Byte[] block;
        private String name;
        private InetSocketAddress socketAddress;
        private int nbMembers;
        private String[] nameMember;
        private String leaderAddress;
        private Byte status;

        public BufferMessageBuilder(Byte opcode) {
            this.opcode = opcode;
        }

        public BufferMessageBuilder setLogin(String login) {
            this.login = login;
            return this;
        }
        public BufferMessageBuilder setPassword(String password) {
            this.password = password;
            return this;
        }
        public BufferMessageBuilder setServerName(String serverName) {
            this.serverName = serverName;
            return this;
        }
        public BufferMessageBuilder setMsg(String msg) {
            this.msg = msg;
            return this;
        }
        public BufferMessageBuilder setServerSrc(String serverSrc) {
            this.serverSrc = serverSrc;
            return this;
        }
        public BufferMessageBuilder setLoginSrc(String loginSrc) {
            this.loginSrc = loginSrc;
            return this;
        }
        public BufferMessageBuilder setServerDst(String serverDst) {
            this.serverDst = serverDst;
            return this;
        }
        public BufferMessageBuilder setLoginDst(String loginDst) {
            this.loginDst = loginDst;
            return this;
        }
        public BufferMessageBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }
        public BufferMessageBuilder setNbBlocks(int nbBlocks) {
            this.nbBlocks = nbBlocks;
            return this;
        }
        public BufferMessageBuilder setBlockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }
        public BufferMessageBuilder setBlock(Byte[] block) {
            this.block = block;
            return this;
        }
        public BufferMessageBuilder setName(String name) {
            this.name = name;
            return this;
        }
        public BufferMessageBuilder setSocketAddress(InetSocketAddress socketAddress) {
            this.socketAddress = socketAddress;
            return this;
        }
        public BufferMessageBuilder setNbMembers(int nbMembers) {
            this.nbMembers = nbMembers;
            return this;
        }
        public BufferMessageBuilder setNameMember(String[] nameMember) {
            this.nameMember = nameMember;
            return this;
        }
        public BufferMessageBuilder setLeaderAddress(String leaderAddress) {
            this.leaderAddress = leaderAddress;
            return this;
        }
        public BufferMessageBuilder setStatus(Byte status) {
            this.status = status;
            return this;
        }

        public BufferMessage build() {
            return new BufferMessage(this);
        }
    }
}