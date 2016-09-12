package org.rc.scrapeviewdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.rc.scrapelayout.R;
import org.rc.scrapeview.ScrapeView;

public class MainActivity extends AppCompatActivity {

    private ScrapeView sv;
    private SeekBar sb;
    private SeekBar sbMaxPercent;
    private TextView tvSize;
    private TextView tvPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sv = (ScrapeView) findViewById(R.id.sv);
        sb = (SeekBar) findViewById(R.id.sb);
        tvSize = (TextView) findViewById(R.id.tvEraseSize);
        tvPercent = (TextView) findViewById(R.id.tvPercentSize);
        sbMaxPercent = (SeekBar) findViewById(R.id.sbPercent);

        sb.setProgress(60);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sv.setEraseSize(progress);
                tvSize.setText("画笔大小：" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sbMaxPercent.setProgress(40);
        sbMaxPercent.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sv.setMaxPercent(progress);
                tvPercent.setText("MaxPercent：" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sv.setEraseCallBack(new ScrapeView.EraseCallBack() {
            @Override
            public void erasing(int percent) {
                Log.d("Demo", "erasing: percent" + percent);
            }

            @Override
            public void erased() {
                Toast.makeText(MainActivity.this, "erased", Toast.LENGTH_SHORT).show();
                sv.clear();
            }
        });
    }

    public void waterMask(View view) {
        sv.setWaterMask(R.mipmap.ic_launcher);
    }

    public void clear(View view) {
        sv.clear();
    }

    public void click(View view) {
        Toast.makeText(this, "click", Toast.LENGTH_SHORT).show();
    }

    public void reset(View view) {
        sv.reset();
    }
}
