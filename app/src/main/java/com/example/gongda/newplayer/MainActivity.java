package com.example.gongda.newplayer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import org.angmarch.views.NiceSpinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MainActivity extends FragmentActivity {

    private boolean start_state = true;//1为将播放，0为将暂停
    private ImageButton start;
    private ImageButton next;
    private ImageButton last;
    private SeekBar seekBar;
    private TextView songName;
    private TextView songArtist;
    private NiceSpinner spinner;
    private ArrayAdapter<String>spinList;
    private int playorder = 0;//0为循环，//1为随机
    //播放路径列表
    public List<String> myMusicList=new ArrayList<String>();
    //作者-歌曲名列表
    public List<String> playList = new ArrayList<String>();
    //作者列表
    public List<String> artistList = new ArrayList<String>();
    //当前播放歌曲的索引
    public int currentListItem=-1;

    ArrayAdapter<String> musicList;
    ViewPager vp;

    private ProReceive Proreceiver = null;
    private CurReceive Curreceiver = null;
    private UnlockReceive Unlockreceiver = null;
    private int time = 0;//歌曲总时长
    public static int progress = 0;//歌曲进度

    Handler prohandler = new Handler();

    IntentFilter filter,filter1,filter2;

    private  String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME
    };

    Fragment_SongList fragment_songList;
    Fragment_Lyric fragment_lyric;

    PagerSlidingTabStrip tabs;

    Message msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findView();
        musicList();

        seekBar.setEnabled(false);

        List<String> dataset = new LinkedList<>(Arrays.asList("循环", "随机"));
        spinner.attachDataSource(dataset);

        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        spinner.setVisibility(View.VISIBLE);

        playlistener();

        try {
            vp = (ViewPager) findViewById(R.id.viewpager);
            vp.setAdapter(new myPagerAdapter(getSupportFragmentManager()));
            tabs.setViewPager(vp);
        } catch (Throwable t){
            t.printStackTrace(System.out);
        }

        Intent intent = new Intent(MainActivity.this,MusicPlay.class);
        getApplicationContext().startService(intent);

        Proreceiver = new ProReceive();
        filter = new IntentFilter();
        filter.addAction("updatePro");
        MainActivity.this.registerReceiver(Proreceiver, filter);
        prohandler.post(updatePro);

        Curreceiver = new CurReceive();
        filter1 = new IntentFilter();
        filter1.addAction("updateCur");
        MainActivity.this.registerReceiver(Curreceiver, filter1);

        Unlockreceiver = new UnlockReceive();
        filter2 = new IntentFilter();
        filter2.addAction("Unlock");
        MainActivity.this.registerReceiver(Unlockreceiver, filter2);

        show_song();
    }
    //配置fragment
    class myPagerAdapter extends FragmentPagerAdapter {
        String[] title = { "列表", "歌词" };

        public myPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    fragment_songList = new Fragment_SongList();
                    return fragment_songList;
                case 1:
                    fragment_lyric = new Fragment_Lyric();
                    return fragment_lyric;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {

            return title.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return title[position];
        }

    }
    //获取音乐列表
    public void musicList(){
        Cursor cu = MainActivity.this.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,
                null,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if(cu.moveToFirst()){
            do{
                myMusicList.add(cu.getString(2));//路径
                playList.add(cu.getString(7));//作者-歌曲名列表
                artistList.add(cu.getString(4));//作者名
            }while(cu.moveToNext());
            musicList=new ArrayAdapter<String>
                    (MainActivity.this,R.layout.support_simple_spinner_dropdown_item, playList);
        }
        else{
            start.setEnabled(false);
            next.setEnabled(false);
            last.setEnabled(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("提示");
            builder.setMessage("本地无歌曲");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ;
                }
            });
            builder.show();
        }
        cu.close();
    }

    public ArrayAdapter<String> getArrayAdapter(){
        return musicList;
    }

    public void changeCurrentListItem(int pos){
        currentListItem = pos;
    }

    public boolean getStartState(){
        return start_state;
    }

    public void setStartState(boolean i){
        start_state = i;
    }

    public void setSeekBar(){seekBar.setEnabled(true);}

    public ImageButton getStart(){
        return start;
    }

    void findView(){
        start = (ImageButton)findViewById(R.id.start);
        next = (ImageButton)findViewById(R.id.next);
        last = (ImageButton)findViewById(R.id.last);
        seekBar = (SeekBar)findViewById(R.id.seekBar1);
        spinner = (NiceSpinner)findViewById(R.id.Spinner01);
        songName = (TextView)findViewById(R.id.songName);
        songArtist = (TextView)findViewById(R.id.songArtist);
        tabs = (PagerSlidingTabStrip)findViewById(R.id.tabs);
    }

    void intentToservice(int state,int Item,int pro){
        Intent intent = new Intent(MainActivity.this,MusicPlay.class);
        intent.putExtra("state", state);
        intent.putExtra("path", myMusicList.get(Item));
        intent.putExtra("progress",pro);
        getApplicationContext().startService(intent);
    }

    void callLoadLrc(int Item){
        String[] fileName = myMusicList.get(Item).split(".mp3");
        //String[] fileName = playList.get(Item).split(".mp3");
        try{
            fragment_lyric.lrcView.LoadLrc(fileName[0]);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    void playlistener(){
        //开始
        start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(currentListItem == -1)
                    currentListItem = 0;
                if(start_state == true){//未播放
                    start.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                    start_state = false;

                    intentToservice(2,currentListItem,0);

                    callLoadLrc(currentListItem);
                    seekBar.setEnabled(true);
                }
                else {//暂停
                    start.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                    start_state = true;

                    intentToservice(4,currentListItem,0);
                }

            }
        });
        //下一首
        next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(currentListItem == -1)
                    currentListItem = 0;
                else if(playorder == 0) {
                    currentListItem++;
                    if (currentListItem == myMusicList.size()) {
                        currentListItem = 0;
                    }
                }
                else{
                    currentListItem = Math.abs(new Random().nextInt())% myMusicList.size();
                }
                start.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                start_state = false;

                intentToservice(3,currentListItem,0);

                callLoadLrc(currentListItem);
                seekBar.setEnabled(true);

                next.setClickable(false);
                last.setClickable(false);
            }
        });
        //上一首
        last.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(currentListItem == -1)
                    currentListItem = myMusicList.size() - 1;
                else if(playorder == 0) {
                    if(currentListItem == 0){
                        currentListItem = myMusicList.size() - 1;
                    }
                    else{
                        currentListItem --;
                    }
                }
                else{
                    currentListItem = Math.abs(new Random().nextInt()) % myMusicList.size();
                }
                start.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                start_state = false;

                intentToservice(5,currentListItem,0);

                callLoadLrc(currentListItem);
                seekBar.setEnabled(true);

                next.setClickable(false);
                last.setClickable(false);
            }
        });
        //监听进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b == true) {
                    intentToservice(6,currentListItem,i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ;
            }
        });
    }

    //使用数组形式操作，导航栏
    public class SpinnerSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            playorder = arg2;
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    //更新progress，刷新进度条
    public class ProReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context,Intent intent){
            Bundle bundle = intent.getExtras();
            time = bundle.getInt("time");
            progress = bundle.getInt("progress",progress);

            fragment_lyric.setProgress(progress);
        }
    }

    Runnable updatePro = new Runnable() {
        @Override
        public void run() {
            seekBar.setMax(time);
            seekBar.setProgress(progress);
            prohandler.post(updatePro);
        }
    };

    //歌曲放完时自动播放下一首
    public class CurReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context,Intent intent){
            Bundle bundle = intent.getExtras();
            if(bundle.getInt("Next") == 1){//播放下一首
                if(playorder == 0) {
                    currentListItem++;
                    if (currentListItem == myMusicList.size()) {
                        currentListItem = 0;
                    }
                }
                else{
                    currentListItem = Math.abs(new Random().nextInt())% myMusicList.size();
                }
                intentToservice(3,currentListItem,0);

                callLoadLrc(currentListItem);
            }
        }
    }

    public class UnlockReceive extends BroadcastReceiver{
        @Override
        public void onReceive(Context context,Intent intent){
            Bundle bundle = intent.getExtras();
            if(bundle.getInt("flag") == 1){
                next.setClickable(true);
                last.setClickable(true);
            }

        }
    }

    //更新当前歌曲
    Thread showsong;

    public void show_song(){
        showsong = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(currentListItem != -1)
                        sendMsg(currentListItem);
                    try{
                        showsong.sleep(200);
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        showsong.start();
    }

    public void sendMsg(int current){
        msg = songHandler.obtainMessage();
        msg.what = 1;
        String[] Name = playList.get(current).split(".mp3");
        msg.obj = Name[0];
        songHandler.sendMessage(msg);
    }

    Handler songHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String[] song_Info = msg.obj.toString().split("-");
                    songName.setText(song_Info[1].trim());
                    songArtist.setText(song_Info[0].trim());
                    break;
                default:
                    break;
            }
        }
    };
    //关闭Service
    protected void onDestroy(){
        stopService(new Intent(MainActivity.this, MusicPlay.class));
        prohandler.removeCallbacks(updatePro);
        MainActivity.this.unregisterReceiver(Proreceiver);
        MainActivity.this.unregisterReceiver(Curreceiver);
        MainActivity.this.unregisterReceiver(Unlockreceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
