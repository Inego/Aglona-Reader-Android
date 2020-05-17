package ru.parallelbooks.aglonareader;

class WordInfo {
	
	public final String word;
    public int line;
    public final float x1;
    public final float x2;
    public final int pos;
    public final boolean eastern;

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
