package jp.takke.tktrafficmonitorsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        {
            final Button button = (Button) findViewById(R.id.start_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final Intent service = new Intent(MainActivity.this, LayerService.class);
                    stopService(service);
                    startService(service);

                    final TextView kbText = (TextView) findViewById(R.id.preview_kb_text);
                    kbText.setText("-");
                }
            });

            // auto start
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.start_button).performClick();
                }
            }, 10);
        }

        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final TextView kbText = (TextView) findViewById(R.id.preview_kb_text);
                kbText.setText("" + (progress/10) + "." + (progress%10) + "KB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                final int progress = seekBar.getProgress();
                restartWithPreview(progress/10, progress%10);
            }
        });

        {
            final Button button = (Button) findViewById(R.id.stop_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopService(new Intent(MainActivity.this, LayerService.class));
                }
            });
        }

        final int[] sampleButtonIds = new int[]{R.id.sample_1kb_button,
                R.id.sample_20kb_button,
                R.id.sample_50kb_button,
                R.id.sample_80kb_button,
                R.id.sample_100kb_button
        };
        final int[] samples = new int[]{1, 20, 50, 80, 100};

        for (int i = 0; i < sampleButtonIds.length; i++) {

            final Button button = (Button) findViewById(sampleButtonIds[i]);
            final int kb = samples[i];

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    restartWithPreview(kb, 0);
                }
            });
        }
    }


    private void restartWithPreview(long kb, long kbd1) {

        final Intent service = new Intent(MainActivity.this, LayerService.class);

        service.putExtra("PREVIEW_RX_KB", kb);
        service.putExtra("PREVIEW_TX_KB", kb);
        service.putExtra("PREVIEW_RX_KBD1", kbd1);
        service.putExtra("PREVIEW_TX_KBD1", kbd1);

        stopService(service);
        startService(service);

        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress((int) (kb*10+kbd1));

        final TextView kbText = (TextView) findViewById(R.id.preview_kb_text);
        kbText.setText(kb + "." + kbd1 + "KB");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
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
