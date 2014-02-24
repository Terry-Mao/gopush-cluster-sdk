package com.ks.gopush.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ks.gopush.cli.utils.Constant;
import com.ks.gopush.cli.utils.HttpUtils;

public class GoPushCli {
	public GoPushCli(String host, Integer port, String key, Integer expire,
			Integer mid, Integer pmid, Listener listener) {
		this.host = host;
		this.port = port;
		this.key = key;
		this.expire = expire;
		this.mid = mid;
		this.pmid = pmid;
		this.listener = listener;
	}

	public void start(boolean isSync) {
		String[] node = null;
		try {
			node = getNodeHostAndPort(HttpUtils.getURL("http", host, port,
					"/server/get", "key", key, "expire", expire, "proto", 2));
			initSocket(node);
			// 协议已经握手，打开
			listener.onOpen();
			ArrayList<Message> messages = getOfflineMessage();
			// 如果有离线消息
			if (messages != null) {
				listener.onOfflineMessage(messages);
			}
			// 准备定时心跳任务
			heartbeatTask = new Thread(new HeartbeatTask());
			heartbeatTask.start();
		} catch (Exception e) {
			listener.onError(e, e.getMessage());
			destory();
			return;
		}

		// 如果是同步协议，block
		if (isSync) {
			try {
				crawl();
			} catch (IOException e) {
				listener.onError(e, e.getMessage());
			} finally {
				destory();
			}
		} else {
			// 异步，nonblock
			new Thread() {
				public void run() {
					try {
						crawl();
					} catch (IOException e) {
						listener.onError(e, e.getMessage());
					} finally {
						destory();
					}
				}
			}.start();
		}
	}

	private void initSocket(String[] node) throws Exception {
		try {
			this.socket = new Socket(node[0], Integer.parseInt(node[1]));
			this.socket.setKeepAlive(true);
			this.socket.setSoTimeout(expire * 2 * 1000);
			this.reader = new BufferedReader(new InputStreamReader(
					this.socket.getInputStream()));
			this.writer = new PrintWriter(new OutputStreamWriter(
					this.socket.getOutputStream()));
		} catch (Exception e) {
			throw new Exception("创建套接字发生异常", e);
		}

		if (!sendHeader()) {
			throw new Exception("握手失败");
		}
	}

	public void destory() {
		if (isDesotry) {
			return;
		}
		isDesotry = true;
		listener.onClose();
		listener = new ListenerAdapter() {
		};
		// 关闭连接 释放线程
		if (heartbeatTask != null && !heartbeatTask.isInterrupted()) {
			heartbeatTask.interrupt();
		}
		if (socket != null && !socket.isClosed() && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	private String[] getNodeHostAndPort(String domain) throws Exception {
		try {
			JSONObject data = new JSONObject(HttpUtils.get(domain));
			// 判断协议
			if (data.getInt(Constant.KS_NET_JSON_KEY_RET) == Constant.KS_NET_STATE_OK) {
				JSONObject jot = data
						.getJSONObject(Constant.KS_NET_JSON_KEY_DATA);
				String server = jot.getString(Constant.KS_NET_JSON_KEY_SERVER);
				String[] result = server.split(":");
				isGetNode = true;
				return result;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("获取节点信息时发生异常", e);
		}
	}

	private boolean sendHeader() throws IOException {
		String expireStr = expire.toString();
		String protocol = "*3\r\n$3\r\nsub\r\n$" + key.length() + "\r\n" + key
				+ "\r\n$" + expireStr.length() + "\r\n" + expireStr + "\r\n";

		// 发送请求协议
		send(protocol);
		String response = receive();
		if (response.startsWith("+")) {
			// 初始心跳
			isHandshake = true;
			return true;
		} else if (response.startsWith("-")) {
			// 协议错误
			return false;
		} else {
			throw new IllegalArgumentException("无法识别的响应内容:" + response);
		}
	}

	private void send(String message) {
		assert socket != null;

		writer.print(message);// 这里原本实现是没有加换行符的
		writer.flush();
	}

	private String receive() throws IOException {
		assert socket != null;

		return reader.readLine();
	}

	private void crawl() throws IOException {
		String line = null;
		try {
			while ((line = receive()) != null) {
				if (line.startsWith("+")) {
					// 忽略心跳
				} else if (line.startsWith("$")) {
					line = receive();
					JSONObject jot = new JSONObject(line);
					Message msg = new Message(
							jot.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
							jot.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
							jot.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_GID));
					listener.onOnlineMessage(msg);
				} else if (line.startsWith("-")) {
					// 存在异常
					return;
				}
			}
		} catch (IOException e) {
			throw new IOException("接受数据时发生异常", e);
		} catch (JSONException e) {
			throw new IOException("解析JSON时发生异常", e);
		}
	}

	private ArrayList<Message> getOfflineMessage() throws IOException,
			JSONException {
		// 获取离线消息
		String offlineMessage = HttpUtils.get(HttpUtils.getURL("http", host,
				port, "/msg/get", "key", key, "mid", mid, "pmid", pmid));
		JSONObject jot = new JSONObject(offlineMessage);
		// 协议错误
		if (jot.getInt(Constant.KS_NET_JSON_KEY_RET) != Constant.KS_NET_STATE_OK) {
			return null;
		}

		// 没有data数据
		if (jot.isNull(Constant.KS_NET_JSON_KEY_DATA)) {
			return null;
		}

		ArrayList<Message> res = new ArrayList<Message>();
		// 获取私信列表
		JSONObject data = jot.getJSONObject(Constant.KS_NET_JSON_KEY_DATA);
		// 没有msgs数据
		if (!data.isNull(Constant.KS_NET_JSON_KEY_MESSAGES)) {
			JSONArray arr = data
					.getJSONArray(Constant.KS_NET_JSON_KEY_MESSAGES);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject message = new JSONObject(arr.getString(0));
				res.add(new Message(message
						.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
						message.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
						0));
			}
		}
		// 获取公信列表
		// 没有msgs数据
		if (!data.isNull(Constant.KS_NET_JSON_KEY_PMESSAGES)) {
			JSONArray arr = data
					.getJSONArray(Constant.KS_NET_JSON_KEY_PMESSAGES);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject message = new JSONObject(arr.getString(0));
				res.add(new Message(message
						.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
						message.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
						1));
			}
		}

		return res;
	}

	public boolean isGetNode() {
		return isGetNode;
	}

	public boolean isHandshake() {
		return isHandshake;
	}

	class HeartbeatTask implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				// 发送心跳
				System.out.println("heartBeat run!");
				send("h");
				try {
					TimeUnit.SECONDS.sleep(expire);
				} catch (InterruptedException e) {
					System.err.println("Timer is stop");
					break;
				}
			}
		}
	}

	// 对象属性
	private String key;
	private Integer expire;
	private Integer mid;
	private Integer pmid;
	private Listener listener;
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
	private String host;
	private Integer port;

	private Thread heartbeatTask;

	private boolean isGetNode = false;
	private boolean isHandshake = false;
	private boolean isDesotry = false;

}
