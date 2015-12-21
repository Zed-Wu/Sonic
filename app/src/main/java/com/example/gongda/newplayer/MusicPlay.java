package com.example.gongda.newplayer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.security.Provider;

/**
 * Created by GongDa on 2015/11/25.
 */
public class MusicPlay extends Service implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mp;
    private int temp = 0;
    private Thread thread = null;
    private int progress = 0;
    private int time = 0;
    private Status status;

    private enum Status{
        IDEL,
        PLAYING,
        SWITCHING
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);


    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mp.release();
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        Log.d("wukun", "onCommand");
        if(intent != null) {
            int state = intent.getIntExtra("state", 0);
            int progress = intent.getIntExtra("progress", 0);
            String path = intent.getStringExtra("path");
            switch (state) {
                case 1://停止
                    if (mp.isPlaying())
                        mp.stop();
                    break;
                case 2://播放
                    play(path, state);
                    break;
                case 3://下一首
                    play(path, state);
                    break;
                case 4://暂停
                    pause();
                    break;
                case 5://上一首
                    play(path, state);
                    break;
                case 6:
                    mp.seekTo(progress);
                    break;
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }

    public void play(String path,int state){
        while(status == Status.SWITCHING);
        status = Status.SWITCHING;
        try{
            if(temp == 0 || state == 3 || state == 5) {
                mp.stop();
                mp.reset();
                mp.setDataSource(path);
                mp.prepare();
            }
            mp.start();
            Intent retIntent = new Intent();
            retIntent.putExtra("flag", 1);
            retIntent.setAction("Unlock");
            sendBroadcast(retIntent);
            if (thread == null) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!Thread.interrupted()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Intent intent1 = new Intent();
                            try {
                                progress = mp.getCurrentPosition();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            intent1.putExtra("progress", progress);
                            try {
                                time = mp.getDuration();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            intent1.putExtra("time", time);
                            intent1.setAction("updatePro");
                            sendBroadcast(intent1);
                        }
                    }
                });
                thread.start();
            }
            temp = 0;
        }catch (IOException e){
            e.printStackTrace();
        }
        status = Status.PLAYING;
    }

    public void pause(){
        if(mp.isPlaying()) {
            mp.pause();
        }
        temp = 1;
    }

    //自动播放下一首
    @Override
    public void onCompletion(MediaPlayer mp){
        Intent intent = new Intent();
        intent.putExtra("Next",1);
        intent.setAction("updateCur");
        sendBroadcast(intent);
        Log.d("wukun","das1");
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
}
