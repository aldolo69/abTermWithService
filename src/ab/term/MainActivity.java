package ab.term;


 
import wei.mark.standout.StandOutWindow;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private Messenger mService = null;
	private boolean mIsBound;
	private TextView textOutput;
	private TextView textEdit;
	 
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            textOutput.setText("Attached.");
            try {
                Message msg = Message.obtain(null, CmdLineService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            textOutput.setText("Disconnected.");
        }
    };
	
	
	
	
	
	
	
	
	
	
	
	 
	
	void doBindService() {
	    bindService(new Intent(this, CmdLineService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	    textOutput.setText("Binding.");
	}

	
	
	
	
	
	
	 void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null, CmdLineService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service has crashed.
	            }
	        }
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        textOutput.setText("Unbinding.");
	    }
	}
	    public void onClick(View v) {
	 		String lvStrCmd;
			lvStrCmd = textEdit.getText().toString();
			sendMessageToService(lvStrCmd);
		}
	    @Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			
			StandOutWindow.show(this, OutputService.class, StandOutWindow.DEFAULT_ID);
           finish();
			
			/*
			setContentView(R.layout.activity_main);
			
			((Button) findViewById(R.id.buttonEnter)).setOnClickListener(this);
			((Button) findViewById(R.id.buttonUp)).setOnClickListener(this);
			((Button) findViewById(R.id.buttonDown)).setOnClickListener(this);
		
			
			this.textOutput=(TextView) findViewById(R.id.outputText);
			this.textEdit=(TextView) findViewById(R.id.editText);
			startService(new Intent(MainActivity.this, CmdLineService.class));
			doBindService(); 
*/
			
			
		
		}
	
	

	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        try {
	            doUnbindService();
	            stopService(new Intent(MainActivity.this, CmdLineService.class));
	        } catch (Throwable t) {
	            Log.e("MainActivity", "Failed to unbind from the service", t);
	        }

	    }


	    /**
         * Handle incoming messages from CmdLineService
         */
        private class IncomingHandler extends Handler {          
                @Override
                public void handleMessage(Message msg) {
                        // Log.d(LOGTAG,"IncomingHandler:handleMessage");
                        switch (msg.what) {
/*                        case CmdLineService.MSG_SET_INT_VALUE:
                             textOutput.setText("Int Message: " + msg.arg1);
                                break;*/
                        case CmdLineService.MSG_STRING:
                                String strIn = msg.getData().getString("STRING");
                                String strPrev=textOutput.getText().toString();
                                textOutput.setText(strPrev + strIn);
                                break;
                        default:
                                super.handleMessage(msg);
                        }
                }
        }       
	    
	    
	    @Override
		    protected void onSaveInstanceState(Bundle outState) {
		        super.onSaveInstanceState(outState);
//salva i dati da qualche parte
//	        outState.putString("textOutput", textOutput.getText().toString());
		    }
	    private void restoreMe(Bundle state) {
	        if (state!=null) {
//*recupera i dati e rimettili a video
//       	   textOutput.setText(state.getString("textOutput"));
	        }
	    }

	    private void sendMessageToService(String str) {
	        if (mIsBound) {
	            if (mService != null) {
	                try {       
	                	Bundle b = new Bundle();
		                b.putString("STRING", str);
		                Message msg = Message.obtain(null, CmdLineService.MSG_STRING);		                
		                msg.setData(b);
		                msg.replyTo = mMessenger;
	                    
	                    
	                    
	                    mService.send(msg);
	                } catch (RemoteException e) {
	                }
	            }
	        }
	    }
	
	
	
	
	
 }
