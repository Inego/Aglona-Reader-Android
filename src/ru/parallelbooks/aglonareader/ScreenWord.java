package ru.parallelbooks.aglonareader;

public class ScreenWord {
	
	public String word;
    public float x1; // start of the Word -- real point on screen
    public float x2; // end of the Word
    public int pairIndex; // index of Pair
    public byte side; // 1 or 2 -- the second or first text
    public int pos; // position of the Word in the Pair
    public float fX1;
    public float fX2;
    public int line;

    public ScreenWord Prev;
    public ScreenWord Next;

}
