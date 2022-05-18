package fr.umlv.java.models;

public class Message {
    private String login;
    private String texte;

    public Message() {
        login = null;
        texte = null;
    }

    public Message(String login, String texte) {
        this.login = login;
        this.texte = texte;
    }

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getTexte() {
        return texte;
    }
    public void setTexte(String texte) {
        this.texte = texte;
    }
}
