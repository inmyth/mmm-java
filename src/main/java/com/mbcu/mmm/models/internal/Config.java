package com.mbcu.mmm.models.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.annotations.Expose;
import com.mbcu.mmm.utils.GsonUtils;
import com.mbcu.mmm.utils.MyUtils;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.types.known.sle.entries.Offer;

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
	
	
	public BotConfig isPairMatched(Offer offer){
		BotConfig res = null;
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		String at1 = OfferI.pairFrom(executed.get(Amount.TakerGets), executed.get(Amount.TakerPays));
		res = botConfigMap.get(at1);
		if (res != null){
			return res;
		}		
		String at2 = OfferI.pairFrom(executed.get(Amount.TakerPays), executed.get(Amount.TakerGets));
		res = botConfigMap.get(at2);
		if (res != null){
			return res;
		}
		return null;
	}


}
