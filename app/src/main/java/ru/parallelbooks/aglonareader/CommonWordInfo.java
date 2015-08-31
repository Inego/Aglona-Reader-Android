package ru.parallelbooks.aglonareader;

class CommonWordInfo extends WordInfo {
	
	public final TextPair textPair;
	public final byte side;

    public CommonWordInfo(TextPair textPair, String word, int line, float newStart, float f, int pos, boolean eastern, byte side)
    {
    	super(word, line, newStart, f, pos, eastern);
    	
        this.textPair = textPair;
        this.side = side;

    }

}
