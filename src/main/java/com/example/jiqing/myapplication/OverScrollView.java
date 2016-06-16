package com.example.jiqing.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * 根据一个下拉刷新修改的ScrollView，阻尼效果（带注释）
 * 
 * Created by smt on 2016/6/10.
 */
public class OverScrollView extends ScrollView {
    public static final String TAG = "PullToRefreshLayout";
    //触发事件的高度默认阀值
    private static final int TRIGGER_HEIGHT = 120;
    //滑动的总距离
    private float overScrollDistance;
    //触发事件的高度阀值，最小值为30
    private int mOverScrollTrigger = TRIGGER_HEIGHT;
    private OverScrollTinyListener mOverScrollTinyListener;
    private OverScrollListener mOverScrollListener;
    // 按下Y坐标，上一个事件点Y坐标
    private float downY, lastY;
    // 下拉的距离。注意：pullDownY和pullUpY不可能同时不为0
    public float pullDownY = 0;
    // 上拉的距离
    private float pullUpY = 0;
    private MyTimer timer;
    // 回滚速度
    public float MOVE_SPEED = 8;
    // 第一次执行布局
    private boolean isLayout = false;
    // 手指滑动距离与下拉头的滑动距离比，中间会随正切函数变化
    private float radio = 2;
    // 实现了Pullable接口的View
    private View pullableView;
    // 过滤多点触碰
    private int mEvents;
    // 这两个变量用来控制pull的方向，如果不加控制，当情况满足可上拉又可下拉时没法下拉
    private boolean canPullDown = true;
    private boolean canPullUp = true;
    /**
     * 执行自动回滚的handler
     */
    private  Handler updateHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // 回弹速度随下拉距离moveDeltaY增大而增大
            MOVE_SPEED = (float) (5 + 15 * Math.tan(Math.PI / 2
                    / getMeasuredHeight() * (pullDownY + Math.abs(pullUpY))));
            if (pullDownY > 0)
                pullDownY -= MOVE_SPEED;
            else if (pullUpY < 0)
                pullUpY += MOVE_SPEED;
            if (pullDownY < 0) {
                // 已完成回弹
                pullDownY = 0;
                timer.cancel();
            }
            if (pullUpY > 0) {
                // 已完成回弹
                pullUpY = 0;
                timer.cancel();
            }
            // 刷新布局,会自动调用onLayout
            requestLayout();
        }

    };

    public OverScrollView(Context context) {
        super(context);
        initView(context);
    }

    public OverScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public OverScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        timer = new MyTimer(updateHandler);
    }

    private void hide() {
        timer.schedule(5);
    }

    /**
     * 不限制上拉或下拉
     */
    private void releasePull() {
        canPullDown = true;
        canPullUp = true;
    }

    /*
     * （非 Javadoc）由父控件决定是否分发事件，防止事件冲突
     *
     * @see android.view.ViewGroup#dispatchTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                lastY = downY;
                timer.cancel();
                mEvents = 0;
                releasePull();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // 过滤多点触碰
                mEvents = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = ev.getY() - lastY;
                if (mEvents == 0) {
                    if (canPullDown && isCanPullDown()) {
                        // 可以下拉，正在加载时不能下拉
                        // 对实际滑动距离做缩小，造成用力拉的感觉
                        pullDownY = pullDownY + deltaY / radio;
                        if (ev.getY() - lastY  < 0) {
                            pullDownY = pullDownY + deltaY ;
                        }
                        if (pullDownY < 0) {
                            pullDownY = 0;
                            canPullDown = false;
                            canPullUp = true;
                        }
                        if (pullDownY > getMeasuredHeight())
                            pullDownY = getMeasuredHeight();
                        overScrollDistance = pullDownY;
                    } else if (canPullUp && isCanPullUp()) {
                        // 可以上拉，正在刷新时不能上拉
                        pullUpY = pullUpY + deltaY / radio;
                        if (ev.getY() - lastY > 0) {
                            pullUpY = pullUpY + deltaY ;
                        }
                        if (pullUpY > 0) {
                            pullUpY = 0;
                            canPullDown = true;
                            canPullUp = false;
                        }
                        if (pullUpY < -getMeasuredHeight())
                            pullUpY = -getMeasuredHeight();
                        overScrollDistance = pullUpY;
                    } else
                        releasePull();
                } else
                    mEvents = 0;
                lastY = ev.getY();
                // 根据下拉距离改变比例
                radio = (float) (2 + 3 * Math.tan(Math.PI / 2 / getMeasuredHeight()
                        * (pullDownY + Math.abs(pullUpY))));
                requestLayout();
                // 因为刷新和加载操作不能同时进行，所以pullDownY和pullUpY不会同时不为0，因此这里用(pullDownY +
                // Math.abs(pullUpY))就可以不对当前状态作区分了
                if ((pullDownY + Math.abs(pullUpY)) > 8) {
                    // 防止下拉过程中误触发长按事件和点击事件
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                }
                if(mOverScrollTinyListener != null){
                    mOverScrollTinyListener.scrollDistance((int)deltaY, (int)overScrollDistance);
                }
                break;
            case MotionEvent.ACTION_UP:
                hide();
                overScrollTrigger();
                if(mOverScrollTinyListener != null && (isCanPullDown() || isCanPullUp())){
                    mOverScrollTinyListener.scrollLoosen();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if(mOverScrollTinyListener != null && (isCanPullDown() || isCanPullUp())){
                    mOverScrollTinyListener.scrollLoosen();
                }
                break;
            default:
                break;
        }
        // 事件分发交给父类
        try {
            super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return true;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!isLayout) {
            // 这里是第一次进来的时候做一些初始化
            pullableView = getChildAt(0);
            isLayout = true;
        }

        pullableView.layout(0, (int) (pullDownY + pullUpY),
                pullableView.getMeasuredWidth(), (int) (pullDownY + pullUpY)
                        + pullableView.getMeasuredHeight());
    }

    class MyTimer {
        private Handler handler;
        private Timer timer;
        private MyTask mTask;

        public MyTimer(Handler handler) {
            this.handler = handler;
            timer = new Timer();
        }

        public void schedule(long period) {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
            mTask = new MyTask(handler);
            timer.schedule(mTask, 0, period);
        }

        public void cancel() {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
        }

        class MyTask extends TimerTask {
            private Handler handler;

            public MyTask(Handler handler) {
                this.handler = handler;
            }

            @Override
            public void run() {
                handler.obtainMessage().sendToTarget();
            }

        }
    }

    /**
     * 判断是否滚动到顶部
     */
    private boolean isCanPullDown() {
        return getScrollY() == 0 ||
                pullableView.getHeight() < getHeight() + getScrollY();
    }

    /**
     * 判断是否滚动到底部
     */
    private boolean isCanPullUp() {
        return  pullableView.getHeight() <= getHeight() + getScrollY();
    }

    private boolean isOnTop(){
        return getScrollY() == 0;
    }

    private boolean isOnBottom(){
        return getScrollY() + getHeight() == pullableView.getHeight();
    }

    /**
     * 当OverScroll超出一定值时，调用此监听
     *
     * @author smt
     * @since 2016-6-10 下午4:36:29
     */
    public interface OverScrollListener {

        /**
         * 顶部
         */
        void headerScroll();

        /**
         * 底部
         */
        void footerScroll();

    }

    /**
     * 每当OverScroll时，都能触发的监听
     * @author King
     * @since 2016-6-10 下午4:39:06
     */
    public interface OverScrollTinyListener{

        /**
         * 滚动距离
         * @param tinyDistance 当前滚动的细小距离
         * @param totalDistance 滚动的总距离
         */
        void scrollDistance(int tinyDistance, int totalDistance);

        /**
         * 滚动松开
         */
        void scrollLoosen();
    }

    /**
     * 设置OverScrollListener出发阀值
     * @param height
     */
    public void setOverScrollTrigger(int height){
        if(height >= 30){
            mOverScrollTrigger = height;
        }
    }

    private void overScrollTrigger(){
        if(mOverScrollListener == null){
            return;
        }

        if(overScrollDistance > mOverScrollTrigger && overScrollDistance >= 0){
            mOverScrollListener.headerScroll();
        }

        if(overScrollDistance < -mOverScrollTrigger && overScrollDistance < 0){
            mOverScrollListener.footerScroll();
        }
    }

    public OverScrollTinyListener getOverScrollTinyListener() {
        return mOverScrollTinyListener;
    }

    public void setOverScrollTinyListener(OverScrollTinyListener OverScrollTinyListener) {
        this.mOverScrollTinyListener = OverScrollTinyListener;
    }

    public OverScrollListener getOverScrollListener() {
        return mOverScrollListener;
    }

    public void setOverScrollListener(OverScrollListener OverScrollListener) {
        this.mOverScrollListener = OverScrollListener;
    }
}
