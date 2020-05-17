package ru.parallelbooks.aglonareader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ParallelTextView extends View {
	ParallelTextData pTD;
	private final MainActivity mainActivity;
	private float splitterMoveOffset;
	private final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private float screenDensityX;
	private float screenDensityY;
	private float screenWidth;
	private float screenHeight;
	private final int STATE_IDLE = 0;
	private final int STATE_POINTERDOWN = 1;
	private final int STATE_DRAGGING_SPLITTER = 3;
	private final int STATE_DRAGGING_PAGE = 4;
	private final int STATE_PAGEDRAGFINISHING = 5;
	private final int STATE_PAGEDRAGREVERTING = 6;
	private final int STATE_SCALING = 7;
	private final int STATE_BRIGHTNESS_CHANGE = 8;
	private final int STATE_SCROLL = 9;
	private int state = STATE_IDLE;
	private int firstDownPointerId;
	private int secondDownPointerId;
	private float pointerDownPositionX;
	private float pointerDownPositionY;
	private float scrollY;
	private float lastBrightY;
	private float initialScalingDistance;
	private int initialFontProportion;
	private double initialBrightness;
	private long lastSingleTapTime;
	private final LongTapTimer longTapTimer = new LongTapTimer();
	private final int LONG_TAP_DELAY = 500;
	// Bitmaps are made static to avoid memory leaks on Android 2.3.3 and lower
	private static Bitmap currentPageBitmap;
	private Canvas currentPageBitmapCanvas;
	private static Bitmap nextPageBitmap;
	private Canvas nextPageBitmapCanvas;
	private float initialFloatingPagePosition;
	private float floatingPagePosition;
	private boolean switchingToNextPage;
	private static Bitmap shadowBitmap;
	private float shadowWidth;
	private final RectF shadowRect = new RectF();
	private boolean suppressParallelTextRedrawing = false;
	private long pageAnimationStartTime;
	private int opState;

	public ParallelTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mainActivity = (MainActivity) context;
		if (shadowBitmap == null) {
			shadowBitmap = Bitmap.createBitmap(50, 1, Bitmap.Config.ARGB_8888);
			Canvas shadowBitmapCanvas = new Canvas(shadowBitmap);
			Paint shadowPaint = new Paint();
			for (int i = 0; i < 50; ++i) {
				shadowPaint.setARGB(200 - i * 4, 0, 0, 0);
				shadowBitmapCanvas.drawPoint((float) i, 0.0f, shadowPaint);
			}
		}
	}

	public void goToNextPage() {
		if (state == STATE_IDLE) {
			prepareSwitchToNextPage();
			setState(STATE_PAGEDRAGFINISHING);
			startPageAnimation(initialFloatingPagePosition);
		}
	}

	public void goToPreviousPage() {
		if (state == STATE_IDLE) {
			prepareSwitchToPrevPage();
			setState(STATE_PAGEDRAGREVERTING);
			startPageAnimation(initialFloatingPagePosition);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int pointerIndex = event.getActionIndex();
		final int pointerId = event.getPointerId(pointerIndex);
		final float x = event.getX(pointerIndex);
		final float y = event.getY(pointerIndex);
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				switch (state) {
					case STATE_IDLE: // A finger is down, start timer for options popup:
						setState(STATE_POINTERDOWN);
						longTapTimer.start();
						pointerDownPositionX = x;
						pointerDownPositionY = y;
						firstDownPointerId = pointerId;
						return true;
					case STATE_POINTERDOWN: // A second finger is down, so scale:
						longTapTimer.cancel();
						if (pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced) {
							pTD.TurnAdvancedPopupOff();
						}
						setState(STATE_SCALING);
						secondDownPointerId = pointerId;
						initialScalingDistance = getDistance(x, y, pointerDownPositionX, pointerDownPositionY);
						initialFontProportion = pTD.fontProportion;
						return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				switch (state) {
					case STATE_POINTERDOWN:
						float MIN_MOVE_DELTA_INCH = 0.1f;
						if (getHeightDiffInches(pointerDownPositionY, y) > MIN_MOVE_DELTA_INCH) { // vertical movement
							if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal
									&& opState == 0 && pTD.XonSplitter(pointerDownPositionX, screenDensityX)) {
								setState(STATE_BRIGHTNESS_CHANGE);
								initialBrightness = pTD.brightness;
								lastBrightY = y;
								// Scroll up/down a line if finger moved at least that far:
							} else {
								setState(STATE_SCROLL);
								scrollY = pointerDownPositionY; // Track finger movement
							}
						} else if (getWidthDiffInches(pointerDownPositionX, x) > MIN_MOVE_DELTA_INCH) { // horizontal movement
							if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal
									&& opState == 0 && pTD.XonSplitter(pointerDownPositionX, screenDensityX)) {
								opState = 1;
								splitterMoveOffset = pointerDownPositionX - pTD.splitterPosition;
								setState(STATE_DRAGGING_SPLITTER);
							} else {
								setState(STATE_DRAGGING_PAGE);
								if (x > pointerDownPositionX) { // going to prev page
									prepareSwitchToPrevPage();
								} else { // next page
									prepareSwitchToNextPage();
								}
							}
						}
						return true;
					case STATE_SCALING:
						float fx = 0, fy = 0;
						boolean firstPointFound = false;
						for (int pi = 0; pi < event.getPointerCount(); ++pi) {
							int pid = event.getPointerId(pi);
							if (pid == firstDownPointerId || pid == secondDownPointerId) {
								if (firstPointFound) {
									final float distance = getDistance(fx, fy, event.getX(pi), event.getY(pi));
									final float factor = distance / initialScalingDistance;
									pTD.fontProportion = (int) (initialFontProportion * factor);
									if (pTD.fontProportion < pTD.fontSizeMin) {
										pTD.fontProportion = (int) pTD.fontSizeMin;
									} else if (pTD.fontProportion > 1000) {
										pTD.fontProportion = 1000;
									}
									pTD.setFontSize(true);
									break;
								} else {
									fx = event.getX(pi);
									fy = event.getY(pi);
									firstPointFound = true;
								}
							}
						}
						return true;
					case STATE_DRAGGING_SPLITTER:
						if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal && opState == 1) {
							float newSplitterPosition = x - splitterMoveOffset;
							if (newSplitterPosition != pTD.splitterPosition) {
								if (MainActivity.DoSoundEffects) {
									// Indicate splitter drag with sound effects.
									// Calculate rate (pitch):
									float SERate = (float) (((newSplitterPosition / screenWidth) * 1.5) + .5);
									// Use +/- 3 to avoid flood of sounds:
									if (newSplitterPosition < pTD.splitterPosition - 3)
										// For now we use 1 effect for left/right, this code allows 2 if desired:
										mainActivity.SoundEffect(6, .2f, .2f, SERate);
									else if (newSplitterPosition > pTD.splitterPosition + 3)
										mainActivity.SoundEffect(6, .2f, .2f, SERate);
								}
								pTD.setSplitterPosition(newSplitterPosition);
								pTD.SetSplitterRatioByPosition();
								pTD.ProcessLayoutChange(false);
								invalidateParallelText();
							}
						}
						return true;
					case STATE_DRAGGING_PAGE:
						final float deltaX = x - pointerDownPositionX;
						floatingPagePosition = initialFloatingPagePosition + deltaX;
						if (floatingPagePosition > 0.0f) {
							floatingPagePosition = 0.0f;
						}
						invalidate();
						break;
					case STATE_SCROLL:
						if (Math.abs(scrollY - y) > pTD.lineHeight) { // large vertical movement
							// Turn off TTS and sentence outline:
							pTD.TurnAdvancedPopupOff();
							if (y < scrollY)
								pTD.ProcessKeyDown(); // scroll text up (move forwards in text)
							else
								pTD.ProcessKeyUp();   // scroll text down
							if (MainActivity.DoSoundEffects) mainActivity.SoundEffect(2, .2f, .2f, 1);
							scrollY = y;
							invalidateParallelText();
						}
						break;
					case STATE_BRIGHTNESS_CHANGE:
//						final double delta = (y - pointerDownPositionY) / screenHeight * 2.0;
						// "up" means "brighter" [crw]:
						final double delta = (pointerDownPositionY - y) / screenHeight * 2.0;
						if (Math.abs(lastBrightY - y) > 5) {  // Lower input sensitivity
							pTD.brightness = initialBrightness + delta;
							if (pTD.brightness < 0.5) {
								pTD.brightness = 0.5f;
							} else if (pTD.brightness > 1.0) {
								pTD.brightness = 1.0;
							}
							if (MainActivity.DoSoundEffects)
								//	Sound effect rises/falls with brightness setting:
								mainActivity.SoundEffect(5, .2f, .2f, (float) pTD.brightness);
							pTD.SetColorsByBrightness();
							invalidateParallelText();
						}
						lastBrightY = y;
						break;
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				switch (state) {
					case STATE_SCALING:
						if (pointerId == firstDownPointerId) {
							firstDownPointerId = -1;
						} else if (pointerId == secondDownPointerId) {
							secondDownPointerId = -1;
						}
						if (firstDownPointerId == -1 && secondDownPointerId == -1) {
							setState(STATE_IDLE);
						}
						return true;
					case STATE_POINTERDOWN:
						final long time = System.currentTimeMillis();
						setState(STATE_IDLE);
						int DOUBLE_TAP_DELAY = 500;
						if (time - lastSingleTapTime < DOUBLE_TAP_DELAY) {
							lastSingleTapTime = 0;
						} else {
							processSingleTap(x, y);
							lastSingleTapTime = System.currentTimeMillis();
						}
						return true;
					case STATE_DRAGGING_SPLITTER:
						if (pTD.LayoutMode == ParallelTextData.LayoutMode_Normal && opState == 1) {
							opState = 0;
						}
						setState(STATE_IDLE);
						return true;
					case STATE_BRIGHTNESS_CHANGE:
					case STATE_SCROLL:
						setState(STATE_IDLE);
						return true;
					case STATE_DRAGGING_PAGE:
						setState(switchingToNextPage ? STATE_PAGEDRAGFINISHING : STATE_PAGEDRAGREVERTING);
						if (MainActivity.DoSoundEffects) mainActivity.SoundEffect(3, .8f, .8f, 1);
						startPageAnimation(floatingPagePosition);
						return true;
				}
				break;
		}
		return false;
	}

	private void prepareSwitchToNextPage() {
		switchingToNextPage = true;
		suppressParallelTextRedrawing = true;
		if (pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced) {
			pTD.TurnAdvancedPopupOff();
		}
		pTD.ProcessPageDown();
		suppressParallelTextRedrawing = false;
		nextPageBitmapCanvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.SRC);
		pTD.onDraw(nextPageBitmapCanvas);
		initialFloatingPagePosition = 0;
	}

	private void prepareSwitchToPrevPage() {
		switchingToNextPage = false;
		suppressParallelTextRedrawing = true;
		if (pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced) {
			pTD.TurnAdvancedPopupOff();
		}
		pTD.ProcessPageUp();
		suppressParallelTextRedrawing = false;
		nextPageBitmapCanvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.SRC);
		pTD.onDraw(nextPageBitmapCanvas);
		swapBitmaps();
		initialFloatingPagePosition = -screenWidth - shadowWidth;
	}

	private void swapBitmaps() {
		final Bitmap tempBitmap = nextPageBitmap;
		nextPageBitmap = currentPageBitmap;
		currentPageBitmap = tempBitmap;
		final Canvas tempCanvas = nextPageBitmapCanvas;
		nextPageBitmapCanvas = currentPageBitmapCanvas;
		currentPageBitmapCanvas = tempCanvas;
	}

	private void startPageAnimation(float ininitalPagePosition) {
		initialFloatingPagePosition = ininitalPagePosition;
		pageAnimationStartTime = System.currentTimeMillis();
		invalidate();
	}

	private void setState(int state) {
		Log.d("ParalleltextView", "State change: " + this.state + " -> " + state);
		this.state = state;
	}

	private void processSingleTap(float x, float y) {
		pTD.LastMouseX = x;
		pTD.LastMouseY = y;
//controls advancedpopup [crw]
		if (pTD.LayoutMode == ParallelTextData.LayoutMode_Advanced) {
			pTD.ProcessMousePosition();
		}
	}

	private void processLongTap() {
		mainActivity.openContextMenu(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (pTD == null)
			return;
		if (currentPageBitmap != null) {
			currentPageBitmap.recycle();
		}
		if (nextPageBitmap != null) {
			nextPageBitmap.recycle();
		}
		currentPageBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		currentPageBitmapCanvas = new Canvas(currentPageBitmap);
		nextPageBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		nextPageBitmapCanvas = new Canvas(nextPageBitmap);
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
		screenWidth = w;
		screenHeight = h;
		shadowWidth = screenWidth / 25.0f;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (state == STATE_PAGEDRAGFINISHING || state == STATE_PAGEDRAGREVERTING) // animating
		{
			final float finalPagePos = state == STATE_PAGEDRAGFINISHING ? (-screenWidth - shadowWidth) : 0.0f;
			long PAGE_ANIMATION_DURATION = 300;
			final float coef = (System.currentTimeMillis() - pageAnimationStartTime) / (float) PAGE_ANIMATION_DURATION;
			if (coef >= 1.0f) {
				floatingPagePosition = 0.0f;
				if (state == STATE_PAGEDRAGFINISHING) {
					swapBitmaps();
				}
				setState(STATE_IDLE);
			} else {
				final float transformedCoef = ((float) Math.sin(coef * Math.PI - Math.PI / 2.0) + 1.0f) / 2.0f; // transform by sine
				floatingPagePosition = initialFloatingPagePosition + (finalPagePos - initialFloatingPagePosition) * transformedCoef;
				invalidate();
			}
		}
		if (state == STATE_DRAGGING_PAGE || state == STATE_PAGEDRAGFINISHING || state == STATE_PAGEDRAGREVERTING) {
			canvas.drawBitmap(nextPageBitmap, 0, 0, null);
			final float shadowLeft = floatingPagePosition + screenWidth;
			final float shadowRight = shadowLeft + shadowWidth;
			shadowRect.set(shadowLeft, 0.0f, shadowRight, screenHeight);
			canvas.drawBitmap(shadowBitmap, null, shadowRect, null);
		}
		canvas.drawBitmap(currentPageBitmap, floatingPagePosition, 0, null);
	}

	public void invalidateParallelText() {
		if (suppressParallelTextRedrawing) {
			return;
		}
		if (pTD != null) {
			currentPageBitmapCanvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.SRC);
			pTD.onDraw(currentPageBitmapCanvas);
		}
		invalidate();
	}

	public void updateNoBookVisibility() {
		MainActivity mActivity = (MainActivity) getContext();
		mActivity.findViewById(R.id.emptyBookView).setVisibility(pTD.bookOpened ? View.INVISIBLE : View.VISIBLE);
	}

	private float getHeightDiffInches(float y1, float y2) {
		return Math.abs(y1 - y2) / screenDensityY;
	}

	private float getWidthDiffInches(float x1, float x2) {
		return Math.abs(x1 - x2) / screenDensityX;
	}

	private static float getDistance(float x1, float y1, float x2, float y2) {
		final float dX = x1 - x2;
		final float dY = y1 - y2;
		return (float) Math.sqrt(dX * dX + dY * dY);
	}

	private class LongTapTimer extends CountDownTimer {
		public LongTapTimer() {
			super(LONG_TAP_DELAY, 1000000);
		}

		@Override
		public void onTick(long l) {
		}

		@Override
		public void onFinish() {
			mainActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (state == STATE_POINTERDOWN) {
						setState(STATE_IDLE);
						processLongTap();
					}
				}
			});
		}
	}
}
