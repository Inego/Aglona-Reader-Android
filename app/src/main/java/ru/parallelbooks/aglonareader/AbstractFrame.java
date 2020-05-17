package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

abstract class AbstractFrame {
	
    public byte side;

    public boolean visible;

    public int line1;
    public int line2;

    public float x1;
    public float x2;

    AbstractFrame(ArrayList<AbstractFrame> list)
    {
        if (list != null)
            list.add(this);
    }


// --Commented out by Inspection START (08/20/15 7:38 PM):
//    public void FillByRenderInfo(RenderedTextInfo renderedTextInfo, byte newSide)
//    {
//        if (renderedTextInfo == null
//            || !renderedTextInfo.valid)
//        {
//            this.visible = false;
//            return;
//        }
//
//        this.visible = true;
//
//        this.side = newSide;
//
//        this.line1 = renderedTextInfo.line1;
//        this.line2 = renderedTextInfo.line2;
//
//        this.x1 = renderedTextInfo.x1;
//        this.x2 = renderedTextInfo.x2;
//    }
// --Commented out by Inspection STOP (08/20/15 7:38 PM)

    public abstract void Draw(ParallelTextData parallelTextData);

}
