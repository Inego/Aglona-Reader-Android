package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

public class TextPair {

	public int aggregateSize = 0;
	public int totalTextSize = 0;

	public String Substring(byte side, int startPosition, int length) {
		if (side == (byte) 1)
			return text1.substring(startPosition, length);
		else
			return text2.substring(startPosition, length);

	}

	public String Substring(byte side, int startPosition) {
		if (side == (byte) 1)
			return text1.substring(startPosition);
		else
			return text2.substring(startPosition);
	}

	public RenderedTextInfo RenderedInfo(byte side) {
		return side == (byte) 1 ? renderedInfo1 : renderedInfo2;
	}

	private int recommended_natural1;
	private int recommended_natural2;

	public void IncRecommendedNatural(byte side) {
		if (side == (byte) 1)
			recommended_natural1++;
		else
			recommended_natural2++;
	}

	public void DecRecommendedNatural(byte side) {
		if (side == (byte) 1)
			recommended_natural1--;
		else
			recommended_natural2--;
	}

	public int GetRecommendedNatural(byte side) {
		if (side == (byte) 1)
			return recommended_natural1;
		else
			return recommended_natural2;
	}

	public byte structureLevel;

	public String text1;
	public String text2;

	// Indicates that Text1 begins a paragraph

	public boolean startParagraph1;

	// Indicates that Text2 begins a paragraph

	public boolean startParagraph2;

	// Current position for processing in text 1

	public int currentPos1;

	// Current position for processing in text 2

	public int currentPos2;

	// Indicates that all lines of text 1 have already been computed

	public boolean allLinesComputed1;

	// Indicates that all lines of text 2 have already been computed

	public boolean allLinesComputed2;

	// How many lines are required to be added in order to compute the start
	// Line of the Next text Pair.
	// Zero means that the Next Pair begins at the same Line.

	public int height;

	public RenderedTextInfo renderedInfo1;
	public RenderedTextInfo renderedInfo2;

	private ArrayList<WordInfo> computedWords1;
	private ArrayList<WordInfo> computedWords2;
	public boolean ContinueFromNewLine1;
    public boolean ContinueFromNewLine2;

	public char GetChar(byte side, int charIndex) {
		if (side == (byte) 1)
			return text1.charAt(charIndex);
		else
			return text2.charAt(charIndex);
	}

	public ArrayList<WordInfo> ComputedWords(byte side, boolean createNew) {
		if (side == (byte) 1) {
			if (createNew && computedWords1 == null)
				computedWords1 = new ArrayList<WordInfo>();
			return computedWords1;
		} else {
			if (createNew && computedWords2 == null)
				computedWords2 = new ArrayList<WordInfo>();
			return computedWords2;
		}
	}

	public ArrayList<WordInfo> ComputedWords(byte side) {
		return ComputedWords(side, false);
	}

	public TextPair() {
		height = -1;

		currentPos1 = 0;
		currentPos2 = 0;

		allLinesComputed1 = false;
		allLinesComputed2 = false;

		renderedInfo1 = new RenderedTextInfo();
		renderedInfo2 = new RenderedTextInfo();

	}

	public TextPair(String text1, String text2, boolean startParagraph1,
			boolean startParagraph2) {
		this();

		if (text1 != null)
			this.text1 = text1;

		if (text2 != null)
			this.text2 = text2;

		this.startParagraph1 = startParagraph1;
		this.startParagraph2 = startParagraph2;

	}

	void ClearComputedWords() {
		if (computedWords1 != null)
			computedWords1.clear();
		if (computedWords2 != null)
			computedWords2.clear();

		height = -1;

		allLinesComputed1 = false;
		allLinesComputed2 = false;
		
		currentPos1 = 0;
		currentPos2 = 0;
		
		ContinueFromNewLine1 = false;
		ContinueFromNewLine2 = false;
		
	}

	int GetLength(byte side) {
		if (side == (byte) 1)
			return text1.length();
		else
			return text2.length();
	}

	void SetStructureLevel(byte p) {
		structureLevel = p;
		startParagraph1 = true;
		startParagraph2 = true;
		
	}
	
	String GetText(byte side) {
		if (side == (byte) 1)
			return text1;
		else
			return text2;
	}

	boolean StartParagraph(byte side) {
		return side == (byte) 1 ? startParagraph1 : startParagraph2;
	}

	void UpdateTotalSize() {
		totalTextSize = text1.length() + text2.length();
	}

	public boolean AllLinesComputed(byte side) {
		return (side == (byte) 1) ? allLinesComputed1 : allLinesComputed2;
	}
}
