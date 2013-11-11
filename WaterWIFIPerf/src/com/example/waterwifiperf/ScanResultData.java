package com.example.waterwifiperf;
//시간/SSID/RSSI/DOWNTCP/UPTCP/DOWNUDP/UPUDP/RTT
//시간=count*time metric
public class ScanResultData {
	public long time;
	public String SSID;
	public double RSSI;
	public double RTT;
}
