package com.mobiperf.util;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.widget.Toast;

import com.mobiperf.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IOUtils {

  public static final String ROOT_PATH = "/mobiperf";
  public static final String OUTPUT_PATH = "/results";
  public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

  public static void writeResultToFile(int id, String mType, String mDir, String rslt) {
    String filename = String.format("%04d_%s_%s_%s.txt", id, mType, mDir, dateFormat.format(new Date()));
    String path = "/mnt/sdcard" + ROOT_PATH + OUTPUT_PATH;
    File resultPath = new File(path);
    Logger.d(path);
    if (!resultPath.exists()) {
      resultPath.mkdirs();
    }
    File resultFile = new File(path, filename);
    _writeToFile(resultFile, rslt);
  }
  
  private static boolean _writeToFile(File file, String data) {
    boolean result = true;
    try {
      FileWriter fileWriter = new FileWriter(file);
      BufferedWriter out = new BufferedWriter(fileWriter);
      out.write(data);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  public static boolean isExternalStorageReady() {

    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWritable = false;
    String state = Environment.getExternalStorageState();

    if (Environment.MEDIA_MOUNTED.equals(state)) {
      mExternalStorageAvailable = mExternalStorageWritable = true;
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      mExternalStorageAvailable = true;
      mExternalStorageWritable = false;
    } else {
      Logger.d("External Storage not available");
    }
    return mExternalStorageWritable;
  }
}
