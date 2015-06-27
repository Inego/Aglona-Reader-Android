package ru.parallelbooks.aglonareader;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class ParallelTextView extends View {

	ParallelTextData pTD;

	public float splitterMoveOffset;

	private float startX;

	private float startY;

	private float screenDensityX;

	private float screenDensityY;

	// / in inches
	private double distance(float dX, float dY) {

		return Math.sqrt(dX * dX + dY * dY);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		MainActivity mainActivity = (MainActivity) getContext();

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:

			if (mainActivity.seekBarMode != 0) {
				mainActivity.setSeekBarMode(MainActivity.SEEKBAR_MODE_OFF);
				break;
			}
			
			if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal) {
				if (pTD.opState == 1) {
					pTD.opState = 0;
					break;
				}
			}

			pTD.LastMouseX = startX;
			pTD.LastMouseY = startY;

			float dstX = Math.abs((event.getX() - startX) / screenDensityX);

			float dstY = Math.abs((event.getY() - startY) / screenDensityY);

			double dst = distance(dstX, dstY);

			if (dst < ParallelTextData.FINGERTIP
					&& pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced) {

				pTD.ProcessMousePosition(true, true);
				break;

			} else {

				

				// Let's determine, which is MOAR
				// x distance or y distance.
				// and additional requirement is that the bigger to the smaller
				// has to be at least two times

				if (pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced)
					pTD.TurnAdvancedPopupOff();

			}

			ProcessNavigation(event);

			break;

		case MotionEvent.ACTION_DOWN:

			if (mainActivity.seekBarMode != 0)
				break;

			if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal
					&& pTD.opState == 0) {

				float eventX = event.getX();

				if (pTD.XonSplitter(eventX, screenDensityX)) {
					pTD.opState = 1;
					splitterMoveOffset = eventX - pTD.splitterPosition;
					break;
				}
			}

			startX = event.getX();
			startY = event.getY();

			break;

		case MotionEvent.ACTION_MOVE:

			if (mainActivity.seekBarMode != 0)
				break;

			if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal
					&& pTD.opState == 1) {
				// Move splitter

				float eventX = event.getX();

				float newSplitterPosition = eventX - splitterMoveOffset;

				if (newSplitterPosition != pTD.splitterPosition) {

					pTD.setSplitterPosition(newSplitterPosition);
					pTD.SetSplitterRatioByPosition();
					pTD.ProcessLayoutChange(false);
					invalidate();
				}
			}

		}

		return true;
	}

	private void ProcessNavigation(MotionEvent event) {
		
		if (pTD.Number() == 0)
			return;
		
		float part = pTD.LastMouseY / pTD.viewHeight;
		
		

		if (part >= 0.75f)
			pTD.ProcessPageDown();
		else if (part >= 0.5f)
			pTD.ProcessKeyDown();
		else if (part >= 0.25f)
			pTD.ProcessKeyUp();
		else
			pTD.ProcessPageUp();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (pTD == null)
			return;
		
		if (!pTD.fontRangeSet) {
			pTD.setFontRange(w, h);
			pTD.setFontSize(false);
		}
		
		pTD.viewWidth = w;
		pTD.viewHeight = h;
		pTD.pTV = this;
		pTD.SetSplitterPositionByRatio();
		pTD.ProcessLayoutChange(true);

		screenDensityX = getContext().getResources().getDisplayMetrics().xdpi;
		screenDensityY = getContext().getResources().getDisplayMetrics().ydpi;

		super.onSizeChanged(w, h, oldw, oldh);
	}

	public ParallelTextView(Context context, AttributeSet attrs) {

		super(context, attrs);

	}

	@Override
	protected void onDraw(Canvas canvas) {

		super.onDraw(canvas);

		if (pTD != null) {
			pTD.onDraw(canvas);
		}

	}

	public void updateNoBookVisibility() {
		
		MainActivity mActivity = (MainActivity) getContext();
		
		((TextView)mActivity.findViewById(R.id.emptyBookView)).setVisibility(pTD.bookOpened ? View.INVISIBLE : View.VISIBLE);
	}

	

	

}
