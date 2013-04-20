package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

public abstract class AbstractFrame {
	
    public byte side;

    public boolean visible;

    public int line1;
    public int line2;

    public float x1;
    public float x2;

    protected AbstractFrame(ArrayList<AbstractFrame> list)
    {
        if (list != null)
            list.add(this);
    }


    public void FillByRenderInfo(RenderedTextInfo renderedTextInfo, byte newSide)
    {
        if (renderedTextInfo == null
            || !renderedTextInfo.valid)
        {
            this.visible = false;
            return;
        }

        this.visible = true;

        this.side = newSide;

        this.line1 = renderedTextInfo.line1;
        this.line2 = renderedTextInfo.line2;

        this.x1 = renderedTextInfo.x1;
        this.x2 = renderedTextInfo.x2;
    }

    public abstract void Draw(ParallelTextData parallelTextData);

}
