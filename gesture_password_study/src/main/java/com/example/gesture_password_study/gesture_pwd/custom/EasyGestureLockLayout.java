package com.example.gesture_password_study.gesture_pwd.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.gesture_password_study.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 手势密码绘制 控件;
 */
public class EasyGestureLockLayout extends RelativeLayout {

    //全局变量统一管理
    private Context mContext;
    private boolean hasRoundBorder;//按键是否允许有圆环外圈
    private boolean ifAllowInteract;//是否允许有事件交互
    private Paint currentPaint;//当前使用的画笔
    private Paint paint_correct, paint_error;//画线用的两种颜色的画笔
    private GestureLockCircleView[] gestureCircleViewArr = null;//用数组来保存所有按键
    private int mCount = 4;// 方阵的行数(列数等同)
    private int mGesturePasswordViewWidth;//每一个按键的边长（因为宽高相同）
    private int mWidth, mHeight;//本layout的宽高
    private int childStartIndex, childEndIndex;//画轨迹线(密码轨迹)的时候，需要指定子的起始和结束 index
    private float marginRate = 0.2f;//缩小MotionEvent到达时的密码键选中的判定范围，这里的0.2的意思是，原本10*10的判定范围，现在，缩小到6*6,其他4，被两头平分
    private boolean ifAllowDrawLockPath = false;//因为有可能存在，down的时候没有点在任何一个键位的范围之内，所以必须用这个变量来控制是否进行绘制
    private int guideLineStartX, guideLineStartY, guideLineEndX, guideLineEndY;//引导线(正在画手势，但是尚未或者无法形成轨迹线的时候，会出现)的起始和终止坐标
    private int downX, downY;//MotionEvent的down事件坐标
    private int movedX, movedY;//MotionEvent的move事件坐标
    private Path lockPath = new Path();//密码的图形路径.用于绘制轨迹线
    private List<Integer> lockPathArr;//手势密码路径，用于输出到外界以及核对密码
    private int minLengthOfPwd = 4;//密码最少位数

    private int mModeStatus = -1;
    private List<Integer> checkPwd;//外界传入的需要核对的密码
    private int maxAttemptTimes = 5;//允许解锁的最大尝试次数,有必要的话，给他设置一个set方法，或者弄一个自定义属性
    private int currentAttemptTime = 1;// 当前尝试次数

    private int resetCurrentTime = 0;//当用户重新设置密码，这个值将会被重置
    private List<Integer> tempPwd;//用于重新设置密码
    private boolean ifCheckOnErr = false;//当前是否检测密码曾失败过

    //常量
    public static final int STATUS_RESET = 0x01;//本类状态：重新设置，此状态下会允许用户绘制两次手势，而且必须相同，绘制完成之后，返回密码值出去；
    // 如果第二次绘制和第一次绘制不同，则强制重新绘制
    public static final int STATUS_CHECK = 0x02;//本类状态：校验密码，此状态下，要求外界传入密码，然后给予用户若干尝试解锁的次数，
    // 如果规定次数之内，密码相同，则返回解锁成功；
    // 如果规定次数之内，都没有绘制出正确密码，则返回解锁失败；

    //************* 构造函数 *****************************
    public EasyGestureLockLayout(Context context) {
        this(context, null);
    }

    public EasyGestureLockLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasyGestureLockLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dealAttr(context, attrs);
        init(context);
    }

    //************* 属性值获取 *****************************
    private void dealAttr(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EasyGestureLockLayout);

        if (ta != null) {
            try {
                hasRoundBorder = ta.getBoolean(R.styleable.EasyGestureLockLayout_ifChildHasBorder, false);
                mCount = ta.getInteger(R.styleable.EasyGestureLockLayout_count, 3);

                ifAllowInteract = ta.getBoolean(R.styleable.EasyGestureLockLayout_ifAllowInteract, false);
            } catch (Exception e) {

            } finally {
                ta.recycle();
            }
        }
    }

    //************* 重写方法 *****************************
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        //取测量之后的宽和高
        mWidth = MeasureSpec.getSize(widthSpec);
        mHeight = MeasureSpec.getSize(heightSpec);
        //强行将绘图使用的宽高置为  测量宽高中的较小值, 因为绘图不能超出边界
        mHeight = mWidth = mWidth < mHeight ? mWidth : mHeight;

        // 初始化mGestureLockViews
        if (gestureCircleViewArr == null) {
            gestureCircleViewArr = new GestureLockCircleView[mCount * mCount];//用数组来保存 “按键”
            mGesturePasswordViewWidth = mWidth / mCount;//等分，不需要留间隙, 因为圆形控件会自己留空隙

            //利用相对布局的参数来放置子元素
            for (int i = 0; i < gestureCircleViewArr.length; i++) {
                //初始化每个GestureLockView
                gestureCircleViewArr[i] = getCircleView(mHeight);
                gestureCircleViewArr[i].setId(i + 1);
                LayoutParams lockerParams = new LayoutParams(
                        mGesturePasswordViewWidth, mGesturePasswordViewWidth);

                // 不是每行的第一个，则设置位置为前一个的右边
                if (i % mCount != 0) {
                    lockerParams.addRule(RelativeLayout.RIGHT_OF,
                            gestureCircleViewArr[i - 1].getId());
                }
                // 从第二行开始，设置为上一行同一位置View的下面
                if (i > mCount - 1) {
                    lockerParams.addRule(RelativeLayout.BELOW,
                            gestureCircleViewArr[i - mCount].getId());
                }
                lockerParams.setMargins(0, 0, 0, 0);
                addView(gestureCircleViewArr[i], lockerParams);
            }
        }

    }

    /**
     * 实验结果，在这里onDraw，绘制出来的线，总是会被子元素覆盖,
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) { //闹半天，这个onDraw没有执行
        super.onDraw(canvas);
        //奇怪，为何不执行onDraw
        // 一般情况下，viewGroup都不会执行onDraw，因为它本身是一个容器，容器不具有自我绘制功能；
        //图像的表现，和绘制的顺序有关系;
        Log.d("onDrawTag", "onDraw");
    }

    /**
     * 然而，由这个方法进行绘制，线，则会覆盖"子";
     *
     * @param canvas
     */
    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);//这一步居然就是绘制 “子”, 具体看View.java 的 19195行
        Log.d("onDrawTag", "dispatchDraw");//那么， 等children画完了之后，再画线，就名正言顺了。⊙︿⊙ 一头包。明白了
        if (gestureCircleViewArr != null && ifAllowInteract) {
            drawLockPath(canvas);
            drawMovingPath(canvas);
        }
    }

    //************* 模式设置 *****************************

    public int getCurrentMode() {
        return mModeStatus;
    }

    /**
     * 切换到Reset模式，重新设置手势密码；
     * 此模式下，不需要入参。设置完成之后，会执行回调GestureEventCallback.onResetFinish(pwd);
     */
    public void switchToResetMode() {
        mModeStatus = STATUS_RESET;
    }

    /**
     * 切换到 校验模式；
     * 这个模式需要传入原始密码，以及最大尝试的次数；
     * <p>
     * 尝试解锁成功，或者超过了最大尝试次数都没有成功，就会执行回调GestureEventCallback.onCheckFinish(boolean succeedOrFailed);
     *
     * @param pwd
     * @param maxAttemptTimes
     */
    public void switchToCheckMode(List<Integer> pwd, int maxAttemptTimes) {
        if (pwd == null || maxAttemptTimes <= 0) {
            Log.e("switchToCheckMode", "参数错误,pwd不能为空，而且 maxAttemptTimes必须大于0");
            return;
        }
        this.currentAttemptTime = 1;
        this.mModeStatus = STATUS_CHECK;
        this.maxAttemptTimes = maxAttemptTimes;
        this.checkPwd = copyPwd(pwd);
    }

    //****************************以下全是业务代码**************************
    private int background_color = 0xff4790FF;
    private int background_color_transparent = 0x00000000;

    /**
     * 初始化画笔，
     *
     * @param context
     */
    private void init(Context context) {
        mContext = context;
        setClickable(true);//为了顺利接收事件，需要开启click;因为你如果不设置，，就只能收到down，其他的一概收不到
        setBackgroundColor(background_color_transparent);//设置透明色；这里如果不设置，onDraw将不会执行；原因:这是一个ViewGroup，本身是容器，不具备自我绘制功能，但是这里设置了背景色，就说明有东西需要绘制，onDraw就会执行；

        paint_correct = new Paint();
        paint_correct.setStyle(Paint.Style.STROKE);
        paint_correct.setAntiAlias(true);
        paint_correct.setColor(getResources().getColor(R.color.colorChecked));

        paint_error = new Paint();
        paint_error.setStyle(Paint.Style.STROKE);
        paint_error.setAntiAlias(true);
        paint_error.setColor(getResources().getColor(R.color.colorCheckedErr));

        initLockPathArr();
        currentPaint = paint_correct;// 默认使用的画笔
    }

    /**
     * 构建单个圆
     *
     * @param wh 边长
     * @return
     */
    private GestureLockCircleView getCircleView(int wh) {
        GestureLockCircleView gestureCircleView = new GestureLockCircleView(mContext);

        double s = Math.pow(mCount, 3) + 0.5f;//除法系数，用于计算内圆的半径； 行数的3次方，并且转为浮点型
        gestureCircleView.setInnerCircle(getResources().getColor(R.color.colorChecked), (float) (wh / s));

        paint_correct.setStrokeWidth((float) (wh / s) * 0.2f);
        paint_error.setStrokeWidth((float) (wh / s) * 0.2f);

        //内圆颜色，内圆半径
        s = Math.pow(mCount, 2) + 0.5f;//除法系数，用于计算外圆的半径;行数的2次方，并且转为浮点型
        gestureCircleView.setBorderRound(hasRoundBorder, getResources().getColor(R.color.colorChecked), (float) (wh / s));//是否有边框，外圆颜色，外圆半径
        gestureCircleView.switchStatus(GestureLockCircleView.STATUS_NOT_CHECKED);
        return gestureCircleView;
    }

    /**
     * 重置所有按键为 notChecked 状态
     */
    private void resetAllCircleBtn() {
        if (gestureCircleViewArr == null) return;
        for (int i = 0; i < gestureCircleViewArr.length; i++) {
            gestureCircleViewArr[i].switchStatus(GestureLockCircleView.STATUS_NOT_CHECKED);
        }
    }

    //*************************手势密码路径的管理***********************************************
    private void initLockPathArr() {
        lockPathArr = new ArrayList<>();
    }

    /**
     * 增加一个密码数字
     *
     * @param p
     */
    private void addPwd(int p) {
        if (!checkRepetition(p)) {
            lockPathArr.add(p);
        }
    }

    private void resetPwd() {
        if (lockPathArr == null)
            lockPathArr = new ArrayList<>();
        else
            lockPathArr.clear();
    }

    /**
     * 绘制密码“轨迹线”
     *
     * @param canvas
     */
    private void drawLockPath(Canvas canvas) {
        canvas.drawPath(lockPath, currentPaint);
    }

    /**
     * 重置引导线的起/终 坐标值
     */
    private void resetMovingPathCoordinate() {
        guideLineStartX = 0;
        guideLineStartY = 0;
        guideLineEndX = 0;
        guideLineEndY = 0;
    }

    /**
     * 绘制引导线
     */
    private void drawMovingPath(Canvas canvas) {
        if (guideLineStartX != 0 && guideLineStartY != 0)//只有当起始位置不是0的时候，才进行绘制
            canvas.drawLine(guideLineStartX, guideLineStartY, guideLineEndX, guideLineEndY, currentPaint);
    }

    /**
     * 辅助方法，获得一个View的中心位置
     *
     * @param v
     * @return
     */
    private Point getCenterPoint(View v) {
        Rect rect = new Rect();
        v.getHitRect(rect);
        int x = rect.left + v.getWidth() / 2;
        int y = rect.top + v.getHeight() / 2;
        return new Point(x, y);
    }

    /**
     * 判断当前点击的点位置是不是在子元素范围之内
     *
     * @param x
     * @param y
     * @param v
     * @return
     */
    private boolean ifClickOnView(int x, int y, View v) {
        Rect r = new Rect();
        v.getHitRect(r);

        //判定点是不是在view范围内，根据业务需求，要给view一个判定的间隙，比如 5*5的View,判定范围只能是3*3
        //以原来的矩阵为基础，重新定一个判定范围,范围暂时定位原来的80%
        //真正的判定区域的矩阵范围

        int w = v.getWidth();
        int h = v.getHeight();

        int realLeft = (int) (r.left + marginRate * w);
        int realTop = (int) (r.top + marginRate * h);
        int realRight = (int) (r.right - marginRate * w);
        int realBottom = (int) (r.bottom - marginRate * h);

        Rect rect1 = new Rect(realLeft, realTop, realRight, realBottom);

        if (rect1.contains(x, y)) {
            return true;
        }
        return false;
    }

    /**
     * 根据点坐标，返回当前点在哪个密码键的范围内,直接返回View对象
     *
     * @param x
     * @param y
     * @return
     */
    private GestureLockCircleView getClickedChild(int x, int y) {
        for (GestureLockCircleView v : gestureCircleViewArr) {
            if (ifClickOnView(x, y, v)) {//
                return v;
            }
        }
        return null;
    }

    /**
     * 根据点坐标，返回当前点在哪个密码键的范围内,直接返回View对象的id
     *
     * @param x
     * @param y
     * @return
     */
    private int getClickedChildIndex(int x, int y) {
        for (int i = 0; i < gestureCircleViewArr.length; i++) {
            View v = gestureCircleViewArr[i];
            if (ifClickOnView(x, y, v)) {//
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查密码值是否重复
     *
     * @return
     */
    private boolean checkRepetition(int pwd) {
        return lockPathArr.contains(pwd);
    }

    /**
     * 手势绘制
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ifAllowInteract)//只有设置了允许事件交互，才往下执行
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onToast("", ColorHolder.COLOR_GRAY);
                    downX = (int) event.getX();
                    downY = (int) event.getY();
                    ifAllowDrawLockPath = false;
                    GestureLockCircleView current = getClickedChild(downX, downY);
                    if (current != null) {//如果当前按下的点，没有在任何一个按键范围之内
                        ifAllowDrawLockPath = true;

                        if (ifCheckOnErr)
                            current.switchStatus(GestureLockCircleView.STATUS_CHECKED_ERR);
                        else
                            current.switchStatus(GestureLockCircleView.STATUS_CHECKED);//down的时候，将当前这个按键设置为checked

                        childStartIndex = getClickedChildIndex(downX, downY);
                        //记录手势密码
                        lockPath.reset();
                        resetPwd();
                        addPwd(childStartIndex);
                        //path处理
                        Point startP = getCenterPoint(gestureCircleViewArr[childStartIndex]);
                        if (startP != null) {//因为如果
                            lockPath.moveTo(startP.x, startP.y);
                            //引导线的起始坐标
                            guideLineStartX = startP.x;
                            guideLineStartY = startP.y;
                        } else {
                            Log.d("tagpx", "1");
                        }
                    } else {
                        //如果第一次点下去，就是在 键位的空隙里面。那么，就不用绘制了
                        Log.d("tagpx", "2");
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (ifAllowDrawLockPath) {
                        movedX = (int) event.getX();
                        movedY = (int) event.getY();
                        childEndIndex = getClickedChildIndex(movedX, movedY);

                        //-1表示没有找到对应的区域
                        boolean flag1 = childStartIndex != -1 && childEndIndex != -1;//没有获取到正确的对应区域
                        boolean flag2 = childStartIndex != childEndIndex;//在同一个区域内不需要画线
                        boolean flag3 = checkRepetition(childEndIndex);//不允许密码值重复,这里要检查当前这个区域是不是已经在lockPathArr里面

                        if (flag1 && flag2 && !flag3) {//如果起点终点都在区域之内，那么就直接绘制“轨迹线”
                            Point endP = getCenterPoint(gestureCircleViewArr[childEndIndex]);
                            GestureLockCircleView cur = getClickedChild(movedX, movedY);
                            if (ifCheckOnErr)
                                cur.switchStatus(GestureLockCircleView.STATUS_CHECKED_ERR);
                            else
                                cur.switchStatus(GestureLockCircleView.STATUS_CHECKED);

                            addPwd(childEndIndex);
                            lockPath.lineTo(endP.x, endP.y);

                            guideLineStartX = endP.x;
                            guideLineStartY = endP.y;
                        }
                        guideLineEndX = movedX;
                        guideLineEndY = movedY;
                        postInvalidate();//刷新视图
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (ifAllowDrawLockPath) {
                        resetMovingPathCoordinate(); // up的时候，要清除引导线
                        lockPath.reset(); //同时要清除轨迹线
                        postInvalidate();//刷新本layout
                        resetAllCircleBtn();//up的时候，把所有按键全部设置为notChecked，
                        onSwipeFinish();
                        if (lockPathArr.size() >= minLengthOfPwd) {
                            if (mModeStatus == STATUS_RESET) {//如果处于reset模式下，执行rest的回调
                                onReset();
                            } else if (mModeStatus == STATUS_CHECK) {//检查模式下，执行onCheck
                                onCheck();
                            } else {
                                throw new RuntimeException("异常模式，请正确调用switchToCheckMode/switchToResetMode!");
                            }
                        } else {
                            onToast(String.format(ToastStrHolder.swipeTooLittlePointStr, minLengthOfPwd), ColorHolder.COLOR_RED);
                        }
                    }
                    break;
                default:
                    break;
            }
        return super.onTouchEvent(event);
    }

    private void onSwipeFinish() {
        if (mGestureEventCallback == null) return;
        mGestureEventCallback.onSwipeFinish(copyPwd(lockPathArr));
    }

    private void onReset() {
        if (mGestureEventCallback == null) return;
        if (resetCurrentTime == 0) {//第一次绘制，赋值给tempPwd
            tempPwd = copyPwd(lockPathArr);
            resetCurrentTime++;
            onToast(ToastStrHolder.tryAgainStr, ColorHolder.COLOR_GRAY);
        } else {
            try {
                boolean s = compare(tempPwd, lockPathArr);
                if (s) {
                    onToast(ToastStrHolder.successStr, ColorHolder.COLOR_GRAY);
                    mGestureEventCallback.onResetFinish(copyPwd(lockPathArr));//执行回调
                } else {
                    onToast(ToastStrHolder.notSameStr, ColorHolder.COLOR_RED);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化当前的绘制次数
     */
    public void initCurrentTimes() {
        resetCurrentTime = 0;
    }

    private void onCheck() {
        if (mGestureEventCallback == null) return;
        boolean compareRes = compare(checkPwd, lockPathArr); //对比当前密码和外界传入的密码
        if (currentAttemptTime <= maxAttemptTimes) {//如果还能继续尝试解锁，那么
            if (compareRes) {//如果成功
                mGestureEventCallback.onCheckFinish(compareRes);//直接返回结果

                currentAttemptTime = 1;
                currentPaint = paint_correct;
                ifCheckOnErr = false;
            } else {//否则，提示
                int remindTime = maxAttemptTimes - currentAttemptTime;
                if (remindTime > 0) {
                    onToast(String.format(ToastStrHolder.wrongPwdInputStr, remindTime), ColorHolder.COLOR_RED);

                    currentPaint = paint_error;
                    ifCheckOnErr = true;
                } else {
                    mGestureEventCallback.onCheckFinish(compareRes);//直接返回结果
                }
                currentAttemptTime++;
            }
        } else {//如果已经不能尝试， 无论是否成功，都要返回结果
            mGestureEventCallback.onCheckFinish(compareRes);
            currentAttemptTime = 1;
        }
    }

    private void onSwipeMore() {
        if (mGestureEventCallback == null) return;
        mGestureEventCallback.onSwipeMore();
    }

    private void onToast(String s, int color) {
        if (mGestureEventCallback == null) return;
        mGestureEventCallback.onToast(s, color);
    }

    /**
     * 提供一个方法，绘制密码点，但是只绘制 圆圈，不绘制引导线和轨迹线
     */
    public void refreshPwdKeyboard(List<Integer> pwd) {
        try {
            for (int i = 0; i < mCount * mCount; i++) {//先把所有的点都设置为notChecked
                gestureCircleViewArr[i].switchStatus(GestureLockCircleView.STATUS_NOT_CHECKED);
            }

            if (null != pwd)
                for (int i = 0; i < pwd.size(); i++) {//再把密码中的点，设置为checked
                    gestureCircleViewArr[pwd.get(i)].switchStatus(GestureLockCircleView.STATUS_CHECKED);
                }
        } catch (IndexOutOfBoundsException e) {
            //这里有可能发生数组越界，因为 本类的各个对象时相互独立的，方阵行数可能不同
            e.printStackTrace();
        }
    }

    //*************************下面业务对接***********************************************
    public interface GestureEventCallback {
        /**
         * 当滑动结束，无论模式，只要滑动之后发现upEvent就执行
         */
        void onSwipeFinish(List<Integer> pwd);

        /**
         * 当重新设置密码成功的时候，将密码返回出去
         *
         * @param pwd 设置的密码
         */
        void onResetFinish(List<Integer> pwd);

        /**
         * 如果当前模式是 check模式，则用这个方法来返回check的结果
         *
         * @param succeedOrFailed 校验是否成功
         */
        void onCheckFinish(boolean succeedOrFailed);

        /**
         * 如果当前滑动的密码格子数太少(比如设置了至少滑动4格，却只滑了2格)
         */
        void onSwipeMore();

        /**
         * 当需要给外界反馈信息的时候
         *
         * @param s     信息内容
         * @param color 有必要的话，传字体颜色给外界
         */
        void onToast(String s, int color);
    }

    /**
     * 反馈给外界的回调
     */
    private GestureEventCallback mGestureEventCallback;

    public void setGestureFinishedCallback(GestureEventCallback gestureFinishedCallback) {
        this.mGestureEventCallback = gestureFinishedCallback;
    }

    public static class GestureEventCallbackAdapter implements GestureEventCallback {

        @Override
        public void onSwipeFinish(List<Integer> pwd) {

        }

        @Override
        public void onResetFinish(List<Integer> pwd) {

        }

        @Override
        public void onCheckFinish(boolean succeedOrFailed) {

        }

        @Override
        public void onSwipeMore() {

        }

        @Override
        public void onToast(String s, int color) {

        }
    }

    //*************************下面是辅助方法以及辅助内部类***********************************************

    /**
     * 辅助方法，复制一份密码对象,因为如果直接把当前对象的密码返回出去，则外界使用的全部都是同一个对象，这个对象可能随时变化，外层逻辑无法对比密码值
     */
    private List<Integer> copyPwd(List<Integer> pwd) {
        List<Integer> copyOne = new ArrayList<>();
        for (int i = 0; i < pwd.size(); i++) {
            copyOne.add(pwd.get(i));
        }
        return copyOne;
    }

    /**
     * 对比两个list是否内容完全相同
     */
    private boolean compare(List<Integer> list1, List<Integer> list2) throws RuntimeException {

        if (list1 == null || list2 == null) {
            throw new RuntimeException("存在list为空，不执行对比");
        }

        if (list1.size() != list2.size())//size长度都不同，就不用比了
            return false;

        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i) != list2.get(i)) {
                return false;
            }
        }
        return true;
    }


    public class ColorHolder {
        public static final int COLOR_RED = 0xffFF3232;
        public static final int COLOR_GRAY = 0xff999999;
        public static final int COLOR_YELLOW = 0xffF8A916;
    }

    public class ToastStrHolder {
        public static final String successStr = "绘制成功";
        public static final String tryAgainStr = "请再次绘制手势密码";
        public static final String notSameStr = "与首次绘制不一致，请再次绘制";
        public static final String forYourSafetyStr = "为了您的账户安全，请设置手势密码";
        public static final String swipeTooLittlePointStr = "请最少连接%s个点";
        public static final String wrongPwdInputStr = "输入错误，您还可以输入%s次";
    }
}
