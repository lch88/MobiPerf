package com.example.audiotest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private Handler mHandler;
    private AudioTestService mArduinoS;
    private int lastMessage;
    private int lastMessageCounter=0;
    private int checksumErrorCounter=0;

    public static final int LAST_MESSAGE_MAX_RETRY_TIMES= 5; //Number of times that last message is repeated

    public static final int ARDUINO_PROTOCOL_ARQ            = 20; //Automatic repeat request
    public static final int ARDUINO_PROTOCOL_ACK            = 21; //Message received acknowledgment

    public MainActivity(){
        this.mHandler = new Handler(){
            public void handleMessage(Message msg) {
                messageReceived(msg);
            }
        };
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.Button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startStop();
            }
        });
        final Button sendB = (Button) findViewById(R.id.SendButton);
        sendB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    TextView txt = (TextView) findViewById(R.id.NumberText);
                    int number = Integer.parseInt(""+txt.getText());
                    showDebugMessage("Send " + number, true);
                    sendMessage(number);
                } catch (Exception e) {
                    showDebugMessage("ERROR happened, check number format",true);
                }
            }
        });

    }


    private void startStop(){
        RadioButton radio = (RadioButton) findViewById(R.id.RadioButton01);
        if (radio.isChecked()){
            // stop
            stop();
            showDebugMessage(">>Service stoped", true);
        } else {
            // start
            start();
            showDebugMessage(">>Service started", true);
        }
        // update UI
        radio.setChecked(! radio.isChecked());
    }


    void showDebugMessage(String message, boolean showToast){
        showToast = false;
        try {
            if (showToast){
                Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                TextView txt = (TextView) findViewById(R.id.DebugText);
                String info = txt.getText().toString();
                if (info.length()> 300)
                    info = info.substring(0, 30);
                info = message + "\n" + info;
                txt.setText(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR showDebugMessage()=" + message, e);
        }
    }

    //-----------------------------------------------
    //
    //-----------------------------------------------
    public void stop(){
        // STOP the arduino service
        if (this.mArduinoS != null)
            this.mArduinoS.stopAndClean();
    }

    public void start(){
        // START the arduino service
        this.mArduinoS = new AudioTestService(this.mHandler);
        new Thread(this.mArduinoS).start();
    }

    private void sendLastMessage(){
        if (lastMessageCounter> LAST_MESSAGE_MAX_RETRY_TIMES) {
            // stop repeating last message, ERROR
            this.showDebugMessage("ERROR sending" + this.lastMessage, false);
            this.writeMessage(ARDUINO_PROTOCOL_ACK); // send ack to avoid ARQ
            lastMessageCounter=0;
        } else {
            this.sendMessage(lastMessage);
            lastMessageCounter++;
        }
    }

    private void sendMessage(int number){
        checksumErrorCounter=0;
        this.lastMessage = number;
        this.writeMessage(number);
    }

    private void writeMessage(int number){
        this.mArduinoS.write(number);
        showDebugMessage("W:"+number, false);
    }

    private void messageReceived(Message msg) {
//              this.showDebugMessage("Received:"+msg.arg1, false);
        int value = msg.arg1;
        showDebugMessage("...." + msg.getWhen() + "Msg Received msg="+value , true);
        switch (value) {
            case ARDUINO_PROTOCOL_ARQ:
                checksumErrorCounter=0;
                showDebugMessage("....ARQ "+value, true);
                sendLastMessage();
                break;
            case ErrorDetection.CHECKSUM_ERROR:
                checksumErrorCounter++;
                if (checksumErrorCounter>1){
                    writeMessage(ARDUINO_PROTOCOL_ARQ); //ARQ is two consecutive CHK ERROR received
                    showDebugMessage("....CHK ERROR "+value, true);
                    checksumErrorCounter=0;
                } else
                    showDebugMessage("....CHK ERROR skipped"+value, true);
                break;
            case AudioTestService.RECORDING_ERROR:
                checksumErrorCounter=0;
                showDebugMessage("RECORDING ERROR "+value, false);
                break;
            default:
                checksumErrorCounter=0;
                showDebugMessage("R:"+value, false);
                this.writeMessage(ARDUINO_PROTOCOL_ACK);
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
