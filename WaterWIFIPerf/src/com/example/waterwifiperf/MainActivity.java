package com.example.waterwifiperf;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;


//시간/SSID/RSSI/DOWNTCP/UPTCP/DOWNUDP/UPUDP/RTT
//시간=count*time metric


import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//어쩌면 	android.telephony.SignalStrength로 3G측정

public class MainActivity extends Activity implements OnClickListener{
	public TextView resultView;
	public boolean isRunning;
	public final Handler handler = new Handler();
	public TimerTask second;
	public Timer timer;
	public List<ScanResultData> scanResultList;
	public Button btnStart;
	public Button btnReset;
	public Button btnSave;
	public EditText editTextRate;
	
	public int frameCount=0;
	public long time;
	
	public int rate=1000;
	
	public ConnectivityManager connManager;
	public NetworkInfo mWifi;
	
	public String testhost = "1216";
	public String testhostip = "127.0.0.1";
	public void testStart() {
		isRunning = true;
		second = new TimerTask() {
			@Override
			public void run() {
				Update();
			}
		};
		
		timer = new Timer();
		timer.schedule(second, 0, rate);
	}
	
	public void testStop(){
		isRunning = false;
		timer.cancel();
         // 대기중이던 취소된 행위가 있는 경우 모두 제거한다.
		time = 0;
		frameCount = 0;
		timer.purge();
		unregisterReceiver(mReceiver); // stop WIFISCan
		timer = null;
	}
	
	public void Update(){
		Runnable updater = new Runnable() {
			public void run() {
				wifimanager.startScan();
				++frameCount;
				time = frameCount*rate;
				
				resultView.setText("");
				for(int i = 0; i<scanResultList.size();++i){
					//resultView.append(scanResultList.get(i).time+"ms/"+((mWifi.isConnected())? "Connected":"Disconnected")+"/"+scanResultList.get(i).SSID+"/"+scanResultList.get(i).RSSI+"dBm/"+scanResultList.get(i).RTT+"\n");
					resultView.append((double)scanResultList.get(i).time/1000+"s/"+((mWifi.isConnected())? "Connected":"Disconnected")+"/"+scanResultList.get(i).SSID+"/"+scanResultList.get(i).RSSI+"dBm/"+"\n");
				}
			}
		};
		handler.post(updater);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		scanResultList = new ArrayList<ScanResultData>();
		setContentView(R.layout.activity_main);
		wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);

		// if WIFIEnabled
		if (wifimanager.isWifiEnabled() == false){
			wifimanager.setWifiEnabled(true);
		}
		
		initWIFIScan();
		
		resultView = (TextView)findViewById(R.id.SensingResultTextView);
		btnStart = (Button)findViewById(R.id.ButtonStart);
		btnReset = (Button)findViewById(R.id.ButtonReset);
		btnSave = (Button)findViewById(R.id.ButtonSave);
		
		editTextRate = (EditText)findViewById(R.id.EditTextSensingRate);
		
		btnStart.setOnClickListener(this);
		btnReset.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		
		/*String service = Context.CONNECTIVITY_SERVICE;
		ConnectivityManager connectivity = (ConnectivityManager) getSystemService(service);

		NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
		int networkType = activeNetwork.getType();

		switch (networkType) {
		case (ConnectivityManager.TYPE_MOBILE) : break;
		case (ConnectivityManager.TYPE_WIFI) : break;
		default: break;
		}

		// Get the mobile network information.
		int network = ConnectivityManager.TYPE_MOBILE;
		NetworkInfo mobileNetwork = connectivity.getNetworkInfo(network);

		NetworkInfo.State state = mobileNetwork.getState();
		NetworkInfo.DetailedState detailedState = mobileNetwork.getDetailedState();*/
		/*
		 * List<WifiConfiguration> l =  wim.getConfiguredNetworks(); 
WifiConfiguration wc = l.get(0); 
tv.append("\n"+ Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress()));
		 */
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		WifiInfo info = wifimanager.getConnectionInfo();
		List<WifiConfiguration> l =  wifimanager.getConfiguredNetworks(); 
		WifiConfiguration wc = l.get(0);
		
		if (info.getBSSID() != null) {
			int strength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
			int speed = info.getLinkSpeed();
			String units = WifiInfo.LINK_SPEED_UNITS;
			String ssid = info.getSSID();
			testhost = info.getSSID();
			testhost = testhost.replace("\"", "");
			
			testhostip =  convertIntegerToIp(info.getIpAddress());
			Log.d("oncreate",testhostip);
			String cSummary = String.format("Connected to %s at %s%s. Strength %s/5", ssid, speed, units, strength);
			resultView.setText(cSummary);
		}
	}
	
	public static String convertIntegerToIp(long ip){
		StringBuffer buf = new StringBuffer();
		buf.append(((ip >> 24) & 0xFF)).append(".")
		.append(((ip >> 16) & 0xFF)).append(".")
		.append(((ip >> 8) & 0xFF)).append(".")
		.append(( ip & 0xFF));
		return buf.toString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
	        case R.id.ButtonStart:
	            if(isRunning){
	            	printToast("stop");
	            	btnStart.setText("Start");
	            	testStop();
	            }
	            else{
	            	if(Integer.valueOf(editTextRate.getText().toString())>0){
	            		scanResultList.clear();
	            		initWIFIScan();
	            		this.rate=Integer.valueOf(editTextRate.getText().toString());
	            		btnStart.setText("Stop");
		            	testStart();
	            	}
	            	else{
	            		printToast("Input Sensing Rate");
	            	}
	            }
	            break;
	        case R.id.ButtonSave:
	        	saveResult();
	            break;
	        case R.id.ButtonReset:
	        	if(isRunning){
	        		testStop();
	        		btnStart.setText("Start");
	        	}
	            resultView.setText("Clear");
	            break;
	        default:
	            break;
		}
	}
	
	void saveResult(){
		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyyMMdd(HH-mm-ss)", Locale.KOREA );
		Date currentTime = new Date ( );
		String mTime = mSimpleDateFormat.format ( currentTime );
		File file = new File(this.getFilesDir(), mTime );
		printToast("Saved as "+mTime);
		
		FileOutputStream outputStream;

		try {
		  outputStream = openFileOutput(mTime, Context.MODE_PRIVATE);
		  outputStream.write(resultView.getText().toString().getBytes());
		  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}
		if(isRunning){
			testStop();
		}
		btnStart.setText("Start");
        resultView.setText("Saved");
	}
	
	///////////////////////////////////////////////////////////
	//Wifi Scanner
	////////////////////////////////////////////////////////////
	
	public List<ScanResult> mScanResult; // ScanResult List
	public WifiManager wifimanager;
	public int scanCount;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				getWIFIScanResult(); // get WIFISCanResult
				wifimanager.startScan(); // for refresh
			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
			}
		}
	};
	
	public void getWIFIScanResult() {
		mScanResult = wifimanager.getScanResults(); // ScanResult
		int level=0;
		for (int i = 0; i < mScanResult.size(); i++) {
			Log.d("wwaterperfscanresult",testhost);
			ScanResult result = mScanResult.get(i);
			
			if(result.SSID.toString().equals(testhost)){	
				ScanResultData scanData = new ScanResultData();
				scanData.time = time;
				scanData.SSID = result.SSID.toString();
				Log.d("wwaterperfscanresult",scanData.SSID);
				scanData.RSSI = result.level;
				/*try {
					scanData.RTT = ping(testhostip);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
				
				scanResultList.add(scanData);
			
				
			}
		}
	}

	public void initWIFIScan() {
		scanCount = 0;
		final IntentFilter filter = new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mReceiver, filter);
		wifimanager.startScan();
		
	}

	public void printToast(String messageToast) {
		Toast.makeText(this, messageToast, Toast.LENGTH_LONG).show();
	}

	/*http://stackoverflow.com/questions/7451200/how-to-icmp-ping-on-android*/   
    public static String pingError = null;
    /**
     * Ping a host and return an int value of 0 or 1 or 2 0=success, 1=fail, 2=error
     * 
     * Does not work in Android emulator and also delay by '1' second if host not pingable
     * In the Android emulator only ping to 127.0.0.1 works
     * 
     * @param String host in dotted IP address format
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int pingHost(String host) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec("ping -c 1 " + host);
        proc.waitFor();     
        int exit = proc.exitValue();
        return exit;
    }

    public double ping(String host) throws IOException, InterruptedException {
    	
    	Log.d("WaterWIFIPerf","ping:"+host);
    	
    	StringBuffer echo = new StringBuffer();
        Runtime runtime = Runtime.getRuntime();
        
        Process proc = runtime.exec("ping -c 1 " + host);
        
        proc.waitFor();
        int exit = proc.exitValue();
        if (exit == 0) {
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            BufferedReader buffer = new BufferedReader(reader);
            String line = "";
            while ((line = buffer.readLine()) != null) {
                echo.append(line + "\n");
            }           
            return getPingStats(echo.toString());   
        } else if (exit == 1) {
            pingError = "failed, exit = 1";
            return -1000;            
        } else {
            pingError = "error, exit = 2";
            return -2000;    
        }       
    }

    /**
     * getPingStats interprets the text result of a Linux ping command
     * 
     * Set pingError on error and return null
     * 
     * http://en.wikipedia.org/wiki/Ping
     * 
     * PING 127.0.0.1 (127.0.0.1) 56(84) bytes of data.
     * 64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.251 ms
     * 64 bytes from 127.0.0.1: icmp_seq=2 ttl=64 time=0.294 ms
     * 64 bytes from 127.0.0.1: icmp_seq=3 ttl=64 time=0.295 ms
     * 64 bytes from 127.0.0.1: icmp_seq=4 ttl=64 time=0.300 ms
     *
     * --- 127.0.0.1 ping statistics ---
     * 4 packets transmitted, 4 received, 0% packet loss, time 0ms
     * rtt min/avg/max/mdev = 0.251/0.285/0.300/0.019 ms
     * 
     * PING 192.168.0.2 (192.168.0.2) 56(84) bytes of data.
     * 
     * --- 192.168.0.2 ping statistics ---
     * 1 packets transmitted, 0 received, 100% packet loss, time 0ms
     *
     * # ping 321321.
     * ping: unknown host 321321.
     * 
     * 1. Check if output contains 0% packet loss : Branch to success -> Get stats
     * 2. Check if output contains 100% packet loss : Branch to fail -> No stats
     * 3. Check if output contains 25% packet loss : Branch to partial success -> Get stats
     * 4. Check if output contains "unknown host"
     * 
     * @param s
     */
    public static double getPingStats(String s) {
        if (s.contains("0% packet loss")) {
            int start = s.indexOf("/mdev = ");
            int end = s.indexOf(" ms\n", start);
            s = s.substring(start + 8, end);            
            String stats[] = s.split("/");
            return Double.valueOf(stats[2]);
        } else if (s.contains("100% packet loss")) {
            pingError = "100% packet loss";
            return -100;            
        } else if (s.contains("% packet loss")) {
            pingError = "partial packet loss";
            return -50;
        } else if (s.contains("unknown host")) {
            pingError = "unknown host";
            return -1;
        } else {
            pingError = "unknown error in getPingStats";
            return -2;
        }       
    }
    
               
    /*http://stackoverflow.com/questions/8252656/how-to-get-download-speed-in-android-device-or-mobile*/
    /*http://www.techrepublic.com/blog/app-builder/create-a-network-monitor-using-androids-trafficstats-class/774*/
    /*
     * long rxBytes = TrafficStats.getTotalRxBytes()- mStartRX;
RX.setText(Long.toString(rxBytes));
long txBytes = TrafficStats.getTotalTxBytes()- mStartTX;
TX.setText(Long.toString(txBytes));
     */
    /*http://developer.android.com/reference/android/net/TrafficStats.html*/
    
}
