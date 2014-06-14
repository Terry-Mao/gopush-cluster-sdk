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
	/**
	 * 初始化GopushCli
	 * 
	 * @param host
	 *            web模块的host
	 * @param port
	 *            web模块的端口
	 * @param key
	 *            订阅的key
	 * @param expire
	 *            设置过期和心跳时间（单位：秒）
	 * @param mid
	 *            设置上次接受私信推送以来最大的消息ID
	 * @param pmid
	 *            设置上次接受公信推送以来最大的消息ID
	 * @param listener
	 *            设置监听器
	 */
	public GoPushCli(String host, Integer port, String key, Integer heartbeat,
			long mid, long pmid, Listener listener) {
		this.host = host;
		this.port = port;
		this.key = key;
		this.heartbeat = heartbeat;
		this.mid = mid;
		this.pmid = pmid;
		this.listener = listener;
	}

	/**
	 * 开始订阅
	 * 
	 * @param isSync
	 *            true: 同步订阅，会阻塞在start函数。 false: 异步订阅，订阅成功后会返回。
	 */
	public void start(boolean isSync) {
		String[] node = null;
		try {
			node = getNodeHostAndPort(HttpUtils.getURL("http", host, port,
					"/server/get", "key", key, "expire", heartbeat, "proto", 2));
			// 初始化socket
			initSocket(node);
			// 协议已经握手，打开
			listener.onOpen();
			ArrayList<PushMessage> messages = getOfflineMessage();
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
			} catch (Exception e) {
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
					} catch (Exception e) {
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
			// 两倍超时时间
			this.socket.setSoTimeout((heartbeat + 15) * 1000);
			this.reader = new BufferedReader(new InputStreamReader(
					this.socket.getInputStream()));
			this.writer = new PrintWriter(new OutputStreamWriter(
					this.socket.getOutputStream()));
			// 发送请求协议头
			sendHeader();
		} catch (Exception e) {
			throw new Exception("初始化套接字错误", e);
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
			int ret = data.getInt(Constant.KS_NET_JSON_KEY_RET);
			if (ret == Constant.KS_NET_STATE_OK) {
				JSONObject jot = data
						.getJSONObject(Constant.KS_NET_JSON_KEY_DATA);
				String server = jot.getString(Constant.KS_NET_JSON_KEY_SERVER);
				String[] result = server.split(":");
				// 已经获取节点
				isGetNode = true;
				return result;
			} else {
				throw new Exception("获取come连接节点时返回码错误: " + ret);
			}
		} catch (IOException e) {
			throw new Exception("获取comet连接节点信息时网络失败", e);
		} catch (JSONException e) {
			throw new Exception("获取comet连接节点信息时json解析失败", e);
		}
	}

	private void sendHeader() throws Exception {
		String heartbeatStr = heartbeat.toString();
		String protocol = "*3\r\n$3\r\nsub\r\n$" + key.length() + "\r\n" + key
				+ "\r\n$" + heartbeatStr.length() + "\r\n" + heartbeatStr + "\r\n";

		// 发送请求协议
		send(protocol);
		String response = receive();
		if (response.startsWith("+")) {
			// 初始心跳
			isHandshake = true;
		} else if (response.startsWith("-")) {
			// 协议错误
			throw new Exception("comet节点握手协议错误: " + response);
		} else {
			throw new IllegalArgumentException("无法识别comet返回协议: " + response);
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

	private void crawl() throws Exception {
		String line = null;
		try {
			while ((line = receive()) != null) {
				if (line.startsWith("+")) {
					// 忽略心跳
				} else if (line.startsWith("$")) {
					line = receive();
					JSONObject jot = new JSONObject(line);
					PushMessage msg = new PushMessage(
							jot.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
							jot.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
							jot.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_GID));
					// 过滤重复数据，（获取离线消息之后的头几条在线消息可能会重复）
					// 注意之后不需要更新mmid和pmid了，之后服务端是绝对的顺序以及无重复返回消息，只有离线读完读在线的过程可能会重复。为了保险还是加上
					if (msg.getGid() == Constant.KS_NET_MESSAGE_PRIVATE_GID) {
						if (msg.getMid() <= this.mid)
							continue;
						else
							mid = msg.getMid();
					} else {
						if (msg.getMid() <= pmid)
							continue;
						else
							pmid = msg.getMid();
					}

					listener.onOnlineMessage(msg);
				} else if (line.startsWith("-")) {
					throw new Exception("comet节点订阅协议错误: " + line);
				}
			}
		} catch (IOException e) {
			throw new Exception("获取comet节点订阅数据网络数据失败", e);
		} catch (JSONException e) {
			throw new Exception("解析comet节点订阅返回JSON时失败", e);
		}
	}

	private ArrayList<PushMessage> getOfflineMessage() throws Exception {
		try {
			// 获取离线消息
			String offlineMessage = HttpUtils.get(HttpUtils.getURL("http",
					host, port, "/msg/get", "key", key, "mid", mid, "pmid",
					pmid));
			JSONObject jot = new JSONObject(offlineMessage);
			// 协议错误
			int ret = jot.getInt(Constant.KS_NET_JSON_KEY_RET);
			if (ret != Constant.KS_NET_STATE_OK) {
				throw new Exception("获取离线消息协议返回码错误: " + ret);
			}

			// 没有data数据
			if (jot.isNull(Constant.KS_NET_JSON_KEY_DATA)) {
				return null;
			}

			ArrayList<PushMessage> res = new ArrayList<PushMessage>();
			int pl = 0;
			// 获取私信列表
			JSONObject data = jot.getJSONObject(Constant.KS_NET_JSON_KEY_DATA);
			// 没有msgs数据
			if (!data.isNull(Constant.KS_NET_JSON_KEY_MESSAGES)) {
				JSONArray arr = data
						.getJSONArray(Constant.KS_NET_JSON_KEY_MESSAGES);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject message = new JSONObject(arr.getString(i));
					res.add(new PushMessage(
							message.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
							message.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
							0));
				}

				// 更新最大私信ID
				pl = res.size();
				if (pl > 0)
					mid = res.get(pl - 1).getMid();
			}
			// 获取公信列表
			// 没有msgs数据
			if (!data.isNull(Constant.KS_NET_JSON_KEY_PMESSAGES)) {
				JSONArray arr = data
						.getJSONArray(Constant.KS_NET_JSON_KEY_PMESSAGES);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject message = new JSONObject(arr.getString(i));
					res.add(new PushMessage(
							message.getString(Constant.KS_NET_JSON_KEY_MESSAGE_MSG),
							message.getLong(Constant.KS_NET_JSON_KEY_MESSAGE_MID),
							1));
				}

				// 更新最大公信ID
				if (res.size() > pl)
					pmid = res.get(res.size() - 1).getMid();
			}

			return res;
		} catch (JSONException e) {
			throw new Exception("解析离线消息返回JSON时失败", e);
		}
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
				// System.out.println("heartBeat run!");
				send("h");
				try {
					TimeUnit.SECONDS.sleep(heartbeat);
				} catch (InterruptedException e) {
					System.err.println("Timer is stop");
					break;
				}
			}
		}
	}

	// 对象属性
	private String key; // subscriber key
	private Integer heartbeat; // heartbeat second
	private long mid; // private mid
	private long pmid; // public mid
	private String host; // web module host
	private Integer port; // web module port
	private Listener listener;
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	private Thread heartbeatTask;

	private boolean isGetNode = false;
	private boolean isHandshake = false;
	private boolean isDesotry = false;

}
