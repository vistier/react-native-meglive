
package com.megvii.livenesslib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.megvii.livenessdetection.DetectionFrame;

public class FaceMask extends View {
    public static final int Threshold = 30;
    Paint localPaint = null;
    RectF mFaceRect = new RectF();
    RectF mDrawRect = null;
    private int normal_colour = 0xff00b4ff;
    private int high_colour = 0xffff0000;
    private boolean isFraontalCamera = true;

    public FaceMask(Context context, AttributeSet atti) {
        super(context, atti);
        mDrawRect = new RectF();
        localPaint = new Paint();
        localPaint.setColor(normal_colour);
        localPaint.setStrokeWidth(5);
        localPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFaceInfo(DetectionFrame faceInfo) {
        if (faceInfo != null)
            mFaceRect = faceInfo.getFacePos();
        else {
            mFaceRect = null;
        }
        postInvalidate();
    }

    public void setFrontal(boolean isFrontal)
    {
        this.isFraontalCamera = isFrontal;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaceRect == null)
            return;
        if (isFraontalCamera) {
            mDrawRect.set(getWidth() * (1 - mFaceRect.right), getHeight()
                    * mFaceRect.top, getWidth() * (1 - mFaceRect.left),
                    getHeight()
                            * mFaceRect.bottom);
        } else {
            mDrawRect.set(getWidth() * mFaceRect.left, getHeight() * mFaceRect.top, getWidth()
                    * mFaceRect.right, getHeight() * mFaceRect.bottom);
        }
        canvas.drawRect(mDrawRect, localPaint);
    }
}
