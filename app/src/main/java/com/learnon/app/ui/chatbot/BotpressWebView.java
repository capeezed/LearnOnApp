package com.learnon.app.ui.chatbot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class BotpressWebView extends WebView {

    private boolean chatActive = false;

    public BotpressWebView(Context context) {
        super(context);
    }

    public BotpressWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (chatActive || isInsideBotpressBubble(event)) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && isInsideBotpressBubble(event)) {
                chatActive = true;
            }
            return super.onTouchEvent(event);
        }

        return false;
    }

    public void setChatActive(boolean chatActive) {
        this.chatActive = chatActive;
    }

    public boolean isChatActive() {
        return chatActive;
    }

    private boolean isInsideBotpressBubble(MotionEvent event) {
        int touchArea = dp(112);
        return event.getX() >= getWidth() - touchArea && event.getY() >= getHeight() - touchArea;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
