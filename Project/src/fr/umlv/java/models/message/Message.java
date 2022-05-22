package fr.umlv.java.models.message;

public class Message {
    private final String serverName;
    private final String login;
    private final String text;

    public Message(String login, String text) {
        this.serverName = null;
        this.login = login;
        this.text = text;
    }
    public Message(String serverName, String login, String text) {
        this.serverName = serverName;
        this.login = login;
        this.text = text;
    }
    public String getServerName() {
        return serverName;
    }
    public String getLogin() {
        return login;
    }
    public String getText() {
        return text;
    }
}
