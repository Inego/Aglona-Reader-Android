package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

public class PopUpInfo {
	
	public float X;
    public int Y;
    public boolean visible;
    public int offsetY;
    public float X2;
    public int Y2;
    public ArrayList<WordInfo> words;
    
    public PopUpInfo()
    {
        X = -1;
        Y = -1;
        offsetY = 0;

        visible = false;
    }

}
