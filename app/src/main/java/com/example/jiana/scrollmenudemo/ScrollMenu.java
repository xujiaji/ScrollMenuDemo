package com.example.jiana.scrollmenudemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;


public class ScrollMenu extends RelativeLayout {
    private boolean isOpenLog = true;//是否打开log
    /**
     * 设置tag为"no_horizontal"的子布局触摸无法水平滑动
     */
    private static final String VIEW_TAG_NO_VERTICAL = "no_vertical";
    /**
     * 设置tag为"no_vertical"的子布局触摸无法垂直滑动
     */
    private static final String VIEW_TAG_NO_HORIZONTAL = "no_horizontal";

    /**
     * 正常状态
     */
    public static final int NORMAL = 0;
    /**
     * 侧滑到顶部
     */
    public static final int TOP = 2;
    /**
     * 滑到右侧
     */
    public static final int RIGHT = 3;
    /**
     * 侧滑到底部
     */
    public static final int BOTTOM = 4;
    private static final String TAG = "ScrollMenu";
    //滑动组件
    private Scroller mScroller;
    //数度跟踪者
    private VelocityTracker mVelocityTracker;

    //最后一个动作的位置
    private float mLastTouchX, mLastTouchY;
    //能被拖动的临界值
    private int mTouchSlop;
    //滑动的最大速度
    private int mMaximumVelocity;
    private float angleLastX, angleLastY;
    //拖动锁
    private boolean mDragging = false;
    private boolean
            canVerticalSlide, //能否垂直方向滑动
            canHorizontalSlide,//能否水平方向滑动
            openVerticalSlide = true,//打开垂直方向滑动
            openHorizontalSlide = true;//打开水平方向的滑动

    /**
     * 当前状态
     */
    private int status = NORMAL;

    private ScrollHandler mScrollHandler;

    public ScrollMenu(Context context) {
        super(context);
        init(context);
    }

    public ScrollMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScrollMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mScrollHandler = new ScrollHandler(this);
        mScroller = new Scroller(context);
        mVelocityTracker = VelocityTracker.obtain();
        //获取系统触摸的临界常量值
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            $e(String.format("computeScroll mScroller --- currX:%d --- currY:%d", mScroller.getCurrX(), mScroller.getCurrY()));
            if (-getHeight() == mScroller.getCurrY()) {
                mScrollHandler.sendEmptyMessage(ScrollHandler.FAST_BOTTOM_TO_NORMAL);
            }

            if (getHeight() == mScroller.getCurrY()) {
                mScrollHandler.sendEmptyMessage(ScrollHandler.FAST_TOP_TO_NORMAL);
            }
            invalidate();
        }
    }


    /**
     * 初始化滚动和开始绘制
     */
    public void toRight() {
        status = RIGHT;
        $e("toRight getScrollX = " + getScrollX());
        mScroller.startScroll(getScrollX(), 0, -(getWidth() + getScrollX()), 0, 1000);
        invalidate();
    }

    public void toTop() {
        status = TOP;
        mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + getHeight(), 1000);
        invalidate();
    }

    public void toBottom() {
        status = BOTTOM;
        mScroller.startScroll(0, getScrollY(), 0, -(getHeight() + getScrollY()), 1000);
        invalidate();
    }

    public void toNormal() {
        if (status == TOP || status == BOTTOM) {
            mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), 1000);
        } else {
            $e("toLeft getScrollX = " + getScrollX());
            mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 1000);
        }
        invalidate();
        status = NORMAL;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            canHorizontalSlide = openHorizontalSlide;
            canVerticalSlide = openVerticalSlide;
            View view = getTargetView(this, (int) ev.getRawX(), (int) ev.getRawY());
            $e("dispatchTouchEvent view = " + view);
            if (view != null) {
                if (view instanceof RecyclerView) {
                    RecyclerView rv = (RecyclerView) view;
                    canHorizontalSlide = openHorizontalSlide && !rv.getLayoutManager().canScrollHorizontally();
                    canVerticalSlide = openVerticalSlide && !canHorizontalSlide;
                } else if (VIEW_TAG_NO_VERTICAL.equals(view.getTag())) {
                    canHorizontalSlide = openHorizontalSlide;
                    canVerticalSlide = false;
                } else if (VIEW_TAG_NO_HORIZONTAL.equals(view.getTag())) {
                    canHorizontalSlide = false;
                    canVerticalSlide = openVerticalSlide;
                }

                $e("dispatchTouchEvent canHorizontalSlide = " + canHorizontalSlide);
                $e("dispatchTouchEvent " +
                        "canVerticalSlide = " + canVerticalSlide);
            }

            if (onTouchDownListener != null) {
                onTouchDownListener.touch(ev);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 监听向子布局传递的触摸事件和拦截事件
     * 如果子布局是交互式的（如button），将仍然能接收到触摸事件
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        $e(String.format("onInterceptTouchEvent action = %d, x = %f, y = %f", ev.getAction(), ev.getX(), ev.getY()));
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //判断是否已经完成滚动，如果滚动则停止
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                //重置速度跟踪器
                mVelocityTracker.clear();
                mVelocityTracker.addMovement(ev);

                //保存初始化触摸位置
                mLastTouchX = ev.getX();
                mLastTouchY = ev.getY();
                angleLastX = ev.getX();
                angleLastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = ev.getX();
                final float y = ev.getY();
                final int xDiff = (int) Math.abs(x - mLastTouchX);
                final int yDiff = (int) Math.abs(y - mLastTouchY);
                $e("onInterceptTouchEvent xDiff = " + xDiff);
                $e("onInterceptTouchEvent yDiff = " + yDiff);
                //计算角度
                double angle = Math.atan2(Math.abs(ev.getY() - angleLastY), Math.abs(ev.getX() - angleLastX)) * 180 / Math.PI;
                //验证移动距离是否足够成为触发拖动事件
                if (xDiff > mTouchSlop || yDiff > mTouchSlop) {
                    canHorizontalSlide = canHorizontalSlide && angle < 30;
                    canVerticalSlide = canVerticalSlide && angle > 30;

                    if (!canVerticalSlide && !canHorizontalSlide) {
                        return super.onInterceptTouchEvent(ev);
                    }

                    mDragging = true;
                    mVelocityTracker.addMovement(ev);
                    $e("onInterceptTouchEvent 获取这个动作事件");
                    //获取这个事件
                    return true;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                mVelocityTracker.clear();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 处理接收的事件（事件由onInterceptTouchEvent获取）
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        $e(String.format("onTouchEvent action = %d, x = %f, y = %f", event.getAction(), event.getX(), event.getY()));
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //获取后续事件
                return true;
            case MotionEvent.ACTION_MOVE:
                move(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                //计算当前的速度，如果速度大于最小数度临界值则开启一个滑动
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) mVelocityTracker.getXVelocity();
                int velocityY = (int) mVelocityTracker.getYVelocity();
                $e("onTouchEvent MotionEvent.ACTION_UP velocityX = " + velocityX);
                $e("onTouchEvent MotionEvent.ACTION_UP velocityY = " + velocityY);
                $e("onTouchEvent getScrollX() = " + getScrollX());
                $e("onTouchEvent getScrollY() = " + getScrollY());
                if (canHorizontalSlide) {
                    if (velocityX >= 5000 || (velocityX >= 0 && getScrollX() <= -getWidth() / 3) || (velocityX < 0 && velocityX > -5000 && getScrollX() < -getWidth() * 2 / 3)) {
                        toRight();
                    } else {
                        toNormal();
                    }
                } else if (canVerticalSlide) {
                    if (velocityY >= 5000 || (velocityY >= 0 && getScrollY() <= -getHeight() / 4)) {
                        toBottom();
                        break;
                    }

                    if ((velocityY < -5000 && status == NORMAL) || (velocityY < 0 && getScrollY() >= getHeight() / 4)) {
                        toTop();
                        break;
                    }

                    toNormal();
                }

                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 处理移动事件
     */
    private void move(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        //水平滚动距离
        float diffX = mLastTouchX - x;
        //垂直方向滑动的距离
        float diffY = mLastTouchY - y;

        //如果可以拖动是否被锁，x与y移动的距离大于可移动的距离
        $e("onTouchEvent mDragging = " + mDragging);
        if (!mDragging && (Math.abs(diffX) > mTouchSlop || Math.abs(diffY) > mTouchSlop)) {
            mDragging = true;
        }

        //计算角度
        double angle = Math.toDegrees(Math.atan2(Math.abs(y - angleLastY), Math.abs(x - angleLastX)));
        $e("onTouchEvent angle = " + angle);

        if (mDragging) {
            //滑动这个view
            if (canHorizontalSlide && angle < 30) {
                scrollBy((int) diffX, 0);
                mLastTouchX = x;
                canVerticalSlide = false;
            } else if (canVerticalSlide && angle > 30) {
                scrollBy(0, (int) diffY);
                mLastTouchY = y;
                canHorizontalSlide = false;
            }
        }
    }

    /**
     * 根据触摸到文字获得具体的子view
     */
    public View getTargetView(View view, int x, int y) {
        View target = null;
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0, len = viewGroup.getChildCount(); i < len; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof RecyclerView) {
                target = isTouchPointInView(child, x, y) ? child : null;
                if (target != null) {
                    break;
                }
            } else if (child instanceof ViewGroup) {
                View v = getTargetView(child, x, y);
                if (v != null) {
                    return v;
                }
            }

            target = (isTouchPointInView(child, x, y) && (VIEW_TAG_NO_VERTICAL.equals(child.getTag()) || VIEW_TAG_NO_HORIZONTAL.equals(child.getTag()))) ? child : null;
            if (target != null) {
                break;
            }
        }
        return target;
    }


    /**
     * 计算(x, y)坐标是否在child view的范围内
     *
     * @param child 子布局
     * @param x     x坐标
     * @param y     y坐标
     * @return 子布局是否在点击范围内
     */
    public boolean isTouchPointInView(View child, int x, int y) {
        int[] location = new int[2];
        child.getLocationOnScreen(location);
        int top = location[1];
        int left = location[0];
        int right = left + child.getMeasuredWidth();
        int bottom = top + child.getMeasuredHeight();
        return y >= top && y <= bottom && x >= left && x <= right;
    }

    public int getStatus() {
        return status;
    }


    private OnScrollCompleteListener onScrollCompleteListener;
    private OnTouchDownListener onTouchDownListener;

    public void setOnTouchDownListener(OnTouchDownListener l) {
        this.onTouchDownListener = l;
    }

    public void setOnScrollCompleteListener(OnScrollCompleteListener l) {
        this.onScrollCompleteListener = l;
    }

    public interface OnScrollCompleteListener {
        void completeTop();

        void completeBottom();
    }

    public interface OnTouchDownListener {
        void touch(MotionEvent ev);
    }

    public void setOpenVerticalSlide(boolean openVerticalSlide) {
        this.openVerticalSlide = openVerticalSlide;
    }

    public void setOpenHorizontalSlide(boolean openHorizontalSlide) {
        this.openHorizontalSlide = openHorizontalSlide;
    }

    private static class ScrollHandler extends Handler {
        /**
         * 快速恢复正常模式
         */
        public static final int FAST_TOP_TO_NORMAL = 0X12345;
        public static final int FAST_BOTTOM_TO_NORMAL = 0X12346;

        private WeakReference<ScrollMenu> wr;
        private boolean isRun;

        public ScrollHandler(ScrollMenu scrollMenu) {
            wr = new WeakReference<>(scrollMenu);
        }

        @Override
        public void handleMessage(Message msg) {
            ScrollMenu mScrollMenu = wr.get();
            if (mScrollMenu == null) {
                return;
            }

            switch (msg.what) {
                case FAST_BOTTOM_TO_NORMAL:
                    mScrollMenu.scrollTo(0, -mScrollMenu.getHeight());
                    mScrollMenu.invalidate();
                    mScrollMenu.scrollTo(0, 0);
                    if (mScrollMenu.onScrollCompleteListener != null && agreeOperated()) {
                        mScrollMenu.onScrollCompleteListener.completeBottom();
                    }
                    break;
                case FAST_TOP_TO_NORMAL:
                    mScrollMenu.scrollTo(0, mScrollMenu.getHeight());
                    mScrollMenu.invalidate();
                    mScrollMenu.scrollTo(0, 0);
                    if (mScrollMenu.onScrollCompleteListener != null && agreeOperated()) {
                        agreeOperated();
                        mScrollMenu.onScrollCompleteListener.completeTop();
                    }
                    break;
            }
        }

        /**
         * 是否同意操作
         */
        private boolean agreeOperated() {
            if (isRun) {
                return false;
            }
            isRun = true;
            Timer tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isRun = false;
                }
            }, 1000);
            return true;
        }
    }

    /**
     * 打印log
     *
     * @param s 打印的log数据
     */
    private void $e(String s) {
        if (isOpenLog) {
            Log.e(TAG, s);
        }
    }
}
