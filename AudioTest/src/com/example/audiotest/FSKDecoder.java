package com.example.audiotest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Vector;

import android.os.Handler;
import android.util.Log;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.Goertzel;

public class FSKDecoder extends Thread {

    private static int MINIMUM_BUFFER = 4410 * 2; // 0.1 sec
    private static int MAXIMUM_BUFFER = 441000 * 2; // 10 sec

    private boolean signalDetected = false;
    private static boolean forceStop;
    private Handler mClientHandler;
    private Deque<byte[]> mSound;
    private int totalBytes = 0;
    private int frontIndex = 0;

    private static String TAG = "FSKDecoder";

    public FSKDecoder(Handler handler){
        this.mClientHandler = handler;
        this.mSound = new ArrayDeque<byte[]>();
        this.forceStop = false;
    }

    public void run() {

        while (!this.forceStop) {

            try {
                // warning: if decode sound takes too much time/processing (as debugging a long array)
                // this loop stops working

                if (messageAvailable()){
                    decodeSound();
                } else
                    Thread.sleep(1*100); // wait to acq enough sound

            } catch (InterruptedException e) {
                Log.e("FSKDecoder:run", "error", e);
                e.printStackTrace();
            }
        }
        Log.i(TAG, "STOP run()");

    }

    public synchronized void stopAndClean(){
        Log.i(TAG, "STOP stopAndClean()");
        this.forceStop = true;
    }

    private synchronized boolean messageAvailable(){
        boolean available = false;
        if (totalBytes >= MINIMUM_BUFFER) available = true;
        Log.v(TAG, "messageAvailable()=" + available);
        return available;
    }

    public synchronized void addSound(byte[] sound, int nBytes){
        byte[] data = new byte[nBytes];
        for (int i = 0; i < nBytes; i++)
            data[i] = sound[i];

        this.mSound.add(data);
        totalBytes += nBytes;

        Log.i(TAG, "addSound nBytes="+ nBytes + " accumulated=" + totalBytes);

        if (this.mSound.size() > MAXIMUM_BUFFER ){
            Log.e(TAG, "ERROR addSound() buffer overflow size=" + totalBytes);
            // reset state and cleaning the buffer
            this.signalDetected = false;
            this.mSound.clear();
            totalBytes = 0;
            frontIndex = 0;
        }
    }

    private synchronized byte[] consumeSoundMessage(){
        byte[] sound = new byte[MINIMUM_BUFFER];

        int s = 0;
        for (int i = MINIMUM_BUFFER; i > 0;) {
            byte current[] = mSound.getFirst();

            if (current.length - frontIndex <= i) {
                i -= (current.length - frontIndex);
                for (int j = frontIndex; j < current.length; j++)
                    sound[s++] = current[j];

                frontIndex = 0;
                mSound.removeFirst();
            }
            else {
                for (; i > 0; i--)
                    sound[s++] = current[frontIndex++];
            }
        }

        totalBytes -= MINIMUM_BUFFER;

        Log.i(TAG, "FSKDEC:consumeSound() nBytes=" + sound.length);
        return sound;
    }

    private double[] byte2double(byte[] data){
        double d[] = new double[data.length/2];
        ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int counter = 0;
        while (buf.remaining() >= 2) {
            double s = buf.getShort();
            d[counter] = s;
            counter++;
        }
        return d;
    }

    private float[] byte2float(byte[] data){
        float d[] = new float[data.length/2];
        ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int counter = 0;
        while (buf.remaining() >= 2) {
            float s = buf.getShort();
            d[counter] = s;
            counter++;
        }
        return d;
    }

    private void decodeSound(){
        byte[] sound = consumeSoundMessage();
        Log.i(TAG, "decodeSound: length=" + sound.length);
        //this.decodeAmplitude(sound, sound.length);
        this.decodeFSK(sound);
    }

    private void decodeFSK(byte[] audioData) {
        //double[] sound = byte2double(audioData);
        float[] soundF = byte2float(audioData);
        AudioEvent aE = new AudioEvent();
        aE.setFloatBuffer(soundF);
        goertzelAudioProcessor.process(aE);

        Log.d(TAG, "decodeFSK: bytes length=" + audioData.length);
        Log.i(TAG, "decodeFSK: doubles length=" + soundF.length);

        try {

            //int message = FSKModule.decodeSound(sound);
            //Log.w(TAG, "decodeFSK():message=" + message + ":" + Integer.toBinaryString(message));
            //validate message integrity
            //message = ErrorDetection.decodeMessage(message);
            //Log.w(TAG, "decodeFSK():message number=" + message + ":" + Integer.toBinaryString(message));
            //this.mClientHandler.obtainMessage(AudioTestService.HANDLER_MESSAGE_FROM_ARDUINO, message, 0).sendToTarget();
        }
        catch (AudioTestException ae){
            Log.e(TAG, "decodeFSK():Androino ERROR="+ ae.getMessage());
            this.mClientHandler.obtainMessage(AudioTestService.HANDLER_MESSAGE_FROM_ARDUINO, ae.getType(), 0).sendToTarget();
        }
        catch (Exception e) {
            Log.e(TAG, "decodeFSK():ERROR="+ e.getMessage(), e);
            this.mClientHandler.obtainMessage(AudioTestService.HANDLER_MESSAGE_FROM_ARDUINO, -2, 0).sendToTarget();
        }
    }

    public static final double[] FREQUENCIES = { FSKModule.FREQUENCY_LOW, FSKModule.FREQUENCY_HIGH };

    private final AudioProcessor goertzelAudioProcessor = new Goertzel(AudioTestService.AUDIO_SAMPLE_FREQ,
            MINIMUM_BUFFER, FREQUENCIES, new Goertzel.FrequenciesDetectedHandler() {
        @Override
        public void handleDetectedFrequencies(final double[] frequencies, final double[] powers, final double[] allFrequencies, final double allPowers[]) {
            String x = "";
            for (int i = 0; i < frequencies.length; i++) {
                // frequency, power check
                x += frequencies[i] + "Hz : " + powers[i] + ", ";
            }
            Log.w(TAG, "decodeFSK(): " + x);
        }
    });
}