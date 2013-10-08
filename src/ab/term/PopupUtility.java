package ab.term;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class PopupUtility extends PopupWindow {

	private View mViewPopupContent = null;
	private View mViewRelativeTo = null;
	private Context mContext = null;
	public PopupWindow mPw = null;

	public PopupUtility(Context ctx, View vPopupContent, View vRelativeTo) {
		super(vPopupContent, WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT, true);
		mViewPopupContent = vPopupContent;
		mViewRelativeTo = vRelativeTo;
		mContext = ctx;

		mPw = this;//new PopupWindow(vPopupContent, WindowManager.LayoutParams.WRAP_CONTENT,
				//WindowManager.LayoutParams.WRAP_CONTENT, true);
 
	}

 

	@Override
	public void showAsDropDown(View viewRelativeTo)
	{
		mViewRelativeTo=viewRelativeTo;
		showPopup(false);
	}
	
	public void showAsDropDownFromTop(View viewRelativeTo)
	{
		mViewRelativeTo=viewRelativeTo;
		showPopup(true);
	}	
	
	
 
		
	public void showPopup(boolean bAlignToTop) {
		// show works using x,y of the root, not of the mTxt
		int iXrel = getRelativeLeft(mViewRelativeTo);
		int iYrel =getRelativeTop(mViewRelativeTo);
		if(bAlignToTop==false){
		 iYrel= iYrel+mViewRelativeTo.getHeight();
		}
		
		// but i don't want to exit from the top or from the left
		int iRootCoords[] = new int[2];
		mViewRelativeTo.getRootView().getLocationOnScreen(iRootCoords);

		/*
		 * _____|_______ | __|____ | | | | |__|____ -10 5
		 */
		if ((iRootCoords[0] + iXrel) < 0) {
			iXrel = -iRootCoords[0];
		}
		if ((iRootCoords[1] + iYrel) < 0) {
			iYrel = -iRootCoords[1];
		}
		// but i don't want the box to go beyond right or bottom margin
		// of the screen
		int iScreenDimensions[] = new int[2];
		getDisplaySize(iScreenDimensions);

		mViewPopupContent.measure(View.MeasureSpec.UNSPECIFIED,
				View.MeasureSpec.UNSPECIFIED);
		int iXsize = mViewPopupContent.getMeasuredWidth();
		int iYsize = mViewPopupContent.getMeasuredHeight();

		if (iRootCoords[0] + iXrel + iXsize > iScreenDimensions[0]) {
			iXrel = iXrel
					- (iRootCoords[0] + iXrel + iXsize - iScreenDimensions[0]);
		}
		if (iRootCoords[1] + iYrel + iYsize > iScreenDimensions[1]) {
			iYrel = iYrel
					- (iRootCoords[1] + iYrel + iYsize - iScreenDimensions[1]);
		}

		mPw.showAtLocation(mViewRelativeTo, Gravity.NO_GRAVITY, iXrel, iYrel);
	}

	private int getRelativeLeft(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft()
					+ getRelativeLeft((View) myView.getParent());
	}

	private int getRelativeTop(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + getRelativeTop((View) myView.getParent());
	}

	private void getDisplaySize(int[] dimensions) {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getMetrics(displaymetrics);
		dimensions[1] = displaymetrics.heightPixels;
		dimensions[0] = displaymetrics.widthPixels;
	}

}
