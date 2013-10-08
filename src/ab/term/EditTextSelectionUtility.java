package ab.term;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class EditTextSelectionUtility implements OnClickListener, OnKeyListener {

	private EditText mTxt = null;

	/*
	 * flag: -selection=initially false, true after onlongclick -swift=initially
	 * false, true after onlonclick -waitswift=initially false, true after a
	 * action_down within the nearby of the start or end of the selection
	 * 
	 * globals: -swiftfrom=start/end to remember the source of the swith. the
	 * other end of the selection does not move -lastX -lastY -lastOffset
	 * -lastOffsetStart,lastOffsetEnd
	 * 
	 * 
	 * 
	 * onlongclick: swift=true? do nothing and exit
	 * 
	 * selection=false? selection=true use lastOffset to select current word set
	 * lastOffsetStart,lastOffsetEnd select text distance lastX,lastY from
	 * startX,Y or endX,Y less then 2 chars? set swift=true,swiftfrom=start or
	 * end
	 * 
	 * selection=true? copy text swift=false selection=false
	 * 
	 * 
	 * ontouchlistener: store present x,y in lastX,lastY compute present offset
	 * and store it in lastOffset selection=false? exit and do not consume event
	 * 
	 * up event? swift=false waitswift=false exit and do not consume event
	 * 
	 * down event? selection=true? distance xy from start or end less then 2
	 * char? set waitswift=true swiftfrom = start or end else set
	 * waitswift=false
	 * 
	 * exit and do not consume event
	 * 
	 * move event? selection=true? waitswift=true? swift=true swift=true? move
	 * start or end, according to swiftfrom, to new position set
	 * lastOffsetStart,lastOffsetEnd select text exit and consume event
	 * 
	 * exit and do not consume event
	 */

	//
	private String sLastSelectedText = null;

	// used during event handling
	private boolean bSelection = false;
	private boolean bSwift = false;
	private boolean bWaitSwift = false;
	private boolean bSwiftFromStart;// true for start/false for end
	long lExitSwiftTimestamp = 0;// timeout 500ms to prevent unwanted
									// onlongclick

	// current finger position
	private int iLastX;
	private int iLastY;
	private int iLastOffset;
	// last selection made
	private int iLastOffsetStart;
	private int iLastOffsetEnd;

	// used in OnTouchListener (^2) to chose the active area around the
	// selection edges
	private int iMaxDistance;

	// used to restore EditText properties
	private int iInputType;
	private Context mContext;

	// used for the popup
	private PopupUtility mPu;

	public EditTextSelectionUtility(Context ctx, EditText et) {

		mTxt = et;
		iInputType = mTxt.getInputType();
		mContext = ctx;
		iMaxDistance = (int) Math.pow(mTxt.getTextSize(), 2);
		resetGlobals();

		mTxt.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				resetGlobals();// stop everything
				return false;
			}
		});

		mTxt.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {

				if (bSwift == true)
					return true; // ignore long click during swift
				if (lExitSwiftTimestamp != 0) {
					if (System.currentTimeMillis() - lExitSwiftTimestamp < 500) {
						// just left the swift?? ignore long click
						return true;
					} else {
						// left swift more then 500ms ago. go head...
						lExitSwiftTimestamp = 0;
					}
				}

				showPopup();
				return true;
			}
		});

		//
		// /*if (bSelection == false) {
		// bSelection = true;
		// bWaitSwift = false;// default
		// Editable sTxt = mTxt.getText();
		// int iMax = sTxt.length();
		// if (iMax > 0) {
		// for (iLastOffsetStart = iLastOffset; iLastOffsetStart > 0;
		// iLastOffsetStart--) {
		// if ("\n\t\r ".contains(sTxt.subSequence(
		// iLastOffsetStart, iLastOffsetStart + 1)) == true) {
		// if (iLastOffsetStart < iMax)
		// iLastOffsetStart++;
		// break;
		// }
		// }
		// for (iLastOffsetEnd = iLastOffset; iLastOffsetEnd < iMax;
		// iLastOffsetEnd++) {
		// if ("\n\t\r ".contains(sTxt.subSequence(
		// iLastOffsetEnd, iLastOffsetEnd + 1)) == true)
		// break;
		// }
		//
		// String sSelectedWord = "";
		// if (iLastOffsetStart < iLastOffsetEnd) {
		// sSelectedWord = sTxt
		// .toString()
		// .substring(iLastOffsetStart, iLastOffsetEnd)
		// .replaceAll("[\n\t\r\\s]", "");
		// }
		// if (sSelectedWord.length() > 0) {
		// mTxt.setSelection(iLastOffsetStart, iLastOffsetEnd);
		// mTxt.setRawInputType(InputType.TYPE_NULL);
		//
		// int iStartXy[] = getCoordFromOffset(
		// iLastOffsetStart, mTxt);
		// int iEndXy[] = getCoordFromOffset(iLastOffsetEnd,
		// mTxt);
		// int iDistanceStartXyLast = (int) Math.pow(
		// iStartXy[0] - iLastX, 2)
		// + (int) Math.pow(iStartXy[1] - iLastY, 2);
		// int iDistanceEndXyLast = (int) Math.pow(iEndXy[0]
		// - iLastX, 2)
		// + (int) Math.pow(iEndXy[1] - iLastY, 2);
		// if (iDistanceStartXyLast < iMaxDistance) {
		// // start of selection nearby the finger
		// bSwift = true;
		// bSwiftFromStart = true;
		// } else if (iDistanceEndXyLast < iMaxDistance) {
		// // end of selection nearby the finger
		// bSwift = true;
		// bSwiftFromStart = false;
		// }
		// }
		// }
		// } else {
		// */ // get selected text. pay attention to start/end exchanged
		//
		// getSelectionOffsets();
		// if (iLastOffsetEnd != -1
		// && iLastOffsetEnd != iLastOffsetStart) {
		// if (iLastOffsetStart < iLastOffsetEnd) {
		// copyToClipboard(mTxt
		// .getText()
		// .subSequence(iLastOffsetStart,
		// iLastOffsetEnd).toString());
		// } else {
		// copyToClipboard(mTxt
		// .getText()
		// .subSequence(iLastOffsetEnd,
		// iLastOffsetStart).toString());
		//
		// }
		// mTxt.setSelection(iLastOffsetEnd);
		// Toast toast = Toast.makeText(
		// mContext,
		// getStringResourceByName("MSG_TXTCOPIED",
		// "Text copied"), Toast.LENGTH_SHORT);
		// toast.show();
		// }
		// resetGlobals();
		// mTxt.setRawInputType(iInputType);
		// }
		// return true;
		// }
		// });

		/*
		 * mTxt.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { if (bSelection == true) {
		 * bSwift = false; bWaitSwift = false; // which of the two side to
		 * slide? the nearest if (Math.abs(iLastOffset - iLastOffsetStart) <
		 * Math .abs(iLastOffset - iLastOffsetEnd)) { iLastOffsetStart =
		 * iLastOffset; }
		 * 
		 * else { iLastOffsetEnd = iLastOffset; }
		 * mTxt.setSelection(iLastOffsetStart, iLastOffsetEnd); } } });
		 */
		/*
		 * swift=true? move start or end, according to swiftfrom, to new
		 * position set lastOffsetStart,lastOffsetEnd select text exit and
		 * consume event
		 * 
		 * exit and do not consume event
		 */

		mTxt.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Layout layout = ((EditText) v).getLayout();
				float x = event.getX() + mTxt.getScrollX()
						- mTxt.getTotalPaddingLeft();
				float y = event.getY() + mTxt.getScrollY()
						- mTxt.getTotalPaddingTop();
				int line = layout.getLineForVertical((int) y);
				iLastOffset = layout.getOffsetForHorizontal(line, x);
				iLastX = (int) x;
				iLastY = (int) y;

				if (bSelection == false)
					return false;// exit and do not consume event

				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (bSwift == true) {
						bSwift = false;
						bWaitSwift = false;
						lExitSwiftTimestamp = System.currentTimeMillis();

						return true;// exit and consume event
					}
					bSwift = false;
					bWaitSwift = false;
					return false;// exit and do not consume event
				}

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					bWaitSwift = false;// default
					getSelectionOffsets();
					int iStartXy[] = getCoordFromOffset(iLastOffsetStart, mTxt);
					int iEndXy[] = getCoordFromOffset(iLastOffsetEnd, mTxt);
					int iDistanceStartXyLast = (int) Math.pow(iStartXy[0]
							- iLastX, 2)
							+ (int) Math.pow(iStartXy[1] - iLastY, 2);
					int iDistanceEndXyLast = (int) Math.pow(iEndXy[0] - iLastX,
							2) + (int) Math.pow(iEndXy[1] - iLastY, 2);
					if (iDistanceStartXyLast < iMaxDistance) {
						// start of selection nearby the finger
						bWaitSwift = true;// wait for swift. not yet a swift
						bSwiftFromStart = true;
					} else if (iDistanceEndXyLast < iMaxDistance) {
						// end of selection nearby the finger
						bWaitSwift = true;// wait for swift. not yet a swift
						bSwiftFromStart = false;
					}

					return false;// do not consume movement
				}
				// during move check for little distance=move the selection
				if (event.getAction() == MotionEvent.ACTION_MOVE) {
					if (bWaitSwift == true) {
						bSwift = true;// it is a real swift. move after down
					}
					if (bSwift == true) {
						// we land here from a onlongclick or from a actiondown
						getSelectionOffsets();
						if (bSwiftFromStart == true) {
							iLastOffsetStart = iLastOffset;
						} else {
							iLastOffsetEnd = iLastOffset;
						}
						mTxt.setSelection(iLastOffsetStart, iLastOffsetEnd);
						return true;// consume movement
					}
				}
				return false;

			}
		});

	}

	private void resetGlobals() {
		bSelection = false;
		bSwift = false;
		bWaitSwift = false;
		iLastOffsetStart = -1;
		iLastOffsetEnd = -1;
		lExitSwiftTimestamp = 0;
	}

	// compute x,y of a given offset
	private int[] getCoordFromOffset(int pos, EditText editText) {
		Layout layout = editText.getLayout();
		int line = layout.getLineForOffset(pos);
		int baseline = layout.getLineBaseline(line);
		int ascent = layout.getLineAscent(line);
		return new int[] { (int) layout.getPrimaryHorizontal(pos),
				(int) baseline + ascent };
	}

	// get offset of selection
	private void getSelectionOffsets() {
		iLastOffsetStart = mTxt.getSelectionStart();
		iLastOffsetEnd = mTxt.getSelectionEnd();

	}

	// get last selected text
	String getLastSelectedText() {
		return sLastSelectedText;
	}

	// copy text to clipboard
	@TargetApi(11)
	private void copyToClipboard(String str) {

		sLastSelectedText = str;

		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mContext
					.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(str);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext
					.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData
					.newPlainText("C&P", str);
			clipboard.setPrimaryClip(clip);
		}
	}

	// copy text to clipboard
	@TargetApi(11)
	private String readFromClipboard() {

		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mContext
					.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard.getText() != null) {
				return clipboard.getText().toString();
			} else {
				return null;
			}
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext
					.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard!=null && clipboard.getPrimaryClip()!=null && clipboard.getPrimaryClip().getItemCount() > 0) {
				return clipboard.getPrimaryClip().getItemAt(0).getText()
						.toString();
			} else {
				return null;
			}
		}
	}

	private String getStringResourceByName(String sStringName,
			String sStringDefault) {
		String packageName = mContext.getPackageName();
		int resId = mContext.getResources().getIdentifier(sStringName,
				"string", packageName);
		if (resId == 0) {
			return sStringDefault;
		} else {
			return mContext.getString(resId);
		}
	}

	private void showPopup() {
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.popup_cut_and_paste, null, false);
		v.setOnKeyListener(this);
		View vItem;
		// selection not active???
		//if (bSelection == false) {
			vItem = v.findViewById(R.id.popup_cap_select);
			vItem.setVisibility(View.VISIBLE);
			vItem.setOnClickListener(this);
		//}

		// selection active???
		if (bSelection == true) {
			vItem = v.findViewById(R.id.popup_cap_copy);
			vItem.setVisibility(View.VISIBLE);
			vItem.setOnClickListener(this);
		}

		// is there something in the c&p buffer???
		if (readFromClipboard() != null) {
			vItem = v.findViewById(R.id.popup_cap_paste);
			vItem.setVisibility(View.VISIBLE);
			vItem.setOnClickListener(this);
		}

		// selection active???
		if (bSelection == true) {
			vItem = v.findViewById(R.id.popup_cap_cut);
			vItem.setVisibility(View.VISIBLE);
			vItem.setOnClickListener(this);
		}

		mPu = new PopupUtility(mContext, v, mTxt);
		mPu.showAsDropDownFromTop(mTxt);//showPopup

	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if(mPu!=null)
		{
		mPu.dismiss();// closePopup
		mPu=null;	
		return true;
		}
		return false;
	}

	private void selectText() {
		bSelection = true;
		bWaitSwift = false;// default
		Editable sTxt = mTxt.getText();
		int iMax = sTxt.length();
		if (iMax > 0) {
			for (iLastOffsetStart = iLastOffset; iLastOffsetStart > 0; iLastOffsetStart--) {
				if ("\n\t\r ".contains(sTxt.subSequence(iLastOffsetStart,
						iLastOffsetStart + 1)) == true) {
					if (iLastOffsetStart < iMax)
						iLastOffsetStart++;
					break;
				}
			}
			for (iLastOffsetEnd = iLastOffset; iLastOffsetEnd < iMax; iLastOffsetEnd++) {
				if ("\n\t\r ".contains(sTxt.subSequence(iLastOffsetEnd,
						iLastOffsetEnd + 1)) == true)
					break;
			}

			String sSelectedWord = "";
			if (iLastOffsetStart < iLastOffsetEnd) {
				sSelectedWord = sTxt.toString()
						.substring(iLastOffsetStart, iLastOffsetEnd)
						.replaceAll("[\n\t\r\\s]", "");
			}
			if (sSelectedWord.length() > 0) {
				mTxt.setSelection(iLastOffsetStart, iLastOffsetEnd);
				mTxt.setRawInputType(InputType.TYPE_NULL);

				int iStartXy[] = getCoordFromOffset(iLastOffsetStart, mTxt);
				int iEndXy[] = getCoordFromOffset(iLastOffsetEnd, mTxt);
				int iDistanceStartXyLast = (int) Math.pow(iStartXy[0] - iLastX,
						2) + (int) Math.pow(iStartXy[1] - iLastY, 2);
				int iDistanceEndXyLast = (int) Math.pow(iEndXy[0] - iLastX, 2)
						+ (int) Math.pow(iEndXy[1] - iLastY, 2);
				if (iDistanceStartXyLast < iMaxDistance) {
					// start of selection nearby the finger
					bSwift = true;
					bSwiftFromStart = true;
				} else if (iDistanceEndXyLast < iMaxDistance) {
					// end of selection nearby the finger
					bSwift = true;
					bSwiftFromStart = false;
				}
			}
		}

	}

	private void copyText() {
		getSelectionOffsets();
		if (iLastOffsetEnd != -1 && iLastOffsetEnd != iLastOffsetStart) {

			int iStart = Math.min(iLastOffsetStart, iLastOffsetEnd);
			int iEnd = Math.max(iLastOffsetStart, iLastOffsetEnd);
 
			// if (iLastOffsetStart < iLastOffsetEnd) {
			copyToClipboard(mTxt.getText().subSequence(iStart, iEnd).toString());
			// } else {
			// copyToClipboard(mTxt
			// .getText()
			// .subSequence(iLastOffsetEnd,
			// iLastOffsetStart).toString());
			//
			// }
			mTxt.setSelection(iLastOffsetEnd);
			// Toast toast = Toast.makeText(
			// mContext,
			// getStringResourceByName("MSG_TXTCOPIED",
			// "Text copied"), Toast.LENGTH_SHORT);
			// toast.show();
		}
		resetGlobals();
		mTxt.setRawInputType(iInputType);

	}

	private void cutText() {
		getSelectionOffsets();
		String sStart = "";
		String sEnd = "";
		if (iLastOffsetEnd != -1 && iLastOffsetEnd != iLastOffsetStart) {
			int iStart = Math.min(iLastOffsetStart, iLastOffsetEnd);
			int iEnd = Math.max(iLastOffsetStart, iLastOffsetEnd);
			String sTxt = mTxt.getText().toString();
			if (iStart > 0)
				sStart = sTxt.substring(0, iStart);
			if (iEnd < sTxt.length())
				sEnd = sTxt.substring(iEnd);
			mTxt.setText(sStart + sEnd);
			mTxt.setSelection(iStart);
			copyToClipboard(sTxt.substring( iStart,iEnd));
		}

		resetGlobals();
		mTxt.setRawInputType(iInputType);

	}

	private void pasteText() {
		String sPaste = readFromClipboard();
		if (sPaste.length() == 0)
			return;// nothing

		String sTxt = mTxt.getText().toString();
		String sStart = "";
		String sEnd = "";

		getSelectionOffsets();
		// is there a selection????
		if (iLastOffsetEnd != -1 && iLastOffsetEnd != iLastOffsetStart) {
			int iStart = Math.min(iLastOffsetStart, iLastOffsetEnd);
			int iEnd = Math.max(iLastOffsetStart, iLastOffsetEnd);
			if (iStart > 0)
				sStart = sTxt.substring(0, iStart);
			if (iEnd < sTxt.length())
				sEnd = sTxt.substring(iEnd);
		} else {
			int iStart = mTxt.getSelectionStart();
			if (iStart >= 0) {
				if (iStart > 0)
					sStart = sTxt.substring(0, iStart);
				if (iStart < sTxt.length())
					sEnd = sTxt.substring(iStart);

			}
		}

		mTxt.setText(sStart + sPaste + sEnd);
		mTxt.setSelection((sStart + sPaste).length());

		resetGlobals();
		mTxt.setRawInputType(iInputType);

	}

	@Override
	public void onClick(View v) {

		mPu.dismiss();// closePopup

		switch (v.getId()) {
		case R.id.popup_cap_select:
			selectText();
			break;
		case R.id.popup_cap_copy:
			copyText();
			break;
		case R.id.popup_cap_paste:
			pasteText();
			break;
		case R.id.popup_cap_cut:
			cutText();
			break;
		}

	}

}
