package com.example.asyncountdown;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int WHAT_COUNTING = 0;
    public static final int WHAT_ERROR = 1;
    public static final int WHAT_ACHIEVE = 2;
    public static final int PROGRESS = 20;
    private TextView textView;
    private int seconds=PROGRESS;
    private ProgressBar progressBar;
    private boolean isCounting=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.tv);
        findViewById(R.id.btn_single).setOnClickListener(this);
        findViewById(R.id.btn_async).setOnClickListener(this);
        progressBar = findViewById(R.id.pb_progressbar);
        progressBar.setMax(PROGRESS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId()==R.id.btn_single){
            if (isCounting){
                Toast.makeText(MainActivity.this,"计时钟..",Toast.LENGTH_LONG).show();
                return;
            }
            countDown();
        }
        if (v.getId()==R.id.btn_async){
            if (isCounting){
                Toast.makeText(MainActivity.this,"计时钟..",Toast.LENGTH_LONG).show();
                return;
            }
            asyncCountDown();
        }
    }

    //cpu数量
    private static final int CPU_COUNT=Runtime.getRuntime().availableProcessors();
    //调用cpu数量的最小值、最大值
    private static final int CORE_POOL_SIZE=Math.max(2,Math.min(CPU_COUNT-1,4));
    //最大线程
    private static final int MAX_POOL_SIZE=CPU_COUNT*2+1;
    //保持运行的时间
    private static final int KEEP_ALIVE_SECONDS=30;
    //自定义线程创建
    private static final ThreadFactory THREAD_FACTORY=new ThreadFactory() {
        private final AtomicInteger count=new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"thread #"+count.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> POOL_QUEUE=new LinkedBlockingQueue<>(128);

    public static ThreadPoolExecutor getExecutor(){
        ThreadPoolExecutor executor=new ThreadPoolExecutor(CORE_POOL_SIZE,MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,POOL_QUEUE,THREAD_FACTORY);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private void countDown() {
        isCounting=true;
        while (seconds>=0){
            String text="剩余" + seconds + "秒";
            textView.setText(text);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        seconds=20;
        textView.setText("计时完成");

    }
    private void asyncCountDown() {
        //线程池启动线程
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                isCounting=true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (seconds>=0){
                            try {
                                Thread.sleep(1000);
                                seconds--;
                                Message msg=handler.obtainMessage();
                                msg.what=0;
                                msg.arg1=seconds;
                                handler.sendMessage(msg);
                            } catch (InterruptedException e) {
                                handler.sendMessage(handler.obtainMessage(1,e.getMessage()));
                            }
                        }
                        handler.sendEmptyMessage(WHAT_ACHIEVE);
                    }
                }).start();
            }
        });
        //显式线程
        /*isCounting=true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (seconds>=0){
                    try {
                        Thread.sleep(1000);
                        seconds--;
                        Message msg=handler.obtainMessage();
                        msg.what=0;
                        msg.arg1=seconds;
                        handler.sendMessage(msg);
                    } catch (InterruptedException e) {
                        handler.sendMessage(handler.obtainMessage(1,e.getMessage()));
                    }
                }
                handler.sendEmptyMessage(WHAT_ACHIEVE);
            }
        }).start();*/
    }

    private  CountHandler handler=new CountHandler(this);
    private static class CountHandler extends AbstractStaticHandler<MainActivity>{


        CountHandler(MainActivity context) {
            super(context);
        }

        @Override
        public void handleMessage(Message msg, MainActivity mainActivity) {
            switch (msg.what){
                case WHAT_COUNTING:
                    mainActivity.textView.setText("线程剩余" + msg.arg1 + "秒");
                    mainActivity.progressBar.setProgress(msg.arg1);
                    break;
                case WHAT_ERROR:
                    Toast.makeText(mainActivity,msg.obj.toString(),Toast.LENGTH_LONG).show();
                    mainActivity.seconds=PROGRESS;
                    mainActivity.isCounting=false;
                    break;
                case WHAT_ACHIEVE:
                    mainActivity.textView.setText("计时完成");
                    mainActivity.progressBar.setProgress(PROGRESS);
                    mainActivity.seconds=PROGRESS;
                    mainActivity.isCounting=false;
                    break;
                default:
                    break;
            }
        }
    }

}
