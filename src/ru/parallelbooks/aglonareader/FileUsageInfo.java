package ru.parallelbooks.aglonareader;

import java.io.Serializable;

public class FileUsageInfo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6905929892933347105L;
	public String FileName;
    public int TopPair;
    public boolean Reversed;
    public float SplitterRatio;
    public int layoutMode;

}
