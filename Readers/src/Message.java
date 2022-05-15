public class Message {
	private final String login;
	private final String text;
	
	public Message(String login, String text) {
		this.login = login;
		this.text = text;
	}
	
	public String getLogin() {
		return login;
	}
	
	public String getText() {
		return text;
	}
}
