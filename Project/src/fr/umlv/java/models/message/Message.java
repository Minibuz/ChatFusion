package fr.umlv.java.models.message;

public class Message {
    private String serverName;
    private final String login;
    private final String message;

    public Message(String login, String message) {
        this.login = login;
        this.message = message;
    }
    public Message(String serverName, String login, String message) {
        this.serverName = serverName;
        this.login = login;
        this.message = message;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    public String getServerName() {
        return serverName;
    }
    public String getLogin() {
        return login;
    }
    public String getMessage() {
        return message;
    }
}
