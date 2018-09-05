package com.example.gesture_password_study.gesture_pwd.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.example.gesture_password_study.R;


/**
 * 手势密码专用的圆形控件
 */
public class GestureLockCircleView extends View {

    public GestureLockCircleView(Context context) {
        this(context, null);
    }

    public GestureLockCircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dealAttr(context, attrs);
        initPaint();
    }

    private void dealAttr(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GestureLockCircleView);

        if (ta != null) {
            try {
                circleFillColor = ta.getColor(R.styleable.GestureLockCircleView_gestureCircleFillColor, 0x00FE6665);
                circleRadius = ta.getDimension(R.styleable.GestureLockCircleView_gestureCircleRadius, 0);

                hasRoundBorder = ta.getBoolean(R.styleable.GestureLockCircleView_hasRoundBorder, false);
                roundBorderColor = ta.getColor(R.styleable.GestureLockCircleView_roundBorderColor, 0x00FE6665);
                roundBorderWidth = ta.getDimension(R.styleable.GestureLockCircleView_roundBorderWidth, 0);
            } catch (Exception e) {

            } finally {
                ta.recycle();
            }
        }
    }


    private int minWidth = 50, minHeight = 50;

    /**
     * 重写onMeasure设定最小宽高
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(minWidth, minHeight);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(minWidth, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, minHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        float centerX = width / 2;
        float centerY = height / 2;

        if (hasRoundBorder) {
            canvas.drawCircle(centerX, centerY, roundBorderWidth, paint_border);
        }
        canvas.drawCircle(centerX, centerY, circleRadius, paint_inner);

    }

    private Paint paint_inner, paint_border;


    private boolean hasRoundBorder;
    private int roundBorderColor;
    private float roundBorderWidth;

    /**
     * 设置内圈的颜色和半径
     *
     * @param circleFillColor
     * @param circleRadius
     */
    public void setInnerCircle(int circleFillColor, float circleRadius) {
        this.circleFillColor = circleFillColor;
        this.circleRadius = circleRadius;
        initPaint();
        postInvalidate();
    }

    public void setBorderRound(boolean hasRoundBorder, int roundBorderColor, float roundBorderWidth) {
        this.hasRoundBorder = hasRoundBorder;
        this.roundBorderColor = roundBorderColor;
        this.roundBorderWidth = roundBorderWidth;
        initPaint();
        postInvalidate();
    }


    private int circleFillColor;
    private float circleRadius;

    private void initPaint() {
        paint_inner = new Paint();
        paint_inner.setColor(circleFillColor);
        paint_inner.setAntiAlias(true);//抗锯齿
        paint_inner.setStyle(Paint.Style.FILL);//FILL填充,stroke描边

        paint_border = new Paint();
        paint_border.setColor(roundBorderColor);
        paint_border.setAntiAlias(true);//抗锯齿
        paint_border.setStyle(Paint.Style.FILL);//FILL填充,stroke描边
    }

    //3个状态
    public static final int STATUS_NOT_CHECKED = 0x01;
    public static final int STATUS_CHECKED = 0x02;
    public static final int STATUS_CHECKED_ERR = 0x03;

    public void switchStatus(int status) {
        switch (status) {
            case STATUS_CHECKED:
                circleFillColor = getResources().getColor(R.color.colorChecked);
                roundBorderColor = getResources().getColor(R.color.colorRoundBorder);
                break;
            case STATUS_CHECKED_ERR:
                circleFillColor = getResources().getColor(R.color.colorCheckedErr);
                roundBorderColor = getResources().getColor(R.color.colorRoundBorderErr);
                break;
            case STATUS_NOT_CHECKED:// 普通状态
            default://以及缺省状态
                //没有外框,内圈为灰色
                circleFillColor = getResources().getColor(R.color.colorNotChecked);
                roundBorderColor = getResources().getColor(R.color.transparent);
                break;
        }
        initPaint();
        postInvalidate();
    }

}


