package ru.parallelbooks.aglonareader;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ParallelText {
	public String author1;
	public String title1;
	public String info1;
	public String lang1;

	public String author2;
	public String title2;
	public String info2;
	public String lang2;

	public String info;

	// --Commented out by Inspection (08/20/15 7:36 PM):public String fileName;

	public final ArrayList<TextPair> textPairs;

	// / Contains a list of pairs which are at least partially computed.
	// / It is used for speedy truncating.
	public final ArrayList<TextPair> computedPairs;
	
	public final BookContents contents;
	
	public int contentsSide;

	public int Number() {
		return textPairs.size();
	}

	public ParallelText() {
		textPairs = new ArrayList<TextPair>();
		computedPairs = new ArrayList<TextPair>();
		contents = new BookContents();
	}

	public TextPair get(int pairIndex) {
		return textPairs.get(pairIndex);
	}

// --Commented out by Inspection START (08/20/15 7:44 PM):
//	public void AddPair(String text1, String text2, boolean startParagraph1,
//			boolean startParagraph2) {
//		TextPair newPair = textPairs.size() == 0 ? new TextPair(text1, text2,
//				true, true) : new TextPair(text1, text2, startParagraph1,
//				startParagraph2);
//
//		textPairs.add(newPair);
//
//	}
// --Commented out by Inspection STOP (08/20/15 7:44 PM)

// --Commented out by Inspection START (08/20/15 7:36 PM):
//	public void AddPair(String text1, String text2) {
//		AddPair(text1, text2, true, true);
//	}
// --Commented out by Inspection STOP (08/20/15 7:36 PM)

	public void Truncate() {
		for (TextPair p : computedPairs)
			p.ClearComputedWords();
		computedPairs.clear();
	}

	public static void InsertWords(ArrayList<CommonWordInfo> list,
			float spaceLeft) {

		if (list == null)
			return;

		Collection<WordInfo> l = null;
		TextPair prev_p = null;
		byte prev_side = 0;

		float bias = 0.0f;

		// Spaces can be only in cases like W E or E W or W W,
		// where W is a "western" word and E is an eastern character
		// they can't be between EE

		CommonWordInfo previousWord = null;

		int numberOfSpacesLeft = 0;

		// So before extending spaces we must know their number.
		for (CommonWordInfo r : list) {
			if (previousWord != null)
				if (!(r.eastern && previousWord.eastern))
					numberOfSpacesLeft++;
			previousWord = r;
		}

		previousWord = null;

		for (CommonWordInfo r : list) {

			if (spaceLeft != 0 && previousWord != null
					&& !(r.eastern && previousWord.eastern)) {
				float inc = (spaceLeft / numberOfSpacesLeft);
				bias += inc;
				spaceLeft -= inc;
				numberOfSpacesLeft--;
			}

			if (prev_p != r.textPair) {
				prev_p = r.textPair;
				prev_side = 0;
			}

			if (r.side != prev_side) {
				prev_side = r.side;
				l = prev_p.ComputedWords(r.side, true);
			}

			l.add(new WordInfo(r.word, r.line, r.x1 + bias, r.x2 + bias, r.pos,
					r.eastern));

			previousWord = r;
		}

		list.clear();
	}

	public boolean Load(String fileName) {

		boolean result;
		
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();

			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			
			UnicodeBOMInputStream u = new UnicodeBOMInputStream(fis);
			
			u.skipBOM();
			
			parser.setInput(new InputStreamReader(u));

			result = Load(parser);

		} catch (XmlPullParserException e) {
			result = false;
		} catch (IOException e) {
			result = false;
		}
		
		return result;

	}

	private boolean Load(XmlPullParser reader) {

		int eventType;
		try {
			eventType = reader.getEventType();

			if (eventType != XmlPullParser.START_DOCUMENT)
				return false;

			do
				eventType = reader.next();
			while (eventType != XmlPullParser.START_TAG
					&& eventType != XmlPullParser.END_DOCUMENT);

			if (eventType != XmlPullParser.START_TAG)
				return false;

			if (!reader.getName().equals("ParallelBook"))
				return false;

			if (reader.getAttributeCount() != 9
					|| !reader.getAttributeName(0).equals("lang1")
					|| !reader.getAttributeName(1).equals("author1")
					|| !reader.getAttributeName(2).equals("title1")
					|| !reader.getAttributeName(3).equals("info1")
					|| !reader.getAttributeName(4).equals("lang2")
					|| !reader.getAttributeName(5).equals("author2")
					|| !reader.getAttributeName(6).equals("title2")
					|| !reader.getAttributeName(7).equals("info2")
					|| !reader.getAttributeName(8).equals("info"))

				return false;

			this.lang1 = reader.getAttributeValue(0);
			this.author1 = reader.getAttributeValue(1);
			this.title1 = reader.getAttributeValue(2);
			this.info1 = reader.getAttributeValue(3);
			this.lang2 = reader.getAttributeValue(4);
			this.author2 = reader.getAttributeValue(5);
			this.title2 = reader.getAttributeValue(6);
			this.info2 = reader.getAttributeValue(7);
			this.info = reader.getAttributeValue(8);

			eventType = reader.next();

			String value;
			String value_s;
			String value_t;

			while (eventType == XmlPullParser.START_TAG) {

				if (!reader.getName().equals("p"))
					return false;

				TextPair p = new TextPair();

				switch (reader.getAttributeCount()) {
				case 2:

					if (!reader.getAttributeName(0).equals("s")
							|| !reader.getAttributeName(1).equals("t"))
						return false;

					value_s = reader.getAttributeValue(0);
					value_t = reader.getAttributeValue(1);

					break;

				case 3:

					if (!reader.getAttributeName(0).equals("l")
							|| !reader.getAttributeName(1).equals("s")
							|| !reader.getAttributeName(2).equals("t"))
						return false;

					value = reader.getAttributeValue(0);

					if (value.equals("3")) {
						p.startParagraph1 = true;
						p.startParagraph2 = true;
					} else if (value.equals("1"))
						p.startParagraph1 = true;
					else if (value.equals("2"))
						p.startParagraph2 = true;
					else if (value.equals("4")) {
						p.SetStructureLevel((byte) 1);
						addToContents(1);
					}
					else if (value.equals("5")) {
						p.SetStructureLevel((byte) 2);
						addToContents(2);
					}
					else if (value.equals("6")) {
						p.SetStructureLevel((byte) 3);
						addToContents(3);
					} else {
						Log.d("###", "Value = " + value);
					}

					value_s = reader.getAttributeValue(1);
					value_t = reader.getAttributeValue(2);

					break;

				default:
					return false;

				}

				p.text1 = value_s;
				p.text2 = value_t;

				p.totalTextSize = value_s.length() + value_t.length();

				textPairs.add(p);

				eventType = reader.next();

				if (eventType != XmlPullParser.END_TAG)
					return false;

				eventType = reader.next();

			}

			if (textPairs.size() > 0)
				UpdateAggregates(0);
			
			contentsSide = 1;

			return true;

		} catch (XmlPullParserException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	private void addToContents(int i) {
		contents.add(textPairs.size(), i);
		
	}

	private void UpdateAggregates(int pairIndex) {
		int accLength;

		if (pairIndex == 0)
			accLength = -2;
		else
			accLength = textPairs.get(pairIndex - 1).aggregateSize;

		TextPair tp;

		int pairsSize = Number();

		for (int i = pairIndex; i < pairsSize; i++) {
			tp = textPairs.get(i);
			accLength += 2 + tp.totalTextSize;
			tp.aggregateSize = accLength;
		}
	}
}
