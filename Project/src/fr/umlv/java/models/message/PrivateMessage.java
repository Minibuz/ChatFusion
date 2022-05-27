package fr.umlv.java.models.message;

public class PrivateMessage {
    private final String serverSrc;
    private final String serverDst;
    private final String loginSrc;
    private final String loginDst;
    private final String message;

    public PrivateMessage(String loginSrc, String serverDst, String loginDst, String message) {
        this.serverSrc = null;
        this.serverDst = serverDst;
        this.loginSrc = loginSrc;
        this.loginDst = loginDst;
        this.message = message;
    }

    public PrivateMessage(String serverSrc, String loginSrc, String serverDst, String loginDst, String message) {
        this.serverSrc = serverSrc;
        this.serverDst = serverDst;
        this.loginSrc = loginSrc;
        this.loginDst = loginDst;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getServerSrc() {
        return serverSrc;
    }

    public String getServerDst() {
        return serverDst;
    }

    public String getLoginSrc() {
        return loginSrc;
    }

    public String getLoginDst() {
        return loginDst;
    }
}
