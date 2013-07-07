package ru.parallelbooks.aglonareader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

public class ParallelTextData {

	// Singleton
	private static ParallelTextData instance;

	public static ParallelTextData getInstance() {
		if (instance == null)
			instance = new ParallelTextData();

		return instance;
	}

	public static boolean instanceExists() {
		return (instance != null);
	}

	public static void dereferenceInstance() {
		instance = null;
	}

	private int Advanced_HighlightedPair;
	public Frame AdvancedHighlightFrame;
	public PopUpInfo popUpInfo;
	int popUpColor;
	int popUpTextColor;
	
	private float indentLength;

	public final static int LayoutMode_Normal = 0;
	public final static int LayoutMode_Alternating = 1;
	public final static int LayoutMode_Advanced = 2;

	public static int popUpOffsetX = 7;
	public static int popUpOffsetY = 7;

	public int opState;

	public int LayoutMode;

	public static final float FINGERTIP = 0.1f;

	public int FirstRenderedPair;
	public int LastRenderedPair;

	public Canvas canvas;

	public ParallelText pText;

	// Had to be sorted, but there are no SortedLists in Java
	private TreeMap<Integer, ArrayList<ScreenWord>> wordsOnScreen;

	public int CurrentPair;

	public int PanelMargin;

	public float splitterPosition;

	private int splitterWidth;

	public boolean reversed;

	public int NumberOfScreenLines;

	public boolean HighlightFirstWords;
	public boolean HighlightFragments;

	// Contains H values of text color table
	ArrayList<Double> colorTableH;
	ArrayList<Integer> darkColorTable;
	ArrayList<Integer> lightColorTable;

	private float leftWidth;

	private float rightWidth;

	public float rightPosition;

	byte NumberofColors;

	public int viewWidth;

	public Paint paint;

	public int viewHeight;

	private int vMargin;

	public Float lineHeight;

	// 0 = not set; 1 = black; 2 = gray
	int currentTextColor;

	public int LastFullScreenLine;

	private float textVOffset;

	private ArrayList<AbstractFrame> frames;

	private float text1start;
	private float text1end;

	private float text2start;
	private float text2end;

	// The color of the transparent-black rectangle upon which the
	// "finished percent" is shown
	int transparentBlack = Color.argb(100, 0, 0, 0);

	float textFontSize;

	private Frame selectionFrame;
	private byte selectionSide;

	private int Selection1Pair;

	private int Selection2Pair;

	private int Selection1Position;

	private int Selection2Position;

	private byte frameoffset_x = 5;
	private int frameoffset_y = 2;

	private HashMap<String, Float> widthDictionary;

	public Float SpaceLength;

	public float SplitterRatio;

	public double brightness;

	public float LastMouseX;
	public float LastMouseY;

	public ParallelTextView pTV;
	private ScreenWord mouse_text_word;
	
	public ArrayList<FileUsageInfo> fileUsageInfo;
	
	public boolean fontRangeSet;
	public float fontSizeMin;
	public float fontSizeMax;
	public int fontProportion; // int between 0 and 1000, where 0 is fontSizeMin and 1000 is fontSizeMax
	
	public boolean bookOpened = false;
	
	
	
	class ProcessArgs {

		public byte side;
		public StringBuilder word;
		public float occLength;
		public ArrayList<CommonWordInfo> words;
		public TextPair p;
		public float MaxWidth;
		public int wordPosition;
		public int height;
		private int requiredHeight;

		public ProcessArgs(byte side) {

			this.side = side;

			occLength = 0;

			words = new ArrayList<CommonWordInfo>();

		}

		public void ProcessCurrentWord(boolean eastern) {

			String wordString = word.toString();

			// Current Word complete, let's get its length
			Float wordLength = WordWidth(wordString);

			float newStart = occLength
					+ (occLength == 0 || eastern && words.size() > 0
							&& words.get(words.size() - 1).eastern ? 0
							: SpaceLength);

			if (newStart + wordLength > MaxWidth && occLength != 0) {
				// Move this Word to the Next Line.
				// Before that we need to flush words to the DB

				ParallelText.InsertWords(words, MaxWidth - occLength);

				height++;

				newStart = 0;

				occLength = 0;

			}

			// Add this Word to the current Line
			words.add(new CommonWordInfo(p, wordString, height, newStart,
					newStart + wordLength - 1, wordPosition, eastern, side));
			occLength = newStart + wordLength;

			word.setLength(0);
		}

		public void ProcessTextFromPair() {

			if ((side == (byte) 1) ? p.allLinesComputed1 : p.allLinesComputed2)
				return;

			int pos;
			int wordPos;

			if (height == -1) {
				pos = 0;
				height = 0;
				if (side == 1)
					p.ContinueFromNewLine1 = false;
				else
					p.ContinueFromNewLine2 = false;
			} else {
				if (side == 1) {
					pos = p.currentPos1;
					if (p.ContinueFromNewLine1) {
						occLength = indentLength;
						p.ContinueFromNewLine1 = false;
					}

				} else {
					pos = p.currentPos2;
					if (p.ContinueFromNewLine2) {
						occLength = indentLength;
						p.ContinueFromNewLine2 = false;
					}
				}
			}

			wordPos = -1;

			char c;

			String text = (side == (byte) 1 ? p.text1 : p.text2);

			word = new StringBuilder();

			int textLength = text.length();

			while (pos < textLength) {

				c = text.charAt(pos);

				if (c == ' ' || c == '\t' || c == '\r') {

					if (word.length() == 0) {
						pos++;
						continue;
					}

					ProcessCurrentWord(false);

					if (requiredHeight != -1 && requiredHeight == height) {
						if (side == (byte) 1)
							p.currentPos1 = wordPos;
						else
							p.currentPos2 = wordPos;
						return;
					}

					wordPos = -1;

				} else if (c == '\n') {
					if (word.length() > 0) {
						ProcessCurrentWord(false);
						if (requiredHeight != -1 && requiredHeight == height) {
							wordPos = pos;
							{
								if (side == (byte) 1)
									p.currentPos1 = wordPos;
								else
									p.currentPos2 = wordPos;
								return;
							}
						}
						wordPos = -1;
					}

					ParallelText.InsertWords(words, 0);

					height++;
					occLength = indentLength;

					if (requiredHeight != -1 && requiredHeight == height) {
						wordPos = ++pos;
						{
							if (side == (byte) 1) {
								p.ContinueFromNewLine1 = true;
								p.currentPos1 = wordPos;
							} else {
								p.ContinueFromNewLine2 = true;
								p.currentPos2 = wordPos;
							}
							return;
						}
					}

				}

				else if (IsEasternCharacter(c)) {
					if (word.length() != 0) {
						ProcessCurrentWord(false);
						if (requiredHeight != -1 && requiredHeight == height) {
							if (side == (byte) 1)
								p.currentPos1 = wordPos;
							else
								p.currentPos2 = wordPos;
							return;
						}
					}

					word.append(c);

					ProcessCurrentWord(true);

					if (requiredHeight != -1 && requiredHeight == height) {
						wordPos = pos;
						{
							if (side == (byte) 1)
								p.currentPos1 = wordPos;
							else
								p.currentPos2 = wordPos;
							return;
						}
					}

					wordPos = -1;

				}

				else {
					if (wordPos == -1)
						wordPos = pos;

					word.append(c);
				}

				pos++;

			}

			// Reached the end, process current Word (if there is any)
			if (word.length() > 0) {
				ProcessCurrentWord(false);
				if (requiredHeight != -1 && requiredHeight == height) {
					if (side == (byte) 1)
						p.currentPos1 = wordPos;
					else
						p.currentPos2 = wordPos;
					return;
				}
			}

			if (side == (byte) 1)
				p.allLinesComputed1 = true;
			else
				p.allLinesComputed2 = true;

			return;

		}

		boolean NeedToLineBreakFirstWord(boolean startParagraph) {
			if (occLength == 0.0f)
				return false;
			if (startParagraph)
				return true;

			return (MaxWidth - occLength - SpaceLength <= WordWidth(GetWord(p,
					side, 0)));

		}

	}

	void DrawBackground(byte side, int line1, float x1, int line2, float x2b,
			int color) {

		float textstart;
		float textend;
		float width;
		int oY; // vertical offset
		int mX; // Background's own X margin (used only in Advanced mode for
				// popup background)

		if (LayoutMode == LayoutMode_Advanced) {
			textstart = PanelMargin;
			textend = viewWidth - PanelMargin;
			width = viewWidth - 2 * PanelMargin;
			oY = popUpInfo.offsetY * popUpOffsetY;
			mX = 5;
		} else if (side == 1 && !reversed || side == 2 && reversed) {
			textstart = PanelMargin;
			textend = leftWidth - PanelMargin;
			width = leftWidth - 2 * PanelMargin;
			oY = 0;
			mX = 0;
		} else {
			textstart = splitterPosition + splitterWidth + PanelMargin;
			textend = viewWidth - PanelMargin;
			width = rightWidth - 2 * PanelMargin;
			oY = 0;
			mX = 0;
		}

		paint.setColor(color);
		paint.setStyle(Style.FILL);

		// paint.setShader(shader)

		if (line1 == line2)
			if (line1 == -1) {
				canvas.drawRect(textstart - mX, oY, textstart + width + mX,
						viewHeight + oY, paint);
			} else
				// A piece of text
				canvas.drawRect(textstart + x1 - mX, vMargin + line1
						* lineHeight + oY, textstart + x2b + mX, vMargin
						+ (line1 + 1) * lineHeight + oY, paint);

		else if (line1 == -1) {
			Path path = new Path();

			path.moveTo(textstart - mX, oY);
			path.lineTo(textstart - mX, vMargin + (line2 + 1) * lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + (line2 + 1)
					* lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + line2 * lineHeight + oY);
			path.lineTo(textend + mX, vMargin + line2 * lineHeight + oY);
			path.lineTo(textend + mX, oY);

			canvas.drawPath(path, paint);
		}

		else if (line2 == -1)
			if (x1 == 0) // Top starts at cursorX = 0
			{
				Path path = new Path();
				path.moveTo(textstart - mX, viewHeight - 1 + oY);
				path.lineTo(textstart - mX, vMargin + line1 * lineHeight + oY);
				path.lineTo(textend + mX, vMargin + line1 * lineHeight + oY);
				path.lineTo(textend + mX, viewHeight - 1 + oY);
				canvas.drawPath(path, paint);
			} else {
				Path path = new Path();
				path.moveTo(textstart - mX, viewHeight - 1 + oY);
				path.lineTo(textstart - mX, vMargin + (line1 + 1) * lineHeight
						+ oY);
				path.lineTo(textstart + x1 - mX, vMargin + (line1 + 1)
						* lineHeight + oY);
				path.lineTo(textstart + x1 - mX, vMargin + line1 * lineHeight
						+ oY);
				path.lineTo(textend + mX, vMargin + line1 * lineHeight + oY);
				path.lineTo(textend + mX, viewHeight - 1 + oY);
				canvas.drawPath(path, paint);
			}

		else if (x1 == 0) {
			Path path = new Path();
			path.moveTo(textend + mX, vMargin + line1 * lineHeight + oY);
			path.lineTo(textstart - mX, vMargin + line1 * lineHeight + oY);
			path.lineTo(textstart - mX, vMargin + (line2 + 1) * lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + (line2 + 1)
					* lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + line2 * lineHeight + oY);
			path.lineTo(textend + mX, vMargin + line2 * lineHeight + oY);
			canvas.drawPath(path, paint);
		} else {
			Path path = new Path();
			path.moveTo(textend + mX, vMargin + line1 * lineHeight + oY);
			path.lineTo(textstart + x1 - mX, vMargin + line1 * lineHeight + oY);
			path.lineTo(textstart + x1 - mX, vMargin + (line1 + 1) * lineHeight
					+ oY);
			path.lineTo(textstart - mX, vMargin + (line1 + 1) * lineHeight + oY);
			path.lineTo(textstart - mX, vMargin + (line2 + 1) * lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + (line2 + 1)
					* lineHeight + oY);
			path.lineTo(textstart + x2b + mX, vMargin + (line2) * lineHeight
					+ oY);
			path.lineTo(textend + mX, vMargin + (line2) * lineHeight + oY);
			canvas.drawPath(path, paint);
		}
	}

	private void RenderBackground(int pairIndex, int cLine, LayoutVars lv) {

		TextPair p = pText.get(pairIndex);

		ArrayList<WordInfo> list = p.ComputedWords(lv.textSide);

		RenderedTextInfo renderedInfo = p.RenderedInfo(lv.textSide);

		if (cLine >= NumberOfScreenLines || list == null || list.size() == 0) {
			renderedInfo.valid = false;
			return;
		}

		WordInfo first = list.get(0);

		WordInfo last = list.get(list.size() - 1);

		if (cLine + last.line < 0) {
			renderedInfo.valid = false;
			return;
		}

		renderedInfo.valid = true;

		if (cLine < 0)
			renderedInfo.line1 = -1;
		else {
			renderedInfo.line1 = cLine + first.line;
			renderedInfo.x1 = first.x1;
		}

		if (cLine + last.line >= NumberOfScreenLines
				|| !(lv.textSide == (byte) 1 ? p.allLinesComputed1
						: p.allLinesComputed2))
			renderedInfo.line2 = -1;
		else {
			renderedInfo.line2 = cLine + last.line;
			renderedInfo.x2 = last.x2;
			renderedInfo.x2B = last.x2;

			ArrayList<WordInfo> nextList = null;
			TextPair nextPair = null;

			if (LayoutMode == LayoutMode_Alternating) {

				if (reversed == (lv.textSide == 1))
					nextPair = p;
				else if (pairIndex < pText.Number() - 1
						&& last.line == p.height)
					nextPair = pText.get(pairIndex + 1);

				if (nextPair != null)
					nextList = nextPair.ComputedWords((byte) (3 - lv.textSide));

			} else {
				// NORMAL and ADVANCED mode
				if (pairIndex < pText.Number() - 1 && last.line == p.height) {
					nextPair = pText.get(pairIndex + 1);
					nextList = nextPair.ComputedWords(lv.textSide);
				}
			}

			if (nextList != null && nextList.size() > 0)
				if (nextList.get(0).x1 > last.x2)
					renderedInfo.x2B += nextList.get(0).x1 - last.x2;

		}

		// Alternating and advanced mode don't use colored backgrounds
		if (LayoutMode != LayoutMode_Normal)
			return;

		// Before drawing text we must draw colored background
		// Colored
		if (HighlightFragments)
			DrawBackground(lv.textSide, renderedInfo.line1, renderedInfo.x1,
					renderedInfo.line2, renderedInfo.x2B,
					lightColorTable.get(pairIndex % NumberofColors));

		if (HighlightFirstWords && list != null && list.size() > 0
				&& first.x2 >= first.x1) {

			RectF r = new RectF(lv.offset + first.x1, vMargin
					+ (cLine + first.line) * lineHeight, lv.offset + first.x2
					+ 1, vMargin + (cLine + first.line + 1) * lineHeight);

			paint.setShader(new LinearGradient(r.left, 0, r.right, 0,
					darkColorTable.get(pairIndex % NumberofColors),
					HighlightFragments ? lightColorTable.get(pairIndex
							% NumberofColors) : Color.WHITE, TileMode.CLAMP));

			canvas.drawRect(r, paint);

			paint.setShader(null);

		}

	}

	public boolean NotFitOnScreen(TextPair p) {

		if (LayoutMode == LayoutMode_Advanced) {
			if (reversed)
				return (p.renderedInfo2.line2 == -1 || p.renderedInfo2.line2 > LastFullScreenLine)
						|| p.renderedInfo2.line1 == -1;
			else
				return (p.renderedInfo1.line2 == -1 || p.renderedInfo1.line2 > LastFullScreenLine)
						|| p.renderedInfo1.line1 == -1;

		} else

			return (p.renderedInfo1.line2 == -1
					|| p.renderedInfo1.line2 > LastFullScreenLine
					|| p.renderedInfo2.line2 == -1 || p.renderedInfo2.line2 > LastFullScreenLine)
					|| p.renderedInfo1.line1 == -1
					|| p.renderedInfo2.line1 == -1;
	}

	private void RenderText(int pairIndex, int cLine, LayoutVars lv) {

		byte side = lv.textSide;
		float offset = lv.offset;

		TextPair p = pText.get(pairIndex);

		RenderedTextInfo renderedInfo = p.RenderedInfo(side);

		if (!renderedInfo.valid)
			return;

		// In view mode, show text in gray if it is not complete
		int newColor = NotFitOnScreen(p) || pairIndex < CurrentPair ? 2
				: ((LayoutMode == LayoutMode_Alternating)
						&& reversed == (side == 2) ? 3 : 1);
		
		if (newColor != currentTextColor) {

			currentTextColor = newColor;

			switch (newColor) {
			case 1:
				paint.setColor(Color.BLACK);
				break;
			case 2:
				paint.setColor(Color.GRAY);
				break;
			case 3:
				paint.setColor(0xFF228B22);
				break;
			}

		}

		paint.setStrokeWidth(1);

		ArrayList<WordInfo> list = p.ComputedWords(side);

		float x;
		int y = -1;

		ScreenWord prev_screen_word = null;
		ScreenWord s = null;
		ArrayList<ScreenWord> l = null;

		int prev_y = -1;

		if (list != null)
			for (int i = 0; i < list.size(); i++) {
				WordInfo r = list.get(i);

				y = cLine + r.line;

				if (y < 0)
					continue;

				if (y >= NumberOfScreenLines) {
					renderedInfo.line2 = -1;
					renderedInfo.x2 = 0;
					return;
				}

				s = new ScreenWord();

				if (prev_screen_word != null) {
					s.Prev = prev_screen_word;
					prev_screen_word.Next = s;
				}

				prev_screen_word = s;

				s.fX1 = r.x1;

				x = s.fX1 + offset;

				if (y != prev_y) {
					l = wordsOnScreen.get(y);

					if (l == null) {
						l = new ArrayList<ScreenWord>();
						wordsOnScreen.put(y, l);
					}
					prev_y = y;

					if (FirstRenderedPair == -1)
						FirstRenderedPair = pairIndex;

					if (pairIndex > LastRenderedPair)
						LastRenderedPair = pairIndex;

				}

				String wrd = r.word;

				canvas.drawText(wrd, x, y * lineHeight + textVOffset, paint);

				s.pairIndex = pairIndex;
				s.pos = r.pos;
				s.side = side;
				s.x1 = x;
				s.fX2 = r.x2;
				s.x2 = s.fX2 + offset;
				s.line = y;
				s.word = wrd;

				l.add(s);

			}
	}

	class LayoutVars {

		float offset;
		byte textSide;

		private LayoutVars(int side) {
			switch (LayoutMode) {
			case LayoutMode_Normal:

				if (side == 1)
					offset = PanelMargin;
				else
					offset = splitterPosition + splitterWidth + PanelMargin;

				if (reversed == (side == 1))
					textSide = 2;
				else
					textSide = 1;

				break;

			case LayoutMode_Alternating:

				if (reversed == (side == 1))
					textSide = 1;
				else
					textSide = 2;

				offset = PanelMargin;

				break;

			case LayoutMode_Advanced:

				offset = PanelMargin;

				textSide = (byte) side;

				break;

			default:
				textSide = 0;
				offset = 0;
				break;
			}
		}

	}

	private void RenderBackgroundSide(byte side, int startPair, int negHeight) {

		LayoutVars lv = new LayoutVars(side);

		int cPair = startPair;
		int cLine = -negHeight;

		// RenderedInfo and Backgrounds

		while (true) {

			TextPair p = pText.get(cPair);

			RenderBackground(cPair, cLine, lv);

			cLine += p.height;

			if (cLine >= NumberOfScreenLines)
				break;

			if (cPair < pText.Number() - 1)
				cPair++;
			else
				break;

		}

	}

	private void RenderPairText(byte side, int startPair, int negHeight) {

		LayoutVars lv = new LayoutVars(side);

		int cPair = startPair;
		int cLine = -negHeight;

		// Text itself

		while (true) {

			TextPair p = pText.get(cPair);

			RenderText(cPair, cLine, lv);

			cLine += p.height;

			if (cLine >= NumberOfScreenLines)
				break;

			if (cPair < pText.Number() - 1)
				cPair++;
			else
				break;
		}

	}

	public void RenderPairs() {

		wordsOnScreen.clear();
		
		if (pText.Number() == 0)
			return;

		TextPair p;

		int negHeight = 0;

		int startPair = CurrentPair;

		p = pText.get(startPair);

		FirstRenderedPair = -1;
		LastRenderedPair = -1;

		// REWIND

		if (LayoutMode == LayoutMode_Advanced) {

			byte side = (reversed ? (byte) 2 : (byte) 1);

			// Special rewinding algorithm for advanced mode
			if (!(p.StartParagraph(side))) {
				do {
					startPair--;
					p = pText.get(startPair);
				} while (!(p.StartParagraph(side)) && p.height == 0);

				negHeight = p.height;

			}

			currentTextColor = -1;

			RenderBackgroundSide(side, startPair, negHeight);
			RenderPairText(side, startPair, negHeight);

			return;

		} else if (LayoutMode == LayoutMode_Alternating) {
			// Special rewinding algorithm for alternating mode
			if (!(p.startParagraph1 || p.startParagraph2)) {
				do {
					startPair--;
					p = pText.get(startPair);
				} while (!(p.startParagraph1 || p.startParagraph2)
						&& p.height == 0);

				negHeight = p.height;

			}

		}

		else // NORMAL mode
		{
			// Standard rewinding algorithm
			if (!(p.startParagraph1 && p.startParagraph2)) {
				// Must rewind back for the closest (from above) Pair that is
				// either true-true or
				// multi-Line (charIndex. e. with Height > 0)

				do {
					startPair--;
					p = pText.get(startPair);
				} while (!(p.startParagraph1 && p.startParagraph2)
						&& p.height == 0);

				negHeight = p.height;

			}

		}

		RenderBackgroundSide((byte) 1, startPair, negHeight);
		RenderBackgroundSide((byte) 2, startPair, negHeight);

		currentTextColor = -1;

		RenderPairText((byte) 1, startPair, negHeight);
		RenderPairText((byte) 2, startPair, negHeight);

	}

	public void Render() {
		// Draw frames
		for (AbstractFrame f : frames)
			f.Draw(this);

		RenderAdvancedPopup();

	}

	private void RenderAdvancedPopup() {

		if (!(LayoutMode == LayoutMode_Advanced && popUpInfo.visible))
			return;

		DrawBackground((byte) 0, popUpInfo.Y, popUpInfo.X, popUpInfo.Y2,
				popUpInfo.X2, popUpColor);

		paint.setColor(popUpTextColor);

		// y * lineHeight + textVOffset

		for (WordInfo sw : popUpInfo.words)
			canvas.drawText(sw.word, sw.x1 + PanelMargin + popUpInfo.X,
					textVOffset + popUpInfo.offsetY * popUpOffsetY
							+ (popUpInfo.Y + sw.line) * lineHeight, paint);

	}

	public int Number() {
		return pText.Number();
	}

	public TextPair get(int pairIndex) {
		return pText.get(pairIndex);
	}

	private void DrawPositionStrip() {

		if (Number() == 0)
			return;

		int totalVolume = get(Number() - 1).aggregateSize;

		if (totalVolume == 0)
			return;

		int prevVolume;

		if (CurrentPair == 0)
			prevVolume = 0;
		else
			prevVolume = get(CurrentPair - 1).aggregateSize;

		paint.setTextSize(12);

		String percentText = String.format(Locale.US, "%.3f%%",
				(float) prevVolume / totalVolume * 100);

		float percentTextLength = paint.measureText(percentText);

		final int borderWidth = 2;

		paint.setColor(transparentBlack);

		canvas.drawRect((float) (viewWidth - percentTextLength) / 2
				- borderWidth, (float) viewHeight - 12 - 2 * borderWidth,
				(float) (viewWidth + percentTextLength) / 2 + borderWidth,
				(float) viewHeight, paint);

		paint.setColor(Color.WHITE);

		canvas.drawText(percentText, (viewWidth - percentTextLength) / 2,
				viewHeight - borderWidth, paint);

		paint.setTextSize(textFontSize);

		// Let's compute the size and coordinates of the rectangle

		// paint.setColor(color);

	}

	public void onDraw(Canvas canvas) {

		this.canvas = canvas;

		RenderPairs();
		Render();

		DrawPositionStrip();

		this.canvas = null;

	}

	private ScreenWord FindScreenWordByPosition(int pairIndex, int pos,
			byte side) {
		if (pos != -1)

			for (Map.Entry<Integer, ArrayList<ScreenWord>> entry : wordsOnScreen
					.entrySet())
				for (ScreenWord sw : entry.getValue())
					if (sw.pairIndex == pairIndex && sw.pos == pos
							&& sw.side == side)
						return sw;

		return null;

	}

	void UpdateSelectionFrame() {

		if (selectionFrame.side == (byte) 0)
			return;

		Rect r = new Rect();

		AssignProperSelectionOrder(r);

		ScreenWord word1 = FindScreenWordByPosition(r.top, r.left,
				selectionSide);
		ScreenWord word2 = FindScreenWordByPosition(r.bottom, r.right,
				selectionSide);

		selectionFrame.visible = true;

		if (word1 == null)
			if (r.top >= LastRenderedPair)
				selectionFrame.visible = false;
			else
				selectionFrame.line1 = -1;
		else {
			selectionFrame.line1 = word1.line;
			selectionFrame.x1 = word1.fX1;
		}

		if (word2 == null)
			if (r.bottom >= LastRenderedPair)
				selectionFrame.line2 = -1;
			else
				selectionFrame.visible = false;
		else {
			selectionFrame.line2 = word2.line;
			selectionFrame.x2 = word2.fX2;
		}

	}

	public void AssignProperSelectionOrder(Rect r) {
		if (Selection1Pair < Selection2Pair || Selection1Pair == Selection2Pair
				&& Selection1Position <= Selection2Position) {
			r.top = Selection1Pair;
			r.left = Selection1Position;
			r.bottom = Selection2Pair;
			r.right = Selection2Position;
		} else {
			r.top = Selection2Pair;
			r.left = Selection2Position;
			r.bottom = Selection1Pair;
			r.right = Selection1Position;
		}
	}

	public static boolean IsEasternCharacter(char c) {
		return (c >= (char) 0x2e80 && !(c >= (char) 0xac00 && c <= (char) 0xd7a3)); // Hangul
	}

	boolean WesternJoint(int firstPair, byte side) {

		TextPair first = pText.get(firstPair);
		TextPair second = pText.get(firstPair + 1);

		int firstLength = first.GetLength(side);

		if (firstLength == 0 || second.GetLength(side) == 0)
			return false; // no space needed between texts since one or both are
							// empty

		return !IsEasternCharacter(first.GetChar(side, firstLength - 1))
				&& !IsEasternCharacter(second.GetChar(side, 0));

	}

	public void DrawFrame(Frame frame) {
		if (frame == null)
			return;

		if (!frame.visible)
			return;

		paint.setColor(frame.framePen.color);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(frame.framePen.thickness);

		float textstart;
		float textend;

		if (frame.side == (byte) 1) {
			textstart = text1start;
			textend = text1end;
		} else {
			textstart = text2start;
			textend = text2end;
		}

		if (frame.line1 == frame.line2)
			if (frame.line1 == -1) {
				// The frame begins and ends beyond the screen
				// We draw two parallel, unconnected lines on both sides
				canvas.drawLine(textstart, 0, textstart, viewHeight - 1, paint);
				canvas.drawLine(textend, 0, textend, viewHeight - 1, paint);
			} else
				// A piece of text
				canvas.drawRect(textstart + frame.x1, vMargin + frame.line1
						* lineHeight - frameoffset_y, textstart + frame.x2 + 2
						* frameoffset_x, vMargin + (frame.line1 + 1)
						* lineHeight + frameoffset_y, paint);

		else if (frame.line1 == -1) {
			Path path = new Path();
			path.moveTo(textstart, 0);
			path.lineTo(textstart, vMargin + (frame.line2 + 1) * lineHeight
					+ frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ (frame.line2 + 1) * lineHeight + frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ frame.line2 * lineHeight + frameoffset_y);
			path.lineTo(textend, vMargin + frame.line2 * lineHeight
					+ frameoffset_y);
			path.lineTo(textend, 0);
			canvas.drawPath(path, paint);
		}

		else if (frame.line2 == -1)
			if (frame.x1 == 0) // Top starts at cursorX = 0
			{
				Path path = new Path();

				path.moveTo(textstart, viewHeight - 1);
				path.lineTo(textstart, vMargin + frame.line1 * lineHeight
						- frameoffset_y);
				path.lineTo(textend, vMargin + frame.line1 * lineHeight
						- frameoffset_y);
				path.lineTo(textend, viewHeight - 1);

				canvas.drawPath(path, paint);
			} else {
				Path path = new Path();

				path.moveTo(textstart, viewHeight - 1);
				path.lineTo(textstart, vMargin + (frame.line1 + 1) * lineHeight
						- frameoffset_y);
				path.lineTo(textstart + frame.x1, vMargin + (frame.line1 + 1)
						* lineHeight - frameoffset_y);
				path.lineTo(textstart + frame.x1, vMargin + frame.line1
						* lineHeight - frameoffset_y);
				path.lineTo(textend, vMargin + frame.line1 * lineHeight
						- frameoffset_y);
				path.lineTo(textend, viewHeight - 1);

				canvas.drawPath(path, paint);
			}

		else if (frame.x1 == 0) {
			Path path = new Path();
			path.moveTo(textend, vMargin + frame.line1 * lineHeight
					- frameoffset_y);
			path.lineTo(textstart, vMargin + frame.line1 * lineHeight
					- frameoffset_y);
			path.lineTo(textstart, vMargin + (frame.line2 + 1) * lineHeight
					+ frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ (frame.line2 + 1) * lineHeight + frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ frame.line2 * lineHeight + frameoffset_y);
			path.lineTo(textend, vMargin + frame.line2 * lineHeight
					+ frameoffset_y);
			canvas.drawPath(path, paint);
		} else

		{
			Path path = new Path();
			path.moveTo(textend, vMargin + frame.line1 * lineHeight
					- frameoffset_y);
			path.lineTo(textstart + frame.x1, vMargin + frame.line1
					* lineHeight - frameoffset_y);
			path.lineTo(textstart + frame.x1, vMargin + (frame.line1 + 1)
					* lineHeight - frameoffset_y);
			path.lineTo(textstart, vMargin + (frame.line1 + 1) * lineHeight
					- frameoffset_y);
			path.lineTo(textstart, vMargin + (frame.line2 + 1) * lineHeight
					+ frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ (frame.line2 + 1) * lineHeight + frameoffset_y);
			path.lineTo(textstart + frame.x2 + 2 * frameoffset_x, vMargin
					+ (frame.line2) * lineHeight + frameoffset_y);
			path.lineTo(textend, vMargin + (frame.line2) * lineHeight
					+ frameoffset_y);
			canvas.drawPath(path, paint);
		}

	}

	public ScreenWord WordAfterCursor(int line, float lastMouseX2, byte side) {
		ArrayList<ScreenWord> listOfWords;

		ScreenWord lastWord = null;

		listOfWords = wordsOnScreen.get(line);

		if (listOfWords != null) {
			for (ScreenWord s : listOfWords) {
				if (side != -1 && s.side != side)
					continue;
				if (lastMouseX2 >= s.x1
						&& (lastWord == null || lastWord.x1 < s.x1))
					lastWord = s;
			}
		}

		return lastWord;
	}

	public void ComputeNumberOfScreenLines() {
		NumberOfScreenLines = (int) ((viewHeight - 2 * vMargin) / lineHeight);

		LastFullScreenLine = NumberOfScreenLines - 1;

		if (lineHeight * NumberOfScreenLines < viewHeight - 2 * vMargin)
			NumberOfScreenLines++;

	}

	public void PrepareScreen(int startPair, int requiredLines) {

		if (pText.Number() == 0)
			return;

		if (LayoutMode == LayoutMode_Normal)
			PrepareScreen_Normal(startPair, requiredLines);

		else if (LayoutMode == LayoutMode_Alternating)
			PrepareScreen_Alternating(startPair, requiredLines);

		else if (LayoutMode == LayoutMode_Advanced)
			PrepareScreen_Advanced(startPair, requiredLines);

	}

	public void PrepareScreen_Normal(int startPair, int requiredLines) {

		ProcessArgs a1 = new ProcessArgs((byte) 1);
		ProcessArgs a2 = new ProcessArgs((byte) 2);

		int remainder = requiredLines;

		int requiredHeight;

		// If the startPair is not starting from a new Line on both texts (i. e.
		// it is not a true-true Pair)
		// then we must ensure that all of the preceding pairs starting from the
		// previous true-true pairs are computed,
		// because we need to know where exactly in the Line our Pair starts on
		// both sides.
		// Actually, it is sufficient to stop at the closest partially-computed
		// Pair (because if it is partially
		// computed we can safely compute it to the end)

		int cPair = startPair;

		TextPair p = pText.get(cPair);

		// Look for the closest true-true or partially computed Pair
		while (!(p.startParagraph1 && p.startParagraph2) && p.height == -1) {
			cPair--;
			p = pText.get(cPair);
		}

		a1.p = p;
		a2.p = p;

		int height;

		TextPair prev_pair = null;

		a1.MaxWidth = (reversed ? rightWidth : leftWidth) - 2 * PanelMargin;
		a2.MaxWidth = (reversed ? leftWidth : rightWidth) - 2 * PanelMargin;

		while (true) {

			if (cPair < startPair || requiredLines == -1)
				requiredHeight = -1;
			else {

				if (p.height != -1 && remainder <= p.height)
					// cool
					return;

				requiredHeight = remainder;

			}

			if (p.allLinesComputed1 && p.allLinesComputed2)
				height = p.height;

			else {

				a1.height = p.height;
				a2.height = p.height;

				if (p.height == -1)
					pText.computedPairs.add(p);

				a1.requiredHeight = requiredHeight;
				a2.requiredHeight = requiredHeight;

				a1.ProcessTextFromPair();
				a2.ProcessTextFromPair();

				// Now we must check whether one of the heights is smaller than
				// the other

				height = a1.height;

				if (a1.height < a2.height) {
					// Line break 1
					ParallelText.InsertWords(a1.words, 0);
					a1.occLength = 0;
					height = a2.height;
				} else if (a2.height < a1.height) {
					// Line break 2
					ParallelText.InsertWords(a2.words, 0);
					a2.occLength = 0;
				}

				if (p.allLinesComputed1
						&& p.allLinesComputed2
						&& (p.structureLevel > 0 || cPair + 1 < pText.Number()
								&& pText.textPairs.get(cPair + 1).structureLevel > 0)) {
					ParallelText.InsertWords(a1.words, 0);
					ParallelText.InsertWords(a2.words, 0);
					a1.occLength = 0;
					a2.occLength = 0;
					height += 2;
				}

				p.height = height;

			}

			if (requiredHeight != -1) {
				remainder -= height;

				if (remainder <= 0)
					return;
			}

			// Are there more text pairs?

			if (cPair + 1 == pText.Number()) {
				// This was the last Pair, no more coming.
				ParallelText.InsertWords(a1.words, 0);
				ParallelText.InsertWords(a2.words, 0);
				return;
			}

			// ...There are.

			cPair++;

			prev_pair = p;

			p = pText.textPairs.get(cPair);

			a1.p = p;
			a2.p = p;

			if (a1.NeedToLineBreakFirstWord(p.startParagraph1)
					|| a2.NeedToLineBreakFirstWord(p.startParagraph2)) {

				ParallelText.InsertWords(a1.words, 0);
				ParallelText.InsertWords(a2.words, 0);

				prev_pair.height++;

				if (requiredHeight != -1) {
					remainder--;

					if (remainder <= 0)
						return;
				}

				a1.occLength = 0;
				a2.occLength = 0;
			}

			if (requiredLines == -1 && cPair > startPair
					&& prev_pair.height > 0)
				return;

		}

	}

	public void PrepareScreen_Alternating(int startPair, int requiredLines) {

		ProcessArgs a = new ProcessArgs((byte) 0);

		int remainder = requiredLines;

		int txtWidth = viewWidth - 2 * PanelMargin;

		// If the startPair is not starting from a new Line on both texts (i. e.
		// it is not a true-true Pair)
		// then we must ensure that all of the preceding pairs starting from the
		// previous true-true pairs are computed,
		// because we need to know where exactly in the Line our Pair starts on
		// both sides.
		// Actually, it is sufficient to stop at the closest partially-computed
		// Pair (because if it is partially
		// computed we can safely compute it to the end)

		int cPair = startPair;

		TextPair p = pText.get(cPair);

		// Look for the closest true-true or partially computed Pair
		while (!(p.startParagraph1 || p.startParagraph2) && p.height == -1) {
			cPair--;
			p = pText.get(cPair);
		}

		a.p = p;

		int height;

		TextPair prev_pair = null;

		byte side1;
		byte side2;

		if (reversed) {
			side1 = 2;
			side2 = 1;
		} else {
			side1 = 1;
			side2 = 2;
		}

		a.MaxWidth = txtWidth;

		while (true) {

			if (cPair < startPair || requiredLines == -1)
				a.requiredHeight = -1;
			else {

				if (p.height != -1 && remainder <= p.height)
					// cool
					return;

				a.requiredHeight = remainder;

			}

			height = p.height;

			if (!(p.allLinesComputed1 && p.allLinesComputed2)) {

				if (p.height == -1) {
					pText.computedPairs.add(p);
					if (p.startParagraph1 || p.startParagraph2)
						a.occLength = indentLength;
				}

				a.height = height;

				a.side = side1;
				a.ProcessTextFromPair();

				a.side = side2;
				a.ProcessTextFromPair();

				height = a.height;

				if (p.allLinesComputed1
						&& p.allLinesComputed2
						&& (p.structureLevel > 0 || cPair + 1 < pText.Number()
								&& pText.get(cPair + 1).structureLevel > 0)) {
					ParallelText.InsertWords(a.words, 0);
					height += 2;
				}

				p.height = height;

			}

			if (a.requiredHeight != -1) {
				remainder -= height;

				if (remainder <= 0)
					return;
			}

			// Are there more text pairs?

			if (cPair + 1 == pText.Number()) {
				// This was the last Pair, no more coming.
				ParallelText.InsertWords(a.words, 0);
				return;
			}

			// ...There are.

			cPair++;

			prev_pair = p;

			p = pText.textPairs.get(cPair);

			a.p = p;
			a.side = side1;

			if (a.words.size() > 0
					&& a.NeedToLineBreakFirstWord(p.startParagraph1
							|| p.startParagraph2)) {

				ParallelText.InsertWords(a.words, (p.startParagraph1
						|| p.startParagraph2 ? 0 : txtWidth - a.occLength));

				prev_pair.height++;

				if (a.requiredHeight != -1) {
					remainder--;

					if (remainder <= 0)
						return;
				}

				a.occLength = (p.startParagraph1 || p.startParagraph2 ? indentLength
						: 0);
			}

			if (requiredLines == -1 && cPair > startPair
					&& prev_pair.height > 0)
				return;

		}

	}

	public void PrepareScreen_Advanced(int startPair, int requiredLines) {

		ProcessArgs a = new ProcessArgs(reversed ? (byte) 2 : (byte) 1);

		int remainder = requiredLines;

		int txtWidth = viewWidth - 2 * PanelMargin;

		// If the startPair is not starting from a new Line on both texts (i. e.
		// it is not a true-true Pair)
		// then we must ensure that all of the preceding pairs starting from the
		// previous true-true pairs are computed,
		// because we need to know where exactly in the Line our Pair starts on
		// both sides.
		// Actually, it is sufficient to stop at the closest partially-computed
		// Pair (because if it is partially
		// computed we can safely compute it to the end)

		int cPair = startPair;

		TextPair p = pText.get(cPair);

		// Look for the closest true-true or partially computed Pair
		while (!(p.StartParagraph(a.side)) && p.height == -1) {
			cPair--;
			p = pText.get(cPair);
		}

		a.p = p;

		TextPair prev_pair = null;

		a.MaxWidth = txtWidth;

		while (true) {

			if (cPair < startPair || requiredLines == -1)
				a.requiredHeight = -1;
			else {

				if (p.height != -1 && remainder <= p.height)
					// cool
					return;

				a.requiredHeight = remainder;

			}

			a.height = p.height;

			if (!(p.AllLinesComputed(a.side))) {

				if (p.height == -1) {
					pText.computedPairs.add(p);
					if (p.StartParagraph(a.side))
						a.occLength = indentLength;
				}

				a.ProcessTextFromPair();

				if (p.AllLinesComputed(a.side)
						&& (p.structureLevel > 0 || cPair + 1 < pText.Number()
								&& pText.get(cPair + 1).structureLevel > 0)) {
					ParallelText.InsertWords(a.words, 0);
					a.height += 2;
				}

				p.height = a.height;

			}

			if (a.requiredHeight != -1) {
				remainder -= a.height;

				if (remainder <= 0)
					return;
			}

			// Are there more text pairs?

			if (cPair + 1 == pText.Number()) {
				// This was the last Pair, no more coming.
				ParallelText.InsertWords(a.words, 0);
				return;
			}

			// ...There are.

			cPair++;

			prev_pair = p;

			p = pText.textPairs.get(cPair);

			a.p = p;

			if (a.words.size() > 0
					&& a.NeedToLineBreakFirstWord(p.StartParagraph(a.side))) {

				ParallelText.InsertWords(a.words, p.StartParagraph(a.side) ? 0
						: txtWidth - a.occLength);

				prev_pair.height++;

				if (a.requiredHeight != -1) {
					remainder--;

					if (remainder <= 0)
						return;
				}

				a.occLength = (p.StartParagraph(a.side) ? indentLength : 0);
			}

			if (requiredLines == -1 && cPair > startPair
					&& prev_pair.height > 0)
				return;

		}

	}

	public void PrepareScreen() {
		PrepareScreen(CurrentPair, NumberOfScreenLines);
	}

	public void UpdateScreen() {
		PrepareScreen();
		pTV.invalidate();
	}

	public void ProcessLayoutChange(boolean computeNumberOfScreenLines) {
		// erase both tables
		pText.Truncate();

		if (computeNumberOfScreenLines)
			ComputeNumberOfScreenLines();
		
		UpdateScreen();
	}

	public Float WordWidth(String word) {
		// First, try to use data from the dictionary if it's there
		Float result = widthDictionary.get(word);
		if (result == null) {
			// Measure and store in the dictionary
			result = paint.measureText(word);
			widthDictionary.put(word, result);
		}
		return result;
	}

	public static String GetWord(TextPair p, byte side, int pos) {
		char c;

		StringBuilder word = new StringBuilder();

		String text = (side == (byte) 1 ? p.text1 : p.text2);

		int length = text.length();

		while (pos < length) {
			c = text.charAt(pos);
			if (c == ' ' || c == '\t')
				if (word.length() == 0) {
					pos++;
					continue;
				} else
					break;

			if (c == '\n' || c == '\r')
				break;

			if (IsEasternCharacter(c))
				return String.valueOf(c);

			word.append(c);

			pos++;
		}

		return word.toString();
	}

	private void InitializeColors() {
		colorTableH = new ArrayList<Double>();

		colorTableH.add(0.162);
		colorTableH.add(0.34);
		colorTableH.add(0.492);
		colorTableH.add(0.68);
		colorTableH.add(0.83);
		colorTableH.add(0.0);
		// colorTableH.add(0.11); // Orange? Too close to yellow. Disable for
		// now

		NumberofColors = (byte) colorTableH.size();

		darkColorTable = new ArrayList<Integer>();
		lightColorTable = new ArrayList<Integer>();

	}

	public void CreateNewParallelBook() {
		pText = new ParallelText();
		CurrentPair = 0;
		reversed = false;
	}

	public void ComputeSpaceLength() {
		widthDictionary.clear();
		SpaceLength = WordWidth(" ");

		ComputeIndent();

		lineHeight = paint.descent() - paint.ascent();

		textVOffset = vMargin + lineHeight - paint.descent();

	}

	public void ComputeSideCoordinates() {

		if (LayoutMode == LayoutMode_Alternating
				|| LayoutMode == LayoutMode_Advanced) {
			text1start = PanelMargin - frameoffset_x;
			text1end = viewWidth - PanelMargin + frameoffset_x;

			text2start = text1start;
			text2end = text1end;
		}

		else if (reversed) {
			text1start = splitterPosition + splitterWidth + PanelMargin
					- frameoffset_x;
			text1end = viewWidth - PanelMargin + frameoffset_x;

			text2start = PanelMargin - frameoffset_x;
			text2end = leftWidth - PanelMargin + frameoffset_x;
		} else {
			text1start = PanelMargin - frameoffset_x;
			text1end = leftWidth - PanelMargin + frameoffset_x;

			text2start = splitterPosition + splitterWidth + PanelMargin
					- frameoffset_x;
			text2end = viewWidth - PanelMargin + frameoffset_x;
		}
	}

	public void setSplitterPosition(float newSplitterPosition) {

		splitterPosition = newSplitterPosition;

		leftWidth = splitterPosition;
		rightWidth = this.viewWidth - splitterWidth - leftWidth;
		rightPosition = splitterPosition + splitterWidth;

		ComputeSideCoordinates();
	}

	public void SetSplitterPositionByRatio() {
		setSplitterPosition((int) ((viewWidth * SplitterRatio) - splitterWidth / 2));
	}

	public void SetSplitterPositionByRatio(float newSplitterRatio) {
		SplitterRatio = newSplitterRatio;
		SetSplitterPositionByRatio();
	}

	public void SetSplitterRatioByPosition() {
		SplitterRatio = (splitterPosition + (float) splitterWidth / 2)
				/ viewWidth;
	}

	public void SetColorsByBrightness() {

		if (darkColorTable.size() > 0) {
			darkColorTable.clear();
			lightColorTable.clear();

		}

		int lightColor;
		int darkColor;

		for (byte i = 0; i < NumberofColors; i++) {
			lightColor = HSL2RGB(colorTableH.get(i), 1, brightness);
			lightColorTable.add(lightColor);

			darkColor = HSL2RGB(colorTableH.get(i), 1, brightness - 0.1);
			darkColorTable.add(darkColor);
		}

	}

	private static int HSL2RGB(double hue, double saturation, double lighting) {
		double v;
		double r, g, b;

		r = lighting; // default to gray
		g = lighting;
		b = lighting;
		v = (lighting <= 0.5) ? (lighting * (1.0 + saturation)) : (lighting
				+ saturation - lighting * saturation);

		if (v > 0) {
			double m;
			double sv;
			int sextant;
			double fract, vsf, mid1, mid2;

			m = lighting + lighting - v;
			sv = (v - m) / v;
			hue *= 6.0;
			sextant = (int) hue;
			fract = hue - sextant;
			vsf = v * sv * fract;
			mid1 = m + vsf;
			mid2 = v - vsf;
			switch (sextant) {
			case 0:
				r = v;
				g = mid1;
				b = m;
				break;
			case 1:
				r = mid2;
				g = v;
				b = m;
				break;
			case 2:
				r = m;
				g = v;
				b = mid1;
				break;
			case 3:
				r = m;
				g = mid2;
				b = v;
				break;
			case 4:
				r = mid1;
				g = m;
				b = v;
				break;
			case 5:
				r = v;
				g = m;
				b = mid2;
				break;
			}
		}

		return Color.rgb((int) (r * 255.0f), (int) (g * 255.0f),
				(int) (b * 255.0f));
	}

	public double getBrightness() {
		return brightness;
	}

	public void setBrightness(double brightness) {
		this.brightness = brightness;

		SetColorsByBrightness();

	}

	public ParallelTextData() {

		CreateNewParallelBook();
		
		fileUsageInfo = new ArrayList<FileUsageInfo>();
		
		wordsOnScreen = new TreeMap<Integer, ArrayList<ScreenWord>>();

		vMargin = 0;
		PanelMargin = 3;
		SplitterRatio = 0.5f;
		splitterWidth = 7;

		LastMouseX = -1;
		LastMouseY = -1;

		textFontSize = 18.0f;

		frames = new ArrayList<AbstractFrame>();

		Pen selectionPen = new Pen(Color.BLACK, 2.0F);

		selectionFrame = new Frame(selectionPen, frames);

		widthDictionary = new HashMap<String, Float>();

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		

		reversed = true;
		
		InitializeColors();

		// ADVANCED MODE POPUP
		popUpInfo = new PopUpInfo();
		Pen AdvancedHighlightPen = new Pen(0xFF4682B4, 2.0F);
		AdvancedHighlightFrame = new Frame(AdvancedHighlightPen, frames);
		int popUpOpacity = 210;
		popUpColor = Color.argb(popUpOpacity, 0, 0, 0);
		popUpTextColor = Color.WHITE;
		
		fontRangeSet = false;
		
	}

	private void GotoPair(int newCurrentPair) {
		if (CurrentPair == newCurrentPair)
			return;
		CurrentPair = newCurrentPair;
		UpdateScreen();
	}

	public void ProcessPageDown() {

		// Let's find the last pair that fully fits on normal lines
		// then rewind to the next of it
		if (LastRenderedPair != CurrentPair) {
			int pairIndex = LastRenderedPair;

			while (pairIndex > 0 && NotFitOnScreen(get(pairIndex)))
				pairIndex--;

			pairIndex++;

			if (pairIndex > Number() - 1)
				pairIndex = Number() - 1;

			GotoPair(pairIndex);
		}

	}

	public void ProcessKeyDown() {

		int n1 = Number() - 1;

		if (CurrentPair == n1)
			return;

		int pairIndex = CurrentPair;
		while (get(pairIndex).height == 0 && pairIndex < n1)
			pairIndex++;
		if (pairIndex < n1)
			pairIndex++;
		if (pairIndex != CurrentPair)
			GotoPair(pairIndex);

	}

	public void ProcessKeyUp() {
		if (CurrentPair == 0)
			return;

		int newCurrentPair = CurrentPair;

		TextPair processedPair;

		while (newCurrentPair > 0) {
			newCurrentPair--;
			processedPair = get(newCurrentPair);

			if (!(processedPair.allLinesComputed1 && processedPair.allLinesComputed2))
				PrepareScreen(newCurrentPair, -1);

			if (processedPair.height > 0)
				break;
		}

		if (newCurrentPair != CurrentPair)
			GotoPair(newCurrentPair);
	}
	

	public void ProcessPageUp() {

		int newCurrentPair = CurrentPair;
		int req = LastFullScreenLine;

		int accLines = 0; // Accumulated lines

		TextPair processedPair;

		while (newCurrentPair > 0) {
			newCurrentPair--;
			processedPair = get(newCurrentPair);

			if (!(processedPair.allLinesComputed1 && processedPair.allLinesComputed2))
				PrepareScreen(newCurrentPair, -1);

			accLines += processedPair.height;

			if (accLines > req) {
				newCurrentPair++;
				break;
			}
		}

		if (newCurrentPair == CurrentPair && newCurrentPair > 0)
			newCurrentPair--;

		if (newCurrentPair != CurrentPair)
			GotoPair(newCurrentPair);

	}

	public boolean XonSplitter(float x, float density) {

		float center = splitterPosition + splitterWidth / 2;

		float dst = (x - center) / density;

		if (dst < 0)
			dst = -dst;

		return (dst < FINGERTIP);

	}

	void ComputeIndent() {
		indentLength = (LayoutMode == LayoutMode_Normal ? 0 : SpaceLength * 8);
	}

	public void SetLayoutMode() {

		ComputeSpaceLength();

		ComputeSideCoordinates();

		Advanced_HighlightedPair = -1;
		AdvancedHighlightFrame.visible = false;
		
	}

	public void ProcessMousePosition(boolean forced, boolean renderRequired) {
		// Let's check whether the cursor points to a Word

		// Compute current Line

		int side = -1;

		boolean needToRender = false;

		int line = (int) ((LastMouseY - vMargin) / lineHeight);

		// Let's see what we've got on this Line

		ScreenWord found_word = WordAfterCursor(line, LastMouseX, (byte) side);

		mouse_text_word = found_word;

		if (LayoutMode == LayoutMode_Advanced) {

			int newPair = (mouse_text_word == null ? -1
					: mouse_text_word.pairIndex);

			if (newPair == Advanced_HighlightedPair) {
				TurnAdvancedPopupOff();
				needToRender = true;
			} else {
				Advanced_HighlightedPair = newPair;

				popUpInfo.visible = false;

				if (newPair == -1) {
					AdvancedHighlightFrame.visible = false;

				} else {

					TextPair p = pText.get(newPair);

					RenderedTextInfo r = (reversed ? p.renderedInfo2
							: p.renderedInfo1);

					if (r.valid) {

						AdvancedHighlightFrame.visible = true;
						AdvancedHighlightFrame.side = mouse_text_word.side;
						AdvancedHighlightFrame.line1 = r.line1;
						AdvancedHighlightFrame.line2 = r.line2;
						AdvancedHighlightFrame.x1 = r.x1;
						AdvancedHighlightFrame.x2 = r.x2;

						byte trSide = (reversed ? (byte) 1 : (byte) 2);

						ProcessArgs a = new ProcessArgs(trSide);

						a.words = new ArrayList<CommonWordInfo>();

						a.MaxWidth = viewWidth - 2 * PanelMargin;
						a.p = p;
						a.requiredHeight = NumberOfScreenLines;

						ArrayList<WordInfo> c = p.ComputedWords(trSide);

						if (c == null || c.size() == 0) {
							a.ProcessTextFromPair();
							ParallelText.InsertWords(a.words, 0);
							c = p.ComputedWords(trSide);

						}

						if (c != null && c.size() != 0) {

							if (c.get(0).line > 0) {
								int linesToDec = c.get(0).line;

								for (WordInfo wi : c)
									wi.line -= linesToDec;
							}

							DeterminePopupPosition(c, r, a.MaxWidth);

						}

					}

					else // !r.Valid
					{

					}

				}

				needToRender = true;
			}

		}

		if (needToRender)
			pTV.invalidate();
	}

	private void DeterminePopupPosition(ArrayList<WordInfo> c,
			RenderedTextInfo r, float maxWidth) {

		popUpInfo.visible = true;
		popUpInfo.words = c;

		WordInfo last = c.get(c.size() - 1);

		// h: height

		int h1;
		int up;
		int down;

		if (r.line1 == -1)
			up = 0;
		else
			up = r.line1;

		if (r.line2 == -1)
			down = 0;
		else
			down = LastFullScreenLine - r.line2;

		if (r.line1 == -1 || r.line2 == -1)
			h1 = LastFullScreenLine + 1;
		else
			h1 = r.line2 - r.line1 + 1;

		int h2 = last.line + 1;

		// s: single
		boolean s1 = (h1 == 1);
		boolean s2 = (h2 == 1);

		float length2 = last.x2;

		popUpInfo.Y = -1;

		if (s1 && s2) {
			if (down > 0) {
				if (r.x1 + length2 <= maxWidth)
					SetPopUpCoordinates(r.line2 + 1, r.x1, 1, 0);
				else
					SetPopUpCoordinates(r.line2 + 1, maxWidth - length2, 1, 0);
			} else if (up > 0) {
				if (r.x1 + length2 <= maxWidth)
					SetPopUpCoordinates(r.line1 - 1, r.x1, -1, 0);
				else
					SetPopUpCoordinates(r.line1 - 1, maxWidth - length2, -1, 0);
			} else if (popUpOffsetX + r.x2 + length2 <= maxWidth)
				SetPopUpCoordinates(r.line1, r.x2, 0, 1);
			else if (r.x1 >= popUpOffsetX + length2)
				SetPopUpCoordinates(r.line1, r.x1 - length2, 0, -1);

		} else {
			if (h2 <= down)
				SetPopUpCoordinates(r.line2 + 1, 0, 1, 0);
			else if (s2 && r.line2 != -1 && r.line2 <= LastFullScreenLine
					&& r.x2 + popUpOffsetX + length2 <= maxWidth)
				SetPopUpCoordinates(r.line2, r.x2, 0, 1);
			else if (h2 <= up)
				if (h2 == 1)
					SetPopUpCoordinates(r.line1 - 1, maxWidth - length2, -1, 0);
				else if (r.line1 != -1 && r.x1 >= length2 + popUpOffsetX)
					SetPopUpCoordinates(r.line1 - h2 + 1, 0, -1, 0);
				else
					SetPopUpCoordinates(r.line1 - h2, 0, -1, 0);
			else if (s2 && r.line1 != -1 && r.x1 >= length2 + popUpOffsetX)
				SetPopUpCoordinates(r.line1, r.x1 - length2, 0, -1);
		}

		if (popUpInfo.Y == -1)
			// The worst case: draw over
			SetPopUpCoordinates((NumberOfScreenLines - h2 + 1) / 2, 0, 0, 0);

		popUpInfo.Y2 = popUpInfo.Y + h2 - 1;
		popUpInfo.X2 = popUpInfo.X + last.x2;

	}

	private void SetPopUpCoordinates(int Y, float x1, int offY, int offX) {
		popUpInfo.Y = Y;
		popUpInfo.X = x1 + offX * popUpOffsetX;
		popUpInfo.offsetY = offY;

	}

	public void TurnAdvancedPopupOff() {
		AdvancedHighlightFrame.visible = false;
		popUpInfo.visible = false;
		Advanced_HighlightedPair = -1;

	}
	
	
	private static float minNumberOfLines = 16;
	private static float maxNumberOfLines = 50;

	public void setFontRange(int w, int h) {
		
		int min;
		int max;
		
		if (w > h) {
			max = w;
			min = h;
		} else {
			max = h;
			min = w;
		}
		
		fontSizeMin = max / maxNumberOfLines;
		fontSizeMax = min / minNumberOfLines;
		
	}
	
	public void setFontSizeByMinMaxProportion() {
		
		textFontSize = fontSizeMin + (fontSizeMax - fontSizeMin) * fontProportion / 1000;
		
	}
	
	public void setFontSize(boolean processLayoutChange) {
		
		setFontSizeByMinMaxProportion();

		paint.setTextSize(textFontSize);

		ComputeSpaceLength();

		if (processLayoutChange)
			ProcessLayoutChange(true);

	}

	public void clearParallelText() {
		
		pText.textPairs.clear();
		pText.Truncate();
		pText.contents.Clear();
		bookOpened = false;
		pTV.updateNoBookVisibility();
		PrepareScreen();
	}

	

}
