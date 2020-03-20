package io.wolff.talkserver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main extends WebSocketServer {
	
	private Object offerMessage = null;
	private WebSocket offerSocket = null;
	private WebSocket answerSocket = null;
	private List<Object> answerIceBuffer = new ArrayList<>();

	public Main(InetSocketAddress inetSocketAddress) {
		super(inetSocketAddress);
	}

	public static void main(String[] args) {
		new Main(new InetSocketAddress("localhost",8081)).run();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		Gson g = new Gson();
		// TODO: do this smarter
		@SuppressWarnings("rawtypes")
		Map req = g.fromJson(message, Map.class);
		Map<String, Object> resp = new HashMap<>();
		// TODO: different init (put in onOpen)
		if("get-offer".equals(req.get("type"))) {
			resp.put("type", "get-offer");
			resp.put("data", offerMessage);
			conn.send(g.toJson(resp));
		} else if("set-offer".equals(req.get("type")) ) {
			this.offerMessage = req.get("data");
			offerSocket = conn;
		} else if("answer".equals(req.get("type"))) {
			// tell the caller there was an answer
			answerSocket = conn;
			// if any ice candidates arrived before he showed up, send them to him
			for(Object ice : this.answerIceBuffer) {
				Map<String, Object> iceResp = new HashMap<>();
				iceResp.put("type", "ice-candidate");
				iceResp.put("data", ice);
				offerSocket.send(g.toJson(iceResp));
			}
			this.answerIceBuffer.clear();
			resp.put("type", "answer");
			resp.put("data", req.get("data"));
			offerSocket.send(g.toJson(resp));
		} else if("ice-candidate".equals(req.get("type"))) {
			// TODO: turns out this can happen as soon as a person connects
			// relay the message to the other person
			resp.put("type", "ice-candidate");
			resp.put("data", req.get("data"));
			if(conn.equals(this.offerSocket)) {
				// the answerer may not have arrived yet
				if(this.answerSocket == null) {
					this.answerIceBuffer.add(req.get("data"));
				} else {
					this.answerSocket.send(g.toJson(resp));
				}
			} else {
				this.offerSocket.send(g.toJson(resp));
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
