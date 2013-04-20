package ru.parallelbooks.aglonareader;

public class WordInfo {
	
	public String word;
    public int line;
    public float x1;
    public float x2;
    public int pos;
    public boolean eastern;

    public WordInfo(String word, int line, float newStart, float f, int pos, boolean eastern)
    {
        this.word = word;
        this.line = line;
        this.x1 = newStart;
        this.x2 = f;
        this.pos = pos;
        this.eastern = eastern;
    }

}
