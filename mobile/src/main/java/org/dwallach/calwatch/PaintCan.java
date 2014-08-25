package org.dwallach.calwatch;

import android.graphics.Paint;
import android.util.SparseArray;

import java.util.HashMap;

/**
 * Cheesy helper for getting Paint values for calendar events and making sure we don't allocate
 * the same color twice.
 * Created by dwallach on 8/15/14.
 */
public class PaintCan {
    private static SparseArray<Paint> map = null;

    public static Paint getPaint(int argb) {
        Paint retPaint;
        Integer argbInt = argb;

        if(map == null)
            map = new SparseArray<Paint>();

        retPaint = map.get(argbInt);
        if(retPaint == null) {
            retPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            retPaint.setStrokeJoin(Paint.Join.BEVEL);
            retPaint.setColor(argb);
            retPaint.setStyle(Paint.Style.FILL);

            map.put(argbInt, retPaint);
        }

        return retPaint;
    }
}
