package com.example.gongda.newplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by GongDa on 2015/12/19.
 */
public class LrcView extends ScrollView {

    private final static String TAG = "LrcView";
    private long mNextTime = 0;//下一句开始时间
    LinearLayout rootView;
    LinearLayout lyricList;
    ArrayList<TextView> lyricItems = new ArrayList<>();

    ArrayList<String> lyricTextList = new ArrayList<>();
    ArrayList<Long> lyricTimeList = new ArrayList<>();

    ArrayList<Integer> lyricItemHeights;

    int height;
    int width;

    int prevSelected = 0;

    OnLyricScrollChangeListener listener;

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);
        final ViewTreeObserver vto = rootView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                height = LrcView.this.getHeight();
                width = LrcView.this.getWidth();
                refreshRootView();
            }
        });
        addView(rootView);
    }

    public void LoadLrc(String fileName) throws Exception{
        lyricTextList.clear();
        lyricTimeList.clear();
        if (fileName == null)
            Log.e(TAG, "file name is null");

        //fileName = fileName.replace(" ","");
        //String path = "/storage/sdcard1/Musiclrc/" + fileName + ".lrc";
        String path = fileName + ".lrc";
        File file = new File(path);
        Log.d(TAG, "load lrc");
        if (!file.exists()) {
            System.out.println("file not exist");
            lyricTextList.add("暂无本地歌词文件");
            lyricTimeList.add(1L);
            refreshLyricList();
            return ;
            //throw newluaunch Exception("lrc not found...");
        }

        Log.d(TAG, "file exists");
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));

        String line = "";
        String[] arr;
        while (null != (line = reader.readLine())) {
            arr = parseLine(line);
            if (null == arr) {
                continue;
            }

            // 如果解析出来只有一个
            if (1 == arr.length) {
                String last = lyricTextList.remove(lyricTextList.size() - 1);
                lyricTextList.add(last + arr[0]);
                continue;
            }
            lyricTimeList.add(Long.parseLong(arr[0]));
            lyricTextList.add(arr[1]);
        }

        reader.close();
        refreshLyricList();
    }

    // 解析时间
    private Long parseTime(String time) {
        // 03:02.12
        String[] min = time.split(":");
        String[] sec = min[1].split("\\.");

        long minInt = Long.parseLong(min[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long secInt = Long.parseLong(sec[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long milInt = Long.parseLong(sec[1].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());

        return minInt * 60 * 1000 + secInt * 1000 + milInt * 10;
    }

    // 解析每行
    private String[] parseLine(String line) {
        Matcher matcher = Pattern.compile("\\[.+\\].+").matcher(line);
        // 如果形如：[xxx]后面啥也没有的，则return空
        if (!matcher.matches()) {
            System.out.println("throws " + line);
            return null;
        }

        line = line.replaceAll("\\[", "");
        String[] result = line.split("\\]");
        result[0] = String.valueOf(parseTime(result[0]));

        return result;
    }

    void refreshRootView() {
        rootView.removeAllViews();
        LinearLayout blank1 = new LinearLayout(getContext()),
                blank2 = new LinearLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height / 2);
        rootView.addView(blank1, params);
        if(lyricList != null)
            rootView.addView(lyricList);
        rootView.addView(blank2, params);
        mNextTime = 0;
    }

    void refreshLyricList() {
        if(lyricList == null)
            lyricList = new LinearLayout(getContext());
        lyricList.setOrientation(LinearLayout.VERTICAL);
        lyricList.removeAllViews();
        lyricItems.clear();
        lyricItemHeights = new ArrayList<>();
        prevSelected = 0;

        for(int i = 0; i < lyricTextList.size(); i++) {
            final TextView textView = new TextView(getContext());
            textView.setText(lyricTextList.get(i));
            textView.setTextSize(15);
            textView.setGravity(Gravity.CENTER);
            final ViewTreeObserver vto = textView.getViewTreeObserver();
            final int index = i;
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    lyricItemHeights.add(index, textView.getHeight());
                }
            });
            lyricList.addView(textView);
            lyricItems.add(index, textView);
        }

        refreshRootView();
    }

    void scrollToIndex(int index) {
        if(index < 0) {
            scrollTo(0, 0);
        }
        if (index < lyricTextList.size()) {
            int sum = 0;
            for (int i = 0; i <= index - 1; i++) {
                sum += lyricItemHeights.get(i);
            }
            sum += lyricItemHeights.get(index) / 2;
            scrollTo(0, sum);
        }
    }

    int getIndex(int length) {
        int index = 0;
        int sum = 0;
        while (sum <= length) {
            if(index >= lyricItemHeights.size()){
                return 0;
            }
            sum += lyricItemHeights.get(index);
            index++;
        }
        return index - 1;
    }

    void setSelected(int index) {
        if(index == prevSelected)
            return;
        for(int i = 0; i < lyricItems.size(); i++) {
            if(i == index)
                lyricItems.get(i).setTextColor(Color.BLUE);
            else
                lyricItems.get(i).setTextColor(Color.BLACK);
        }
        prevSelected = index;
    }

    public void setLyricText(ArrayList<String> textList, ArrayList<Long> timeList) {
        if(textList.size() != timeList.size()) {
            throw new IllegalArgumentException();
        }
        this.lyricTextList = textList;
        this.lyricTimeList = timeList;
        refreshLyricList();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        setSelected(getIndex(t));
        if (listener != null) {
            listener.onLyricScrollChange(getIndex(t), getIndex(oldt));
        }
    }

    public void setOnLyricScrollChangeListener(OnLyricScrollChangeListener i) {
        listener = i;
    }

    public interface OnLyricScrollChangeListener {
        void onLyricScrollChange(int index, int oldindex);
    }

    public synchronized void changeCurrent(long time) {
        // 如果当前时间小于下一句开始的时间
        // 直接return
        if (mNextTime > time) {
            return;
        }
        // 每次进来都遍历存放的时间
        for (int i = 0; i < lyricTimeList.size(); i++) {
            // 发现这个时间大于传进来的时间
            // 那么现在就应该显示这个时间前面的对应的那一行
            // 每次都重新显示，是不是要判断：现在正在显示就不刷新了
            int index = 0;
            if (lyricTimeList.get(i) > time && i >= 1) {
                System.out.println("换");
                mNextTime = lyricTimeList.get(i);
                index = i <= 1 ? 0 : i - 1;
                scrollToIndex(index);
                break;
            }
        }
    }
}
