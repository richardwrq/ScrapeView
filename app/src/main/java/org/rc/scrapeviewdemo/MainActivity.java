package org.rc.scrapeviewdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.rc.scrapelayout.R;
import org.rc.scrapeview.ScrapeView;

public class MainActivity extends AppCompatActivity {

    private ScrapeView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sv = (ScrapeView) findViewById(R.id.sv);
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
