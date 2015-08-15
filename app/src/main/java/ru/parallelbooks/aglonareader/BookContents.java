package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

public class BookContents {
	
	class Chapter {
		public int pair;
		public int level;
	}
	
	ArrayList<Chapter> chapters;
	
	public BookContents() {
		
		chapters = new ArrayList<Chapter>();
		
	}

	public void Clear() {
		
		chapters.clear();
		
	}

	public void add(int pair, int level) {

		Chapter c = new Chapter();
		c.pair = pair;
		c.level = level;
		
		chapters.add(c);
		
	}

	public int size() {
		return chapters.size();
	}

	public Chapter get(int i) {

		return chapters.get(i); 
	}

}
