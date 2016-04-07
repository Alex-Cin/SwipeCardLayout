package github.alex.swipecardlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class SwipeCardLayout extends FrameLayout {
    private int cardViewLayoutId;
    private float startRawX;
    private float startRawY;
    private int cardHeight;
    private int cardWidth;
    /**
     * CardView 左右晃动的 半径 radius = cardHeight x 2
     */
    private float radius;
    /**
     * 左右  状态的临界值
     */
    private int swipeDistance;
    private int cardLeftMargin = 0;
    private int cardRightMargin = 0;
    private int cardTopMargin = 0;
    private int screenWidth;
    private ReLayoutHandler reLayoutHandler;
    private ReMoveHandler reMoveHandler;
    private OnSwipeCardListener onSwipeCardListener;
    private static final int whatRemoving = 100;
    private static final int whatRemoved = 101;
    /**
     * 卡片的总数
     */
    private int cardCount;
    /**
     * 卡片的剩余个数
     */
    private int cardBalanceCount;
    /**
     * 堆栈的原始长度
     */
    private int stackLength;
    /**
     * 手指在X轴上滑动的矢量  向量
     */
    private int distanceX;
    /**
     * 正在操作的 卡片
     */
    private View swipingCardView;

    public SwipeCardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 设置子布局的资源 id,必须满足 cardCount > stackLength
     *
     * @param cardCount        卡片的总数
     * @param stackLength      堆栈的长度
     * @param cardViewLayoutId 卡片布局的 id
     * @param cardTopMargin    单位 dp
     */
    public void setCardView(int cardCount, int stackLength, int cardViewLayoutId, int cardTopMargin) {
        this.cardViewLayoutId = cardViewLayoutId;
        this.cardCount = cardCount;
        this.cardBalanceCount = cardCount;
        this.stackLength = stackLength;
        swipeDistance = (int) dp2Px(48);
        reLayoutHandler = new ReLayoutHandler();
        reMoveHandler = new ReMoveHandler();
        screenWidth = getScreenWidth(getContext());
        cardLeftMargin = (int) dp2Px(32);
        cardRightMargin = (int) dp2Px(32);
        this.cardTopMargin = (int) dp2Px(cardTopMargin);
        addCardView(stackLength);
        View lastChildView = getLastView();
        cardHeight = getViewHeight(lastChildView);
        cardWidth = screenWidth - cardLeftMargin - cardRightMargin;
        radius = cardHeight * 2;
    }

    /**
     * 给堆栈布局添加 CardView
     */
    private void addCardView(int stackLength) {
        LayoutParams params = new LayoutParams(screenWidth - cardLeftMargin - cardRightMargin, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = this.cardTopMargin;
        for (int i = 0; (i < stackLength) && (i < cardBalanceCount); i++) {
            View cardView = LayoutInflater.from(getContext()).inflate(cardViewLayoutId, null);
            cardView.setLayoutParams(params);
            addView(cardView, i);
        }
    }

    /**
     * 设置水平滑动的临界值
     *
     * @param swipeDistance 单位 dp
     */
    public void setSwipeDistance(int swipeDistance) {
        this.swipeDistance = (int) dp2Px(swipeDistance);
    }

    /**
     * 得到最后一个 ChildView
     */
    public View getLastView() {
        int childCount = getChildCount();
        if (childCount > 0) {
            return getChildAt(childCount - 1);
        } else {
            return null;
        }
    }

    /**
     * 得到倒数第2个 ChildView
     */
    public View getPreLastView() {
        int childCount = getChildCount();
        if (childCount > 1) {
            return getChildAt(childCount - 2);
        } else {
            return null;
        }
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        View lastChildView = getLastView();
        if (lastChildView != null) {
            lastChildView.setOnTouchListener(new CardOnTouchListener());
        }
        int childCount = getChildCount();
        for (int i = childCount - 1, j = 0; i >= 0; i--, j++) {
            View view = getChildAt(i);
            //view.setScaleX(Math.pow(0.9,j));
            view.layout(cardLeftMargin + j * 20, getPaddingTop() + j * 20 + cardTopMargin, cardWidth + cardRightMargin - j * 20, cardHeight + getPaddingTop() + cardTopMargin + j * 20);
        }
        onSwipeCardListener.onSwiping(0, cardBalanceCount, cardCount, stackLength);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private final class CardOnTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                distanceX = 0;
                startRawX = event.getRawX();
                startRawY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                distanceX = (int) (event.getRawX() - startRawX);
                float angle = swipeDistance2Rotation(distanceX);
                v.setRotation(angle);
                v.layout(cardLeftMargin + distanceX, getPaddingTop() + cardTopMargin, cardWidth + distanceX + cardRightMargin, cardHeight + getPaddingTop() + cardTopMargin);
                int swipeStatus = 0;
                if (Math.abs(distanceX) <= swipeDistance) {
                    swipeStatus = 0;
                } else if (distanceX < 0) {
                    swipeStatus = -1;
                } else if (distanceX > 0) {
                    swipeStatus = 1;
                }
                onSwipeCardListener.onSwiping(swipeStatus, cardBalanceCount, cardCount, stackLength);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (Math.abs(distanceX) < swipeDistance) {
                    ReLayoutThread reLayoutThread = new ReLayoutThread();
                    swipingCardView = v;
                    reLayoutThread.start();
                } else {
                    ReMoveThread reMoveThread = new ReMoveThread();
                    swipingCardView = v;
                    reMoveThread.start();
                }
            }
            return true;
        }
    }

    private final class ReLayoutThread extends Thread {
        @Override
        public void run() {
            for (int i = distanceX; i > 0; ) {
                i = i - 50;
                distanceX = i;
                reLayoutHandler.sendEmptyMessage(100);
                SystemClock.sleep(40);
            }
            for (int i = distanceX; i < 0; ) {
                i = i + 50;
                distanceX = i;
                reLayoutHandler.sendEmptyMessage(100);
                SystemClock.sleep(40);
            }
            distanceX = 0;
            reLayoutHandler.sendEmptyMessage(100);
        }
    }

    private final class ReLayoutHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            float angle = swipeDistance2Rotation(distanceX);
            //KLog.e("ReLayoutHandler = "+cardWidth+" distanceX = "+ distanceX+"  "+(cardWidth + distanceX));
            swipingCardView.layout(cardLeftMargin + distanceX, getPaddingTop() + cardTopMargin, cardWidth + distanceX + cardRightMargin, cardHeight + getPaddingTop() + cardTopMargin);
            swipingCardView.setRotation(angle);
        }
    }

    private final class ReMoveThread extends Thread {
        @Override
        public void run() {
            super.run();
            for (int i = distanceX; (distanceX < 0) && i > (-radius); ) {
                i = i - 100;
                distanceX = i;
                reMoveHandler.sendEmptyMessage(whatRemoving);
                if (i > -800) {
                    SystemClock.sleep(40);
                } else {
                    SystemClock.sleep(5);
                }
                //KLog.e("distanceX = "+distanceX+" radius = "+radius);
            }
            for (int i = distanceX; (distanceX > 0) && (i < radius); ) {
                i = i + 100;
                distanceX = i;
                reMoveHandler.sendEmptyMessage(whatRemoving);
                if (i < 800) {
                    SystemClock.sleep(40);
                } else {
                    SystemClock.sleep(5);
                }
            }
            reMoveHandler.sendEmptyMessage(whatRemoved);
        }
    }

    private final class ReMoveHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == whatRemoving) {
                float angle = swipeDistance2Rotation(distanceX);
                swipingCardView.layout(cardLeftMargin + distanceX, getPaddingTop() + cardTopMargin, cardWidth + distanceX + cardRightMargin, cardHeight + getPaddingTop() + cardTopMargin);
                swipingCardView.setRotation(angle);
            } else if (msg.what == whatRemoved) {
                if (cardBalanceCount > stackLength) {
                    swipingCardView.layout(cardLeftMargin, getPaddingTop() + cardTopMargin, cardWidth + cardRightMargin, cardHeight + getPaddingTop() + cardTopMargin);
                    swipingCardView.setRotation(0);
                } else if (cardBalanceCount > 0) {
                    removeViewAt(getChildCount() - 1);
                    invalidate();
                }
                cardBalanceCount = cardBalanceCount - 1;
                if (onSwipeCardListener != null) {
                    onSwipeCardListener.onSwipeFinish(cardBalanceCount, cardCount, stackLength);
                }
            }
        }
    }

    private float swipeDistance2Rotation(float distance) {
        return (float) (Math.asin(distance / radius) * 180 / Math.PI);
    }
    /**
     * 获得屏幕宽度
     */
    private int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }
    /**
     * 数据转换: dp---->px
     */
    private float dp2Px(float dp) {
        if (getContext() == null) {
            return -1;
        }
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * 获得这个View的高度
     * 测量这个view，最后通过getMeasuredHeight()获取高度.
     *
     * @param view 要测量的view
     * @return 测量过的view的高度
     */
    private int getViewHeight(View view) {
        measureView(view);
        return view.getMeasuredHeight();
    }

    private void measureView(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(childWidthSpec, childHeightSpec);
    }

    public void setOnSwipeCardListener(OnSwipeCardListener onSwipeCardListener) {
        this.onSwipeCardListener = onSwipeCardListener;
    }

    public interface OnSwipeCardListener {
        /**
         * @param cardBalanceCount 卡片的剩余个数
         * @param cardCount        卡片的总个数
         * @param stackLength      堆栈的原始长度
         */
        public void onSwipeFinish(int cardBalanceCount, int cardCount, int stackLength);

        /**
         * @param cardBalanceCount 卡片的剩余个数
         * @param cardCount        卡片的总个数
         * @param stackLength      堆栈的原始长度
         * @param swipeStatus      [-1, 偏左] [0, 居中] [1, 偏右]
         */
        public void onSwiping(int swipeStatus, int cardBalanceCount, int cardCount, int stackLength);
    }
}
