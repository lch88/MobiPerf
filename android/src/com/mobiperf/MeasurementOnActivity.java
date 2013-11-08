package com.mobiperf;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MeasurementOnActivity extends Activity {

  LinearLayout layout;
  TextView textView;

  long CD_MILLIS = 5000;
  long EXP_MILLIS = 20 * 1000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.empty);

    layout = (LinearLayout) findViewById(R.id.measuringLayout);
    textView = (TextView) findViewById(R.id.statusTextView);
  }

  @Override
  protected void onStart() {
    super.onStart();

    layout.setBackgroundColor(Color.RED);
    new CountDownTimer(CD_MILLIS + 500, 1000) {
      public void onTick(long millisUntilFinished) {
        textView.setText(Long.toString(millisUntilFinished / 1000));
      }

      public void onFinish() {
        textView.setText("Start!");
        sendBroadcast(new UpdateIntent("", UpdateIntent.MEASUREMENT_ACTION));
        measuring();
      }
    }.start();
  }

  public void measuring() {
    layout.setBackgroundColor(Color.CYAN);
    new CountDownTimer(EXP_MILLIS + 100, 1000) {
      public void onTick(long millisUntilFinished) {
        textView.setText(Long.toString(millisUntilFinished / 1000));
      }

      public void onFinish() {
        textView.setText("Done!");
      }
    }.start();
  }
}
