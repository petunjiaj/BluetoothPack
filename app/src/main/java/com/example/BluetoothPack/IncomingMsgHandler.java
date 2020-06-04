package com.example.BluetoothPack;

import android.os.Handler;
import android.util.Log;

import java.lang.ref.WeakReference;

/** IncomingMsgHandler
 *  this is an Handler extension, that notify activity (witch contains an instance),
 *  by incoming message from BluetoothConnectionService (witch calls handleMessage).
 *  It's listener-interface is implemented in relative Activity to notify msg-updates.
 */

class IncomingMsgHandler extends Handler {
    private static final String TAG = "handler";
    WeakReference<OnMessageReceivedListener> listenerReference;

    // constructor: needs a listener-interface to notify updates.
    public IncomingMsgHandler(OnMessageReceivedListener listener) {
        listenerReference = new WeakReference<>(listener);
    }

    // handle new incoming messages:
    public synchronized void handleMessage(String message) {
        Log.d(TAG, "incoming message:" + message);
        OnMessageReceivedListener listener = listenerReference.get();
        if(listener != null) {
            Log.d(TAG, "listener not null. Handling message..");
            listener.handleMessage(message);
        }
        else {
            Log.d(TAG, "listener null!!");
        }
    }

    // clear interface.
    public void clear() {
        listenerReference.clear();
    }

    // listener for updates:
    public interface OnMessageReceivedListener {
        void handleMessage(String message);
    }
}

