package com.mbcu.mmm.models.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.mbcu.mmm.utils.GsonUtils;
import com.mbcu.mmm.utils.MyUtils;

public class Config {
	private String net;
	private Credentials credentials;
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

	public String getNet() {
		return net;
	}

	public void setNet(String net) {
		this.net = net;
	}
	
	public ArrayList<BotConfig> getBots() {
		return bots;
	}
	
	public void setBots(ArrayList<BotConfig> bots) {
		this.bots = bots;
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
		
		res.setBotConfigMap(BotConfig.buildMap(res.getBots()));
		res.setBots(null);
		return res;
	}

}
