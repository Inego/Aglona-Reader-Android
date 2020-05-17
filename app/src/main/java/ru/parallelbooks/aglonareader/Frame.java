package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

public class Frame extends AbstractFrame {
	
	public Pen framePen;

    public Frame(Pen pen, ArrayList<AbstractFrame> list)
    {
    	super(list);
    	
        visible = false;
        if (pen != null)
            this.framePen = pen;
        if (list != null)
            list.add(this);
    }

    public void Draw(ParallelTextData parallelTextControl)
    {
        if (parallelTextControl != null)
            parallelTextControl.DrawFrame(this);
    }
    
    

}
