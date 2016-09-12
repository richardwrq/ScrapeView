package org.rc.scrapeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;


/**
 * Description:
 * Author:       WuRuiqiang (263454190@qq.com)
 * CreateDate:   2016/9/11-16:06
 * UpdateUser:
 * UpdateDate:
 * UpdateRemark:
 * Version:      [v1.0]
 */
public class ScrapeView extends View {

    private static final String TAG = "ScrapeView";

    //擦除画笔的大小
    private float eraseSize = DEFAULT_ERASE_SIZE;
    //擦除面积百分比，超过这个百分比自动将剩余部分擦除
    private int maxPercent = DEFAULT_MAX_PERCENT;
    //遮罩层
    private int maskResId;
    private Bitmap mMaskBitmap;
    //水印
    private BitmapDrawable mWatermark;
    //擦除画笔
    private Paint mErasePaint;
    private Paint mBitmapPaint;
    private Canvas mCanvas;
    private Path mErasePath;
    private int maskWidth;
    private int maskHeight;

    private int[] bitmapPixels;

    private int mTouchSlop;
    private float startX;
    private float startY;
    //上一个move事件触发的时间
    private long lastMoveEventTime;
    //两个move事件超过这个临界值才调用erase函数，节省资源 单位：毫秒
    private long moveEventTimeCritical = 300;

    private volatile boolean isClear;

    private EraseCallBack eraseCallBack;
    private ComputeScrapedAreaRunnable computeRunnable;

    private static final int DEFAULT_ERASE_SIZE = 60;
    private static final int DEFAULT_MAX_PERCENT = 40;

    public ScrapeView(Context context) {
        this(context, null);
    }

    public ScrapeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrapeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrapeView);

        eraseSize = typedArray.getDimensionPixelOffset(
                R.styleable.ScrapeView_EraseSize,
                DEFAULT_ERASE_SIZE);
        maxPercent = typedArray.getInteger(R.styleable.ScrapeView_MaxPercent, DEFAULT_MAX_PERCENT);
        maskResId = typedArray.getResourceId(R.styleable.ScrapeView_MaskLayer, R.color.default_mask);
        mWatermark = (BitmapDrawable) typedArray.getDrawable(R.styleable.ScrapeView_WaterMask);
        typedArray.recycle();

        init();
    }

    private void init() {
        mErasePath = new Path();
        mErasePaint = new Paint();
        computeRunnable = new ComputeScrapedAreaRunnable();

        mErasePaint.setAntiAlias(true);
        mErasePaint.setDither(true);
        mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//设置擦除效果
        mErasePaint.setStyle(Paint.Style.STROKE);
        mErasePaint.setStrokeCap(Paint.Cap.ROUND);//设置笔尖形状，让绘制的边缘圆滑
        setEraseSize(eraseSize);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setDither(true);
        mBitmapPaint.setColor(Color.TRANSPARENT);

        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mMaskBitmap, getPaddingLeft(), getPaddingTop(), null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maskWidth = w - getPaddingLeft() - getPaddingRight();
        maskHeight = h - getPaddingTop() - getPaddingBottom();
        setMask(maskResId);
        if (w != oldw && h != oldh) {
            Log.d(TAG, "init bitmapPixels");
            bitmapPixels = new int[maskWidth * maskHeight];
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startErase(event.getX(), event.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                erase(event.getX(), event.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                stopErase();
                invalidate();
                Log.d(TAG, "move interval:" + (event.getEventTime() - lastMoveEventTime));
                if (!isClear && event.getEventTime() - lastMoveEventTime >= moveEventTimeCritical) {
                    new Thread(computeRunnable).start();
                }
                lastMoveEventTime = event.getEventTime();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void startErase(float x, float y) {
        mErasePath.reset();
        mErasePath.moveTo(x, y);
        mCanvas.drawPoint(x, y, mErasePaint);
        this.startX = x;
        this.startY = y;
    }

    private void erase(float x, float y) {
        int dx = (int) Math.abs(x - startX);
        int dy = (int) Math.abs(y - startY);
        if (dx >= mTouchSlop || dy >= mTouchSlop) {
            this.startX = x;
            this.startY = y;

            mErasePath.lineTo(x, y);
            mCanvas.drawPath(mErasePath, mErasePaint);

            mErasePath.reset();
            mErasePath.moveTo(startX, startY);
        }
    }

    private void stopErase() {
        this.startX = 0;
        this.startY = 0;
        mErasePath.reset();
    }

    /**\
     * 设置橡皮檫尺寸大小（默认大小是 60）
     *
     * @param eraserSize 橡皮檫尺寸大小
     */
    public void setEraseSize(float eraserSize) {
        this.eraseSize = eraserSize;
        mErasePaint.setStrokeWidth(eraserSize);
    }

    public void setMask(int resId) {
        if (resId == -1) {
            return;
        }

        Drawable d = getResources().getDrawable(resId);

        if (d == null) {
            return;
        }
        if (mMaskBitmap != null) {
            mMaskBitmap.recycle();
            mMaskBitmap = null;
        }
        BitmapDrawable bd = (BitmapDrawable) d;
        mMaskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mMaskBitmap);
        Rect rect = new Rect(0, 0, maskWidth, maskHeight);
        mCanvas.drawRect(rect, mBitmapPaint);
        bd.setBounds(0, 0, maskWidth, maskHeight);
        bd.draw(mCanvas);

        if (mWatermark != null) {
            mWatermark.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            Rect bounds = new Rect(rect);
            mWatermark.setBounds(bounds);
            mWatermark.draw(mCanvas);
        }
    }

    /**
     * 设置水印
     * @param resId
     */
    public void setWaterMask(int resId) {
        if (resId == -1) {
            mWatermark = null;
        } else {
            Drawable drawable = getResources().getDrawable(resId);
            if (drawable == null) {
                return;
            }
            if (mWatermark != null) {
                mWatermark.getBitmap().recycle();
            }
            mWatermark = (BitmapDrawable) drawable;
            mWatermark.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            Rect rect = new Rect(0, 0, maskWidth, maskHeight);
            Rect bounds = new Rect(rect);
            mWatermark.setBounds(bounds);
            mWatermark.draw(mCanvas);
            invalidate();
        }
    }

    public void setMaxPercent(int maxPercent) {
        if (maxPercent >= 0 || maxPercent <= 100) {
            this.maxPercent = maxPercent;
        }
    }

    public void reset() {
        isClear = false;
        setMask(maskResId == 0 ? R.color.default_mask : maskResId);
        setVisibility(VISIBLE);
        invalidate();
    }

    public void clear() {
        isClear = true;
        if (mMaskBitmap != null) {
            mMaskBitmap.recycle();
            mMaskBitmap = null;
        }
        mMaskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mMaskBitmap);
        mCanvas.drawRect(new Rect(0, 0, maskWidth, maskHeight), mBitmapPaint);
        invalidate();
        setVisibility(GONE);
    }

    public void setEraseCallBack(EraseCallBack eraseCallBack) {
        this.eraseCallBack = eraseCallBack;
    }

    /**
     * 计算已擦除部分面积占总面积的百分比
     */
    private class ComputeScrapedAreaRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "ComputeScrapedArea Start");
            mMaskBitmap.getPixels(bitmapPixels, 0, mMaskBitmap.getWidth(), 0, 0, mMaskBitmap.getWidth(),  mMaskBitmap.getHeight());

            float erasePixels = 0;
            float totalPixels = bitmapPixels.length;
            //统计
            for (int i = 0; i < totalPixels; i++) {
                if (isClear) {
                    return;
                }
                if (bitmapPixels[i] == 0) {
                    erasePixels++;
                }
            }

            final int percent;
            if (erasePixels > 0 && totalPixels > 0) {
                percent = Math.round(erasePixels * 100 / totalPixels);
            } else {
                percent = 0;
            }
            Log.d(TAG, "ScrapedArea Percent:" + percent);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (eraseCallBack != null) {
                        eraseCallBack.erasing(percent);
                        if (percent >= maxPercent) {
                            eraseCallBack.erased();
                            isClear = true;
                        }
                    } else if (percent >= maxPercent) {
                        clear();
                    }
                }
            });

        }
    }

    public interface EraseCallBack {
        void erasing(int percent);

        void erased();
    }

}
