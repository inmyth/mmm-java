package com.mbcu.mmm.models.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.mbcu.mmm.utils.GsonUtils;
import com.mbcu.mmm.utils.MyUtils;

public class Config {
	private static final int DEFAULT_INTERVAL_BALANCER = 4;
	private static final int DEFAULT_INTERVAL_ACCOUNT_BALANCE = 2;
	public static final int HOUR_ACCOUNT_BALANCER_EMAILER = 6;
	
	private String net;
	private String datanet;
	private Credentials credentials;
	private ArrayList<String> emails;
	private Intervals intervals;
	private transient HashMap<String, BotConfig> botConfigMap;
	private ArrayList<BotConfig> bots;

	public void setBotConfigMap(HashMap<String, BotConfig> botConfigMap) {
		this.botConfigMap = botConfigMap;
	}

	public HashMap<String, BotConfig> getBotConfigMap() {
		return botConfigMap;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void setDatanet(String datanet) {
		this.datanet = datanet;
	}
	
	public String getDatanet() {
		return datanet;
	}
	
	public String getNet() {
		return net;
	}

	public void setNet(String net) {
		this.net = net;
	}
	
	public Intervals getIntervals() {
		return intervals;
	}
	
	public void setIntervals(Intervals intervals) {
		this.intervals = intervals;
	}
	
	
	
	public ArrayList<BotConfig> getBots() {
		return bots;
	}
	
	public void setBots(ArrayList<BotConfig> bots) {
		this.bots = bots;
	}
	
	public ArrayList<String> getEmails() {
		return emails;
	}

	public static Config build(String fileName) throws IOException{
		String raw = MyUtils.readFile(fileName);
		
		Config res = GsonUtils.toBean(raw, Config.class);
		if (res == null){
			throw new IOException("Failed to parse config");
		}
		
		if (res.bots.isEmpty()){
			throw new IOException("Bots are empty");
		}
		
		if (res.intervals == null){
			Intervals intervals = new Intervals();
			intervals.accountBalance = DEFAULT_INTERVAL_ACCOUNT_BALANCE;
			intervals.balancer 			 = DEFAULT_INTERVAL_BALANCER;
			res.setIntervals(intervals);
		} else {
			if (res.intervals.accountBalance < DEFAULT_INTERVAL_ACCOUNT_BALANCE){
				res.intervals.accountBalance = DEFAULT_INTERVAL_ACCOUNT_BALANCE;
			}
			if (res.intervals.balancer < DEFAULT_INTERVAL_BALANCER){
				res.intervals.balancer = DEFAULT_INTERVAL_BALANCER;
			}			
		}
		
		int s = res.emails.stream()
		.filter(MyUtils::isEmail)
		.collect(Collectors.toList())
		.size();
		
		if (s != res.emails.size()){
			throw new IOException("Wrong email format"); 
		}
		
		res.setBotConfigMap(BotConfig.buildMap(res.getCredentials(), res.getBots()));
		res.setBots(null);
		return res;
	}
	

}
