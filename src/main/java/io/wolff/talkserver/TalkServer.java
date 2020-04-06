package io.wolff.talkserver;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

public class TalkServer extends WebSocketServer {
	
	private Map<UUID, User> userMap = new HashMap<>();
	private Map<WebSocket, User> usersBySocket = new HashMap<>();
	
	private Gson g = new Gson();

	public TalkServer(InetSocketAddress inetSocketAddress) {
		super(inetSocketAddress);
	}

	public static void main(String[] args) {
		new TalkServer(new InetSocketAddress("localhost",8081)).run();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// a new user connected, give the user a UUID
		// TODO: use objects
		Map<String, Object> newUserMessage = new HashMap<>();
		newUserMessage.put("type", "new-user");
		Map<String, String> currentUserMap = new HashMap<>();
		for(User user : this.userMap.values()) {
			currentUserMap.put(user.id.toString(), user.displayName);
		}
		newUserMessage.put("data", currentUserMap);
		conn.send(g.toJson(newUserMessage));
		
		User u = new User(conn, UUID.randomUUID());
		// TODO: better logging
		System.out.println(u.id + " joined");
		
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
		System.out.println(userToRemove.id + " left");
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
		User sender = this.usersBySocket.get(conn);
		
		System.out.println(sender.id + ": "+message);
		
		if("send-offer".equals(req.get("type"))) {
			// caller wants to send offer to target in message
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
		} else if("set-username".equals(req.get("type"))) {
			// update this user
			String newUsername = (String) req.get("data");
			this.usersBySocket.get(conn).displayName = newUsername;
			// now tell everyone else
			Map<String, Object> outMessage = new HashMap<>();
			outMessage.put("type", "set-username");
			outMessage.put("from", sender.id);
			outMessage.put("data", req.get("data"));
			for(WebSocket user : usersBySocket.keySet()) {
				if(!user.equals(conn)) {
					user.send(g.toJson(outMessage));
				}
			}
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
