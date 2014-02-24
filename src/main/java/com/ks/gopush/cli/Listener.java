package com.ks.gopush.cli;

import java.util.ArrayList;

public interface Listener {
	void onOpen();

	void onClose();

	void onOnlineMessage(Message message);

	void onOfflineMessage(ArrayList<Message> messages);

	void onError(Throwable e, String message);
}
