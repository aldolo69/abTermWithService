package ab.term;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

public class OutputService extends StandOutWindow implements OnClickListener,
		OnKeyListener, TextWatcher {

	// service management.
	private Messenger mService = null;
	private boolean mIsBound;

	private EditText textEdit = null;
	private EditText textOutput = null;
	// cut&paste are managed by this class
	private EditTextSelectionUtility mEtsuEdit = null;
	private EditTextSelectionUtility mEtsuOutput = null;
	// main menu management
	private Context mContext;
	private PopupUtility mPu;
	private int id = -1;// this window id

	// command line management
	private CmdStore cmdStore;
	private boolean bCtrlPressed = false;
	private Button mButtonControl = null;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null,
						CmdLineService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null;
		}
	};

	void doBindService() {
		bindService(new Intent(this, CmdLineService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							CmdLineService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	/**************************** data entry ********************************/

	class CmdStore {
		/**** remember 10 commands **************/
		private static final int cmdItems = 10;
		private String cmds[] = new String[cmdItems];
		private int cmdCurrentInsert = 0;
		private int cmdCurrentRead = 0;

		/**************************************/
		public String getPrev() {
			int prev = cmdCurrentRead - 1;
			if (prev < 0)
				prev = cmdItems - 1;
			cmdCurrentRead = prev;
			return (cmds[prev]);
		}

		public String getNext() {
			int nxt = cmdCurrentRead + 1;
			if (nxt == cmdItems)
				nxt = 0;
			cmdCurrentRead = nxt;
			return (cmds[nxt]);
		}

		public void setCmd(String cmd) {
			cmds[cmdCurrentInsert] = cmd;
			cmdCurrentInsert++;
			if (cmdCurrentInsert == cmdItems)
				cmdCurrentInsert = 0;
			cmdCurrentRead = cmdCurrentInsert;
		}
	}

	private void onEnterKey() {
		String strCmd = textEdit.getText().toString();
		if (strCmd.length() == 0)
			return;
		textEdit.setText("");
		cmdStore.setCmd(strCmd);
		sendMessageToService(strCmd);

	}

	// textwatcher. manage ctrl-... sequence
	private int iInsertCharStart = -1;
	private boolean bInserting=false;
	

	public void afterTextChanged(Editable s) {
		if (bCtrlPressed == true&&iInsertCharStart>=0) {
			char cInsertChar=s.charAt(iInsertCharStart);
			char cCtrlChar = '?';
			if (cInsertChar >= 'A' && cInsertChar <= 'Z') {
				cCtrlChar = (char) ((int) cInsertChar - (int) 'A' + 1);
			}
			if (cInsertChar >= 'a' && cInsertChar <= 'z') {
				cCtrlChar = (char) ((int) cInsertChar - (int) 'a' + 1);
			}
			switch (cInsertChar) {
			case '@':
				cCtrlChar = (char) 0;
				break;
			case '[':
				cCtrlChar = (char) 27;
				break;
			case '~':
				cCtrlChar = (char) 28;
				break;
			case ']':
				cCtrlChar = (char) 29;
				break;
			case '^':
				cCtrlChar = (char) 30;
				break;
			case '_':
				cCtrlChar = (char) 31;
				break;
			}
			if( cCtrlChar != '?')
			{
				bInserting=true;
				textEdit.getText().replace(iInsertCharStart, iInsertCharStart+1, String.valueOf(cCtrlChar));
				iInsertCharStart=-1;	
				bInserting=false;
				
			}

		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		if (after == 1&&bInserting==false) {
			iInsertCharStart = start;
		} else {
			iInsertCharStart = -1;
		}
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonUp:
			textEdit.setText(cmdStore.getPrev());
			break;
		case R.id.buttonDown:
			textEdit.setText(cmdStore.getNext());
			break;
		case R.id.buttonEnter:
			onEnterKey();
			break;
		case R.id.buttonCtrl:
			toggleCtrl();
			break;
		case R.id.popup_mainmenu_quit:
			mPu.dismiss();// closePopup
			mPu = null;
			closeAll();
			break;
		case R.id.popup_mainmenu_settings:
			mPu.dismiss();// closePopup
			mPu = null;
			hide(id);
			Intent dialogIntent = new Intent(getBaseContext(),
					SettingsActivity.class);
			dialogIntent.putExtra("id", id);

			dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().startActivity(dialogIntent);
			break;

		}
	}

	// manage enter key
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// popup open????
		if (mPu != null) {
			mPu.dismiss();// closePopup
			mPu = null;
			return true;
		}

		if (v == textOutput) {
			return true;
		}
		if (v == textEdit) {

			// if keydown and "enter" is pressed
			if ((event.getAction() == KeyEvent.ACTION_DOWN)
					&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
				onEnterKey();
				return true;
			}
		}
		return false;
	}

	// output data coming from SH process or the
	// keyboard*****************************************
	private void appendTextAndScroll(String text) {
		if (textOutput != null) {

			int iLen = textOutput.getText().length();
			if (iLen > 4000) {
				textOutput.setText(textOutput.getText().delete(0, iLen - 4000)
						+ text);
			} else {
				textOutput.append(text);

			}
			iLen = textOutput.getText().length();

			textOutput.setSelection(iLen, iLen);
		}
	}

	/**
	 * Handle incoming messages from CmdLineService
	 */
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case CmdLineService.MSG_STRING:
				String strIn = msg.getData().getString("STRING");
				appendTextAndScroll(strIn);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToService(String str) {
		if (mIsBound) {
			if (mService != null) {
				try {
					Bundle b = new Bundle();
					b.putString("STRING", str);
					Message msg = Message.obtain(null,
							CmdLineService.MSG_STRING);
					msg.setData(b);
					msg.replyTo = mMessenger;

					mService.send(msg);
				} catch (RemoteException e) {
				}
			}
		}
	}

	/****** popup management *****************************************/

	@Override
	public boolean onClose(int id, Window window) {

		doUnbindService();
		stopService(new Intent(OutputService.this, CmdLineService.class));
		return false;
	}

	public boolean preferences() {
		return false;
	}

	@Override
	public String getAppName() {
		return this.getString(R.string.app_name);
	}

	@Override
	public int getAppIcon() {
		return R.drawable.app_terminal;
	}

	@Override
	public void createAndAttachView(int id, FrameLayout frame) {
		mContext = (Context) this;
		this.id = id;
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.output, frame, true);

		((Button) v.findViewById(R.id.buttonEnter)).setOnClickListener(this);
		((Button) v.findViewById(R.id.buttonUp)).setOnClickListener(this);
		((Button) v.findViewById(R.id.buttonDown)).setOnClickListener(this);
		mButtonControl = ((Button) v.findViewById(R.id.buttonCtrl));
		mButtonControl.setOnClickListener(this);
		setCtrl(false);

		this.textOutput = (EditText) v.findViewById(R.id.outputText);
		mEtsuOutput = new EditTextSelectionUtility((Context) this, textOutput);
		textOutput.setOnKeyListener(this);

		this.textEdit = (EditText) v.findViewById(R.id.editText);
		mEtsuEdit = new EditTextSelectionUtility((Context) this, textEdit);
		textEdit.setOnKeyListener(this);
		textEdit.addTextChangedListener(this);
		cmdStore = new CmdStore();
		PrepareAccordingToSetup();
		startService(new Intent(OutputService.this, CmdLineService.class));
		doBindService();

	}

	// the window will be centered
	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		return new StandOutLayoutParams(id, 250, 300,
				StandOutLayoutParams.CENTER, StandOutLayoutParams.CENTER);
	}

	// move the window by dragging the view
	@Override
	public int getFlags(int id) {
		return super.getFlags(id)
				| StandOutFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE
				| StandOutFlags.FLAG_DECORATION_SYSTEM
				| StandOutFlags.FLAG_BODY_MOVE_ENABLE
				| StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
				| StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
				| StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
	}

	public String getPersistentNotificationTitle(int id) {
		return getAppName() + " " + this.getString(R.string.running);
	}

	@Override
	public String getPersistentNotificationMessage(int id) {
		return this.getString(R.string.action_clktoclose);
	}

	@Override
	public Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseIntent(this, OutputService.class, id);

	}

	@Override
	public int getHiddenIcon() {
		return android.R.drawable.ic_menu_info_details;
	}

	@Override
	public String getHiddenNotificationTitle(int id) {
		return getAppName() + " " + this.getString(R.string.hidden);
	}

	@Override
	public String getHiddenNotificationMessage(int id) {
		return this.getString(R.string.action_clktoshow);
	}

	// return an Intent that restores the MultiWindow
	@Override
	public Intent getHiddenNotificationIntent(int id) {
		return StandOutWindow.getShowIntent(this, getClass(), id);
	}

	// remove default menu at all. show a new popup instead
	@Override
	public PopupWindow getDropDown(final int id) {

		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.popup_main_menu, null, false);
		v.setOnKeyListener(this);
		View vItem;
		vItem = v.findViewById(R.id.popup_mainmenu_quit);
		vItem.setOnClickListener(this);
		vItem = v.findViewById(R.id.popup_mainmenu_settings);
		vItem.setOnClickListener(this);

		mPu = new PopupUtility(mContext, v, null);
		return (PopupWindow) mPu;
	};

	@Override
	public boolean onShow(int id, Window window) {
		if (this.id == id) {
			PrepareAccordingToSetup();
		}
		return false;
	}

	void PrepareAccordingToSetup() {

		if (textEdit == null)
			return;

		// apply setup from settings.
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		int iFontSize = Integer.parseInt(preferences.getString(
				mContext.getString(R.string.settings_fontsize_key),
				mContext.getString(R.string.settings_fontsize_default)));
		int iFontType = Integer.parseInt(preferences.getString(
				mContext.getString(R.string.settings_fonttype_key),
				mContext.getString(R.string.settings_fonttype_default)));

		Typeface tf;
		switch (iFontType) {
		case 0:
		default:
			tf = Typeface.create(Typeface.MONOSPACE, 0);
			break;
		case 1:
			tf = Typeface.create(Typeface.SANS_SERIF, 0);
			break;
		case 2:
			tf = Typeface.create(Typeface.SERIF, 0);
			break;
		}

		textEdit.setTextSize(TypedValue.COMPLEX_UNIT_PT, iFontSize);
		textEdit.setTypeface(tf);
		textOutput.setTextSize(TypedValue.COMPLEX_UNIT_PT, iFontSize);
		textOutput.setTypeface(tf);

	}

	// ctrl button management
	private void toggleCtrl() {
		bCtrlPressed = !bCtrlPressed;
		setCtrl(bCtrlPressed);
	}

	private void setCtrl(boolean b) {
		bCtrlPressed = b;
		if (b == true) {
			setRightBitmap(mButtonControl,
					this.getResources().getDrawable(R.drawable.led_green));
		} else {
			setRightBitmap(mButtonControl,
					this.getResources().getDrawable(R.drawable.led_gray));

		}
	}

	private void setRightBitmap(Button btn, Drawable drwIcon) {
		int iSize = (int) btn.getTextSize() / 2;
		Drawable drw[] = btn.getCompoundDrawables();
		drwIcon.setBounds(0, 0, iSize, iSize);
		btn.setCompoundDrawables(drw[0], drw[1], drwIcon, drw[3]);
	}
	//

}
