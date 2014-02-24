package com.ks.gopush.cli;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GoPushCliTest {
	@Before
	public void init() {
		local.set(new GoPushCli("42.96.200.187", 8090, "Terry-Mao", 30, 0, 0,
				new Listener() {
					@Override
					public void onOpen() {
						System.err.println("dang dang dang dang~");
					}

					@Override
					public void onOnlineMessage(Message message) {
						System.err.println("online message: "
								+ message.getMsg());
					}

					@Override
					public void onOfflineMessage(ArrayList<Message> messages) {
						if (messages != null)
							for (Message message : messages) {
								System.err.println("offline message: "
										+ message.getMsg());
							}
					}

					@Override
					public void onError(Throwable e, String message) {
						Assert.fail(message);
					}

					@Override
					public void onClose() {
						System.err.println("pu pu pu pu~");
					}
				}));
	}

	// @Test
	public void testNoSync() {
		GoPushCli cli = local.get();
		cli.start(false);

		Assert.assertTrue("获取节点失败", cli.isGetNode());
		Assert.assertTrue("握手失败", cli.isHandshake());
		cli.destory();
	}

	@Test
	public void testSync() {
		final GoPushCli cli = local.get();
		new Thread() {
			public void run() {
				cli.start(true);
			}
		}.start();
		try {
			TimeUnit.SECONDS.sleep(10000000);
		} catch (InterruptedException e) {
		}
		Assert.assertTrue("获取节点失败", cli.isGetNode());
		Assert.assertTrue("握手失败", cli.isHandshake());
		cli.destory();
	}

	private ThreadLocal<GoPushCli> local = new ThreadLocal<GoPushCli>();
}
