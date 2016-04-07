package com.alex.swipecardlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.socks.library.KLog;

import github.alex.swipecardlayout.SwipeCardLayout;

public class MainActivity extends AppCompatActivity {

    private SwipeCardLayout swipeCardLayout;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        initView();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initView() {
        findViewById(R.id.bt_reload).setOnClickListener(new MyOnClickListener());
        swipeCardLayout = (SwipeCardLayout) findViewById(R.id.scl);
        swipeCardLayout.setCardView(16, 5, R.layout.card_user_info, 32);
        swipeCardLayout.setSwipeDistance(96);
        swipeCardLayout.setOnSwipeCardListener(new MyOnSwipeCardListener());
        View lastView = swipeCardLayout.getLastView();
        TextView textView = (TextView) lastView.findViewById(R.id.tv_info);
        textView.setText("剩余卡片数量 = " + 16 + " 卡片总数 = " + 16 + " 堆栈的长度 = " + 5);

    }

    private final class MyOnSwipeCardListener implements SwipeCardLayout.OnSwipeCardListener {
        @Override
        public void onSwipeFinish(int cardBalanceCount, int cardCount, int stackLength) {
            // KLog.e("currIndex = "+currIndex);
            View lastView = swipeCardLayout.getLastView();
            if (lastView != null) {
                TextView textView = (TextView) lastView.findViewById(R.id.tv_info);
                textView.setText("剩余卡片数量 = " + cardBalanceCount + " 卡片总数 = " + cardCount + " 堆栈的长度 = " + stackLength);
            }
            if (cardBalanceCount > 0) {

            } else if (cardBalanceCount == 0) {
                findViewById(R.id.bt_reload).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.iv_foot_left).setSelected(false);
            findViewById(R.id.iv_foot_right).setSelected(false);
        }

        @Override
        public void onSwiping(int swipeStatus, int cardBalanceCount, int cardCount, int stackLength) {
            View preLastView = swipeCardLayout.getPreLastView();
            if (preLastView != null) {
                TextView textView = (TextView) preLastView.findViewById(R.id.tv_info);
                textView.setText("剩余卡片数量 = " + (cardBalanceCount-1) + " 卡片总数 = " + cardCount + " 堆栈的长度 = " + stackLength);
            }
            if (swipeStatus == -1) {
                findViewById(R.id.iv_foot_left).setSelected(true);
                findViewById(R.id.iv_foot_right).setSelected(false);
            } else if (swipeStatus == 0) {
                findViewById(R.id.iv_foot_left).setSelected(false);
                findViewById(R.id.iv_foot_right).setSelected(false);
            } else if (swipeStatus == 1) {
                findViewById(R.id.iv_foot_left).setSelected(false);
                findViewById(R.id.iv_foot_right).setSelected(true);
            }
        }
    }

    private final class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (R.id.bt_reload == v.getId()) {
                swipeCardLayout.setCardView(14, 5, R.layout.card_user_info, 32);
                View lastView = swipeCardLayout.getLastView();
                TextView textView = (TextView) lastView.findViewById(R.id.tv_info);
                textView.setText("剩余卡片数量 = " + 14 + " 卡片总数 = " + 14 + " 当前卡片在堆栈中的编号 = " + 5 + " 堆栈的长度 = " + 5);
                findViewById(R.id.bt_reload).setVisibility(View.GONE);
            }
        }
    }
}
