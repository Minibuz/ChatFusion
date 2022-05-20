package fr.umlv.java.models;

public class Message {
    private String login;
    private String text;

    public Message() {
        login = null;
        text = null;
    }

    public Message(String login, String text) {
        this.login = login;
        this.text = text;
    }

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getText() {
        return text;
    }
    public void setText(String texte) {
        this.text = texte;
    }
}
