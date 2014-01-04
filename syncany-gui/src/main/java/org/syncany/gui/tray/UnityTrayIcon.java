/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.gui.tray;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.WSClient;
import org.syncany.util.JsonHelper;

/**
 * @author pheckel
 *
 */
public class UnityTrayIcon extends TrayIcon {
	private static final Logger logger = Logger.getLogger(WSClient.class.getSimpleName());
	private WebSocketServer webSocketClient;

	public UnityTrayIcon() {
		try {
			Map<String, String> map = new HashMap<>();
			map.put("client_id", MainGUI.getClientIdentification());

			this.webSocketClient = new WebSocketServer(new InetSocketAddress(8882)) {
				@Override
				public void onOpen(WebSocket conn, ClientHandshake handshake) {
					String id = handshake.getFieldValue("client_id");
					logger.fine("Client with id '" + id + "' connected");
				}

				@Override
				public void onMessage(WebSocket conn, String message) {
					logger.fine("Unity Received from " + conn.getRemoteSocketAddress().toString() + ": " + message);
					handleCommand(JsonHelper.fromStringToMap(message));
				}

				@Override
				public void onError(WebSocket conn, Exception ex) {
					logger.fine("Server error : " + ex.toString());
				}

				@Override
				public void onClose(WebSocket conn, int code, String reason, boolean remote) {
					logger.fine(conn.getRemoteSocketAddress().toString() + " disconnected");
				}
			};

			webSocketClient.start();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						ProcessBuilder processBuilder = new ProcessBuilder("src/main/python/unitytray.py", "src/main/resources/images",
								"All folders in sync");
						processBuilder.start();
					}
					catch (IOException e) {
						throw new RuntimeException("Unable to determine Linux desktop environment.", e);
					}
				}
			}).start();

		}
		catch (Exception e) {
			throw new RuntimeException("Cannot instantiate Unity tray icon.", e);
		}
	}

	protected void handleCommand(Map<String, Object> map) {
		String command = (String)map.get("command");
		
		switch (command){
			case "DONATE":
				showDonate();
				break;
		}
	}

	public void sendToAll(String text) {
		Collection<WebSocket> con = webSocketClient.connections();
		synchronized (con) {
			for (WebSocket c : con) {
				sendTo(c, text);
			}
		}
	}

	public void sendTo(WebSocket ws, String text) {
		ws.send(text);
	}

	@Override
	public void updateFolders(Map<String, Map<String, String>> folders) {
		sendToAll(JsonHelper.fromMapToString(folders));
	}

	@Override
	public void updateStatusText(String statusText) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "update_tray_status_text");
		parameters.put("text", statusText);

		sendToAll(JsonHelper.fromMapToString(parameters));
	}

	@Override
	public void makeSystemTrayStartSync() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "start_syncing");

		sendToAll(JsonHelper.fromMapToString(parameters));
	}

	@Override
	public void makeSystemTrayStopSync() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "stop_syncing");

		sendToAll(JsonHelper.fromMapToString(parameters));
	}
}
