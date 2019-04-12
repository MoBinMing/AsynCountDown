package com.example.asyncountdown;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public abstract class AbstractStaticHandler<T> extends Handler {
    private final WeakReference<T> context;
    AbstractStaticHandler(T context){
        this.context = new WeakReference<>(context);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        T t = context.get();
        handleMessage(msg,t);
    }

    /**
     * 处理消息的业务逻辑
     * @param msg Message对象
     * @param t context
     */
    public abstract void handleMessage(Message msg,T t);
}

