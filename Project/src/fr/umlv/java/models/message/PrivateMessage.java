package fr.umlv.java.models.message;

public class PrivateMessage {
    private final String serverSrc;
    private final String serverDst;
    private final String loginSrc;
    private final String loginDst;
    private final String text;

    public PrivateMessage(String loginSrc, String serverDst, String loginDst, String text) {
        this.serverSrc = null;
        this.serverDst = serverDst;
        this.loginSrc = loginSrc;
        this.loginDst = loginDst;
        this.text = text;
    }

    public PrivateMessage(String serverSrc, String loginSrc, String serverDst, String loginDst, String text) {
        this.serverSrc = serverSrc;
        this.serverDst = serverDst;
        this.loginSrc = loginSrc;
        this.loginDst = loginDst;
        this.text = text;
    }

    public String getText() {
        return text;
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
