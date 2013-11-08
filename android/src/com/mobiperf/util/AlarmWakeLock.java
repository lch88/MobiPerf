package com.mobiperf.util;

import android.content.Context;
import android.os.PowerManager;

/**
 * User: Changhoon
 * Date: 13. 11. 7
 * Time: 오후 8:53
 * Source from: 아크사 (http://blog.naver.com/PostView.nhn?blogId=s145&logNo=140153041039)
 */

public class AlarmWakeLock {
  private static final String TAG = "AlarmWakeLock";
  private static PowerManager.WakeLock mWakeLock;

  public static void wakeLock(Context context) {
    if(mWakeLock != null) {
      return;
    }

    PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

    mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
    mWakeLock.acquire();
  }

  public static void releaseWakeLock() {
    if(mWakeLock != null) {
      mWakeLock.release();
      mWakeLock = null;
    }
  }
}
