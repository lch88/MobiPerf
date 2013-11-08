package com.mobiperf.util;

import android.os.Environment;
import com.mobiperf.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: changhoon
 * Date: 2013. 11. 8.
 * Time: PM 4:49
 * To change this template use File | Settings | File Templates.
 */
public class IOUtils {

  public static final String ROOT_PATH = "/mobiperf";
  public static final String OUTPUT_PATH = "/results";

  public static void writeResultToFile(int id, String mType, String mDir, String rslt) {
    String filename = String.format("%04d_%s_%s.txt", id, mType, mDir);
    String path = Environment.getExternalStorageDirectory() + ROOT_PATH + OUTPUT_PATH;
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
