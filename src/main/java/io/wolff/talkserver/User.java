package io.wolff.talkserver;

import java.util.UUID;

import org.java_websocket.WebSocket;

public class User {
	public String displayName;
	public final WebSocket connection;
	public final UUID id;
	public User(WebSocket connection, UUID id) {
		super();
		this.connection = connection;
		this.id = id;
	}
	
	
}
