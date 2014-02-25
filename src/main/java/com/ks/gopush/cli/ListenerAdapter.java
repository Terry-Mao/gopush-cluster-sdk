package com.ks.gopush.cli;

import java.util.ArrayList;

public abstract class ListenerAdapter implements Listener {
	@Override
	public void onClose() {

	}

	@Override
	public void onOfflineMessage(ArrayList<PushMessage> messages) {

	}

	@Override
	public void onOnlineMessage(PushMessage message) {

	}
	
	@Override
	public void onOpen() {

	}

	@Override
	public void onError(Throwable e, String message) {
	}
}
