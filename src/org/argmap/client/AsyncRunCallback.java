package org.argmap.client;

import org.argmap.client.ArgMap.MessageType;

import com.google.gwt.core.client.RunAsyncCallback;

public abstract class AsyncRunCallback implements RunAsyncCallback {
	@Override
	public void onFailure(Throwable reason) {
		ArgMap.messageTimed("Code download failed", MessageType.ERROR);
		Log.log("arc.on", "Code download failed" + reason.toString());
	}
}
