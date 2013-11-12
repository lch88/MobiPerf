package com.example.audiotest;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;


import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.DTMF;
import be.hogent.tarsos.dsp.Goertzel;

public class AudioTestService implements Runnable {

    public static final int EMULATOR_SAMPLE_FREQ = 8000; // emulator sample freq 8000Hz
    public static final int GT540_SAMPLE_FREQ = 44100; // LG GT540
    public static final int AUDIO_SAMPLE_FREQ = GT540_SAMPLE_FREQ;
    public static int ACQ_AUDIO_BUFFER_SIZE = 16000;

    private static final String TAG = "ArduinoService";

    private Handler mClientHandler;
    private FSKDecoder mDecoder;
    private static boolean forceStop = false;
    public static final int HANDLER_MESSAGE_FROM_ARDUINO = 2000;
    public static final int RECORDING_ERROR = -1333;

    public AudioTestService(Handler handler) {
        this.mClientHandler = handler;
    }

    @Override
    public void run() {
        this.forceStop = false;
        // Decoder initialization
        this.mDecoder = new FSKDecoder(this.mClientHandler);
        this.mDecoder.start();

        // Sound recording loop
        this.audioRecordingRun();

        // DEVELOPMENTDUMMYARDUINO
        //dummyRecordingRun();
    }

    public synchronized void write(int message) {
        encodeMessage(message);
    }
    public void stopAndClean() {
        Log.i(TAG, "STOP stopAndClean():");
        this.forceStop = true;
    }

    private void dummyRecordingRun(){
        while (!forceStop) {
            double num = Math.random();
            if (num>0.8) {
                int n = 20 + (int) (10.0 * Math.random());
                //this.mClientHandler.obtainMessage(HANDLER_MESSAGE_FROM_ARDUINO,n,0).sendToTarget();
            }
            Log.i(TAG, "dummyRecordingRun()");
            try {Thread.sleep(2*1000);} catch (InterruptedException e) {e.printStackTrace();}
        }
    }

    private void audioRecordingRun(){
        try {

            int AUDIO_BUFFER_SIZE = ACQ_AUDIO_BUFFER_SIZE; //44000;//200000;// 16000;
            int minBufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_FREQ, 2, AudioFormat.ENCODING_PCM_16BIT);
            if (AUDIO_BUFFER_SIZE < minBufferSize) AUDIO_BUFFER_SIZE = minBufferSize;

            Log.i(TAG, "buffer size:" + AUDIO_BUFFER_SIZE);
            byte[] audioData = new byte[AUDIO_BUFFER_SIZE];

            AudioRecord aR = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_FREQ, 2, AudioFormat.ENCODING_PCM_16BIT,
                    AUDIO_BUFFER_SIZE);

            // prepare an input stream (probably pipestream)
            PipedOutputStream byteOutStream = new PipedOutputStream();
            PipedInputStream byteInStream = new PipedInputStream(byteOutStream);

            // make an audio input stream with AudioSystem.NOT_SPECIFIED length

            // define a dispatcher

            // add goertzel processor

            // start

            // audio recording
            aR.startRecording();
            int nBytes = 0;
            int index = 0;
            this.forceStop = false;
            // continuous loop
            while (true) {
                nBytes = aR.read(audioData, index, AUDIO_BUFFER_SIZE);
                Log.d(TAG, "audio acq: length=" + nBytes);
                // Log.v(TAG, "nBytes=" + nBytes);
                if (nBytes < 0) {
                    Log.e(TAG, "audioRecordingRun() read error=" + nBytes);
                    this.mClientHandler.obtainMessage(AudioTestService.HANDLER_MESSAGE_FROM_ARDUINO, RECORDING_ERROR, 0).sendToTarget();
                }

                this.mDecoder.addSound(audioData, nBytes);

                // add bytes to audio input stream

                if (this.forceStop) {
                    this.mDecoder.stopAndClean();
                    break;
                }
            }

            aR.stop();
            aR.release();

        }
        catch (IOException e) {
            Log.i(TAG, "ERRORs, IOException" + e);
        }
        finally {
            Log.i(TAG, "STOP audio recording stoped");
        }
    }

    private void encodeMessage(int value) {
        // audio initialization
        int AUDIO_BUFFER_SIZE = 16000;
        int minBufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_FREQ, 2, AudioFormat.ENCODING_PCM_16BIT);
        if (AUDIO_BUFFER_SIZE < minBufferSize) AUDIO_BUFFER_SIZE = minBufferSize;
        AudioTrack aT = new AudioTrack(AudioManager.STREAM_MUSIC,
                AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUFFER_SIZE,
                AudioTrack.MODE_STREAM);
        aT.play();

        //error detection encoding
        Log.i(TAG, "encodeMessage() value=" + value);
        value = ErrorDetection.createMessage(value);
        Log.i(TAG, "encodeMessage() message=" + value);
        // sound encoding
        double[] sound = FSKModule.encode(value);

        ByteBuffer buf = ByteBuffer.allocate(4 * sound.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sound.length; i++) {
            int yInt = (int) sound[i];
            buf.putInt(yInt);
        }
        byte[] tone = buf.array();
        // play message
        int nBytes = aT.write(tone, 0, tone.length);

        aT.stop();
        aT.release();
    }


}