package io.wolff.talkserver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main extends WebSocketServer {
	
	private Map<UUID, User> userMap = new HashMap<>();
	private Map<WebSocket, User> usersBySocket = new HashMap<>();
	
	private Gson g = new Gson();

	public Main(InetSocketAddress inetSocketAddress) {
		super(inetSocketAddress);
	}

	public static void main(String[] args) {
		new Main(new InetSocketAddress("localhost",8081)).run();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// a new user connected, give the user a UUID
		// TODO: use objects
		Map<String, Object> newUserMessage = new HashMap<>();
		User u = new User(conn, UUID.randomUUID());
		
		newUserMessage.put("type", "new-user");
		newUserMessage.put("data", u.id);
		conn.send(g.toJson(newUserMessage));
		
		// tell everyone a new user connected, get the ball rolling
		for(User other : userMap.values()) {
			Map<String, Object> userJoinMessage = new HashMap<>();
			userJoinMessage.put("type", "user-join");
			userJoinMessage.put("data", u.id);
			other.connection.send(g.toJson(userJoinMessage));
		}
		
		// put this user in our maps
		this.userMap.put(u.id, u);
		this.usersBySocket.put(conn, u);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// user disconnected
		User userToRemove = null;
		for(User u : userMap.values()) {
			if(u.connection.equals(conn)) {
				userToRemove = u;
				break;
			}
		}
		userMap.remove(userToRemove.id);
		usersBySocket.remove(conn);
		for(User remain : userMap.values()) {
			Map<String, Object> userLeaveMessage = new HashMap<>();
			userLeaveMessage.put("type", "user-leave");
			userLeaveMessage.put("data", userToRemove.id);
			remain.connection.send(g.toJson(userLeaveMessage));
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// TODO: do this smarter
		@SuppressWarnings("rawtypes")
		Map req = g.fromJson(message, Map.class);
		Map<String, Object> resp = new HashMap<>();
		User sender = this.usersBySocket.get(conn);
		
		if("send-offer".equals(req.get("type"))) {
			// TODO: caller wants to send offer to target in message
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) req.get("data");
			User target = userMap.get(UUID.fromString((String) req.get("target")));
			Map<String, Object> targetMessage = new HashMap<>();
			targetMessage.put("type", "receive-offer");
			targetMessage.put("from", sender.id.toString());
			targetMessage.put("data", data);
			target.connection.send(g.toJson(targetMessage));
		} else if("send-answer".equals(req.get("type"))) {
			// TODO: someone is sending an answer to the target
			User target = userMap.get(UUID.fromString((String) req.get("target")));
			@SuppressWarnings("unchecked")
			Map<String, Object> answer = (Map<String, Object>) req.get("data");
			Map<String, Object> targetMessage = new HashMap<>();
			targetMessage.put("type", "receive-answer");
			targetMessage.put("from", sender.id.toString());
			targetMessage.put("data", answer);
			target.connection.send(g.toJson(targetMessage));
		} else if("ice-candidate".equals(req.get("type"))) {
			User target = userMap.get(UUID.fromString((String) req.get("target")));
			Map<String, Object> outMessage = new HashMap<>();
			outMessage.put("type", "ice-candidate");
			outMessage.put("from", sender.id);
			outMessage.put("data", req.get("data"));
			target.connection.send(g.toJson(outMessage));
		} else {
			throw new RuntimeException("Unsupported Request: "+req.get("type"));
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		
	}

}
