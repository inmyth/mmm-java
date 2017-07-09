package com.mbcu.mmm.sequences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONObject;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.Events.WSError;
import com.mbcu.mmm.main.Events.WSGotText;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.Common.OnOfferCanceled;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnOfferCreated;
import com.mbcu.mmm.sequences.Common.OnOfferEdited;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.sequences.Common.OnResponseFail;
import com.mbcu.mmm.sequences.Common.OnResponseSuccess;
import com.mbcu.mmm.utils.GsonUtils;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.AccountIDField;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.Hash256Field;
import com.ripple.core.fields.UInt32Field;
import com.ripple.core.serialized.enums.EngineResult;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;

import io.reactivex.schedulers.Schedulers;

public class GCommon extends Base {

	public GCommon(Config config) {
		super(MyLogger.getLogger(GCommon.class.getName()), config);
		this.config = config;
		String subscribeRequest = Subscribe
				.build(Command.SUBSCRIBE)
//				.withOrderbookFromConfig(config)
				.withAccount(config.getCredentials().getAddress())
				.stringify();

		bus.toObservable()
				 .subscribeOn(Schedulers.newThread())
				
				.subscribe(o -> {
					if (o instanceof Events.WSConnected) {
						log("connected", Level.FINER);
						log("Sending subsribe request");
						log(subscribeRequest);
						bus.send(new Events.WSRequestSendText(subscribeRequest));
					} else if (o instanceof Events.WSDisconnected) {
						log("disconnected");
					} else if (o instanceof Events.WSError) {
						Events.WSError event = (WSError) o;
						log(event.e.getMessage(), Level.SEVERE);
					} else if (o instanceof Events.WSGotText) {
//						if (!isSent){
//							for (int seq = 5968 ; seq < 5970; seq ++){						
//								bus.send(new Events.WSRequestSendText(sign(seq, config)));
//							}
//							isSent = true;
//						}
						
						Events.WSGotText event = (WSGotText) o;
						log(event.raw, Level.FINER);						
						reroute(event.raw);
					}
				});
	}
	
	private void reroute(String raw) throws Exception {
		if (raw.contains("response")) {
			filterResponse(raw);
		} else if (raw.contains("transaction")) {
			filterStream(raw);
		}
	}
	
	
	private void filterResponse(String raw){
		JSONObject whole = new JSONObject(raw);	
		JSONObject result = whole.optJSONObject("result");
		
		if (result.has("tx_json")){
			if ((raw.contains("OfferCreate") || raw.contains("OfferCancel"))){
				Hash256 hash = Hash256.fromHex(result.optJSONObject("tx_json").optString("hash"));
				AccountID accId = AccountID.fromAddress(result.optJSONObject("tx_json").optString("Account"));
				String engResult = result.optString("engine_result");					
				UInt32 sequence = new UInt32(result.getJSONObject("tx_json").getInt("Sequence"));
				log(engResult + " " +  accId + " ,hash " + hash + " ,seq" + sequence);
				if (engResult.equals(EngineResult.tesSUCCESS.toString())){
					bus.send(new OnResponseSuccess(accId, hash, sequence));
				}else{				
					bus.send(new OnResponseFail(engResult, accId, hash, sequence));		}	
			}		
		}else if (result.has("account_data")){
			UInt32 sequence = new UInt32(result.getJSONObject("account_data").getInt("Sequence"));
			bus.send(new OnAccountInfoSequence(sequence));	
		}

	}

	public void filterStream(String raw) {
		if (!raw.contains("tesSUCCESS") && 
				!(raw.contains("OfferCreate") || raw.contains("Payment") || raw.contains("OfferCancel"))){
			log("Stream parse condition failed : " + raw, Level.WARNING);
			return;
		}
		
		if (!raw.contains(config.getCredentials().getAddress())){
			log("Not related to our order : " + raw, Level.WARNING);
			return;
		}
				
		ArrayList<RLOrder> oes = new ArrayList<>();
		Offer offerCreated = null;
		JSONObject transaction = new JSONObject(raw);

		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));
		AccountID txnAccId = txn.account();
		Hash256 txnHash = txn.hash();
		UInt32 txnSequence = txn.sequence();

		ArrayList<AffectedNode> deletedNodes = new ArrayList<>();
		ArrayList<Offer> offersExecuteds = new ArrayList<>();

		for (AffectedNode node : meta.affectedNodes()) {
			if (!node.isCreatedNode()) {
				LedgerEntry asPrevious = (LedgerEntry) node.nodeAsPrevious();
				if (node.isDeletedNode()) {
					deletedNodes.add(node);
				}
				if (asPrevious instanceof Offer) {
					offersExecuteds.add((Offer) asPrevious);
				}
			} else {
				LedgerEntry asFinal = (LedgerEntry) node.nodeAsPrevious();
				if (asFinal instanceof Offer) {
					Offer offer = (Offer) asFinal;
					offerCreated = offer;
				}
			}
		}

		Hash256 previousTxnId = null;
		for (AffectedNode deletedNode : deletedNodes) {
			LedgerEntry le = (LedgerEntry) deletedNode.nodeAsFinal();
			previousTxnId = le.get(Hash256.PreviousTxnID);
			if (previousTxnId != null) {
				break;
			}
		}

		String txType = txn.get(Field.TransactionType).toString();
		if (txType.equals("OfferCancel")) {
			log("CANCELED Account: " + txn.account().address + " Seq: " + txn.sequence() + "  prevTxnId: " + previousTxnId);
			bus.send(new OnOfferCanceled(txn.account(), txn.sequence(), previousTxnId));
			return;
		}

		if (txType.equals("OfferCreate")) {
			RLOrder offerCreate = RLOrder.fromOfferCreate(txn);
			log("OFFER CREATE Account: " + txnAccId + " Hash " + txnHash + " Sequence " + txnSequence + "\n" + GsonUtils.toJson(offerCreate));
			// OnOfferCreate event is only needed to increment sequence.
			bus.send(new OnOfferCreate(txnAccId, txnHash, txnSequence));

			if (offerCreated != null) {
				AccountID ocAccId = offerCreated.account();		
				if (previousTxnId == null) {
					log("OFFER CREATED OCID " + ocAccId + " TxnID " + txnAccId + " OCPrevTxnId " + previousTxnId + " \n" + offerCreated.prettyJSON() );
//					bus.send(new OnOfferCreated(txnAccId, ocAccId, offerCreated.previousTxnID(), rlOfferCreated));				
				}else{
					log("EDITED " + previousTxnId + " to " + txn.hash() + " \n" + offerCreated.prettyJSON());
//					bus.send(new OnOfferEdited(ocAccId, txnHash, previousTxnId, rlOfferCreated));
				}
			}
			

			
//			if (txn.account().address.equals(this.config.getCredentials().getAddress())) {
//				FilterAutobridged fa = new FilterAutobridged();
//				for (Offer offer : offersExecuteds) {
//					STObject finalFields = offer.get(STObject.FinalFields);
//					if (finalFields != null) {
//						fa.push(offer);
//					}
//				}
//				oes.addAll(fa.process());
//			}else{
//				for (Offer offer : offersExecuteds) {
//					STObject finalFields = offer.get(STObject.FinalFields);
//					
//					if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
//						oes.add(RLOrder.fromOfferExecuted(offer, true));
//					}
//				}
//			}
		}
		
		if (txType.equals("OfferCreate") || txType.equals("Payment")){
			for (Offer offer : offersExecuteds) {		
				FilterAutobridged fa = new FilterAutobridged();

				STObject finalFields = offer.get(STObject.FinalFields);
				if (finalFields != null) {				
					log("OE" + offer.get(Hash256.AccountHash).toString() + " " + offer.get(UInt32.Sequence) + " \n " + offer.prettyJSON() );
				}
			}
		}
		
//		else if (txType.equals("Payment") && !txn.account().address.equals(config.getCredentials().getAddress())) {
//			// we only care about payment not from ours.
//			for (Offer offer : offersExecuteds) {
//				STObject finalFields = offer.get(STObject.FinalFields);
//				if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
//					oes.add(RLOrder.fromOfferExecuted(offer, false));
//				}
//			}
//		}
		
//		if (!oes.isEmpty()){
//			final StringBuffer sb = new StringBuffer("OFFER EXECUTED");
//			oes.forEach(oe -> {
//				sb.append(GsonUtils.toJson(oe));
//			});
//			log(sb.toString());
//			bus.send(new OnOfferExecuted(oes));
//		}

	}

	private static class FilterAutobridged {
		// pretty sure autobridge happens on OE belonging to others
		public ArrayList<Offer> cache = new ArrayList<>();
		public HashMap<String, ArrayList<Offer>> map = new HashMap<>();
		private boolean flagXRPfirst = false;
		private boolean flagXRPlast = false;
		private boolean flagSizeTwo = false;

		void push(Offer offer) {			
			String pair = RLOrder.buildPair(offer);
			if (pair.startsWith(Currency.XRP.toString())){
				flagXRPfirst = true;
			}else if (pair.endsWith(Currency.XRP.toString())){
				flagXRPlast = true;
			}
			if (map.get(pair) == null) {
				map.put(pair, new ArrayList<>());
			}
			map.get(pair).add(offer);
			flagSizeTwo = map.size() == 2; 
			cache.add(offer);
		}

		List<RLOrder> process() {
			List<RLOrder> res = new ArrayList<>();
			if (!flagSizeTwo || !flagXRPfirst || !flagXRPlast){
				cache.stream().forEach(oe -> {
					res.add(RLOrder.fromOfferExecuted(oe, false));
				});
				return res;
			}
			res.addAll(RLOrder.fromAutobridge(map));
			return res;
		}
	}
	

}
