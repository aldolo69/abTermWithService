package ab.term;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CmdLineService extends Service {

	/****** shell management START***************/
	private Process process = null;
	private DataOutputStream os = null;
	private InputStream is = null;
	private InputStream isErr = null;
	/****** shell management END ***************/

	private Timer timer = new Timer();

	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); //registered clients.
	int mValue = 0; // Holds last value set by a client.
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_STRING = 3;
	final Messenger mMessenger = new Messenger(new IncomingHandler()); 
	 
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	class IncomingHandler extends Handler { // Handler of incoming messages from
											// clients.
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_STRING:

				try {
					if (process == null) {
						process = Runtime.getRuntime().exec("/system/bin/sh");
						os = new DataOutputStream(process.getOutputStream());
						is = process.getInputStream();
						isErr = process.getErrorStream();
					}
				} catch (Exception e) {
					sendMessageToUI("Failed to start shell");
				}

				if (process != null) {
					sendMessageToUI("$ " + msg.getData().getString("STRING")+"\n");

					try {
										
				        
						byte[] bytes = msg.getData().getString("STRING").getBytes("UTF-8");
				        
						
						os.write(bytes);
					 	os.writeBytes("\n");
						
						os.flush();
					} catch (Exception e) {
						sendMessageToUI("Failed to send command");
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToUI(String str) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				// Send data as a String
				Bundle b = new Bundle();
				b.putString("STRING", str);
				Message msg = Message.obtain(null, MSG_STRING);
				msg.setData(b);
				mClients.get(i).send(msg);

			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				mClients.remove(i);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				onTimerTick();
			}
		}, 0, 1000L);
 	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY; // run until explicitly stopped.
	}

 
	private void onTimerTick() {
//		 Log.i("TimerTick", "Tick...");
		try {
	 
			int readed = 0;
			byte[] buff = new byte[4096];
			if(is!=null)
			{
			while (is.available() > 0) {
				readed = is.read(buff);
				if (readed <= 0)
					break;
				String seg = new String(buff, 0, readed);
				sendMessageToUI(seg);
			}}
			if(isErr!=null)
			{
			while (isErr.available() > 0) {
				readed = isErr.read(buff);
				if (readed <= 0)
					break;
				String seg = new String(buff, 0, readed);
				sendMessageToUI(seg);
			}}

		} catch (Throwable t) { // you should always ultimately catch all
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (timer != null) {
			timer.cancel();
		}
 	}
}