package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONObject;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.main.WebSocketClient.WSError;
import com.mbcu.mmm.main.WebSocketClient.WSGotText;
import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.models.request.Subscribe.Stream;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.serialized.enums.EngineResult;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Common extends Base {	
	private Common(Config config) {
		super(MyLogger.getLogger(Common.class.getName()), config);
		this.config = config;
		String subscribeRequest = Subscribe
			.build(Command.SUBSCRIBE)
//				.withOrderbookFromConfig(config)
			.withStream(Stream.LEDGER)
			.withAccount(config.getCredentials().getAddress())
			.stringify();

		bus.toObservable()
			.subscribeOn(Schedulers.newThread())			
			.subscribe(new Observer<Object>() {

				@Override
				public void onSubscribe(Disposable d) {
					// TODO Auto-generated method stub						
				}

				@Override
				public void onNext(Object o) {
					if (o instanceof WebSocketClient.WSConnected) {
						log("connected", Level.FINER);
						log("Sending subsribe request");
						log(subscribeRequest);
						bus.send(new WebSocketClient.WSRequestSendText(subscribeRequest));
					} else if (o instanceof WebSocketClient.WSDisconnected) {
						log("disconnected");
					} else if (o instanceof WebSocketClient.WSError) {
						WebSocketClient.WSError event = (WSError) o;
						log(event.e.getMessage(), Level.SEVERE);
					} else if (o instanceof WebSocketClient.WSGotText) {						
						WebSocketClient.WSGotText event = (WSGotText) o;
//						log(event.raw, Level.FINER);													
						reroute(event.raw);
					}						
				}

				@Override
				public void onError(Throwable e) {
					log(e.getMessage(), Level.SEVERE);					
				}

				@Override
				public void onComplete() {						
				}
			});
	}

	public static Common newInstance(Config config) {
		return new Common(config);
	}

	private void reroute(String raw) {
		
		if (raw.contains("response")) {
			filterResponse(raw);
		} else if (raw.contains("transaction")) {
			filterStream2(raw);
		} else if (raw.contains("ledgerClosed")){
			filterLedgerClosed(raw);
		}
	}
		
	private void filterLedgerClosed(String raw){
		JSONObject whole = new JSONObject(raw);	
		bus.send(new OnLedgerClosed(whole));		
	}

	private void filterResponse(String raw){
		JSONObject whole = new JSONObject(raw);	
		JSONObject result = whole.optJSONObject("result");

		if (whole.getString("status").equals("error")){ // this is error
			StringBuilder er = new StringBuilder("ERROR :\n");
			er.append(raw);
			log(er.toString(), Level.SEVERE);
			return;
		}
		
		if (result.has("tx_json")){
			if ((raw.contains("OfferCreate") || raw.contains("OfferCancel"))){
				Hash256 hash = Hash256.fromHex(result.optJSONObject("tx_json").optString("hash"));
				AccountID accId = AccountID.fromAddress(result.optJSONObject("tx_json").optString("Account"));
				String engResult = result.optString("engine_result");					
				UInt32 sequence = new UInt32(result.getJSONObject("tx_json").getInt("Sequence"));
				log(engResult + " " +  accId + " " + hash + " " + sequence);
				if (engResult.equals(EngineResult.tesSUCCESS.toString())){
					bus.send(new OnResponseTesSuccess(accId, hash, sequence));
				}else{				
					bus.send(new OnResponseFail(engResult, accId, hash, sequence));		}	
			}		
		}else if (result.has("account_data")){
			UInt32 sequence = new UInt32(result.getJSONObject("account_data").getInt("Sequence"));
			bus.send(new OnAccountInfoSequence(sequence));	
		}else if (result.has("validated_ledgers")){
			bus.send(new OnLedgerClosed(result));		
		}

	}
	
	public void filterStream2(String raw) {
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
		ArrayList<BefAf> ors = new ArrayList<>();
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
			log("OFFER CREATE Account: " + txnAccId + " Hash " + txnHash + " Sequence " + txnSequence + "\n" + offerCreate.stringify());
			// OnOfferCreate event is only needed to increment sequence.
			bus.send(new OnOfferCreate(txnAccId, txnHash, txnSequence));

			if (offerCreated != null) {
				AccountID ocAccId = offerCreated.account();		
				if (previousTxnId == null) {
					log("OFFER CREATED OCID " + ocAccId + " TxnID " + txnAccId + " OCPrevTxnId " + previousTxnId + " \n" + offerCreated.prettyJSON() );
					bus.send(new OnOfferCreated(txnAccId, ocAccId, offerCreated.previousTxnID(), RLOrder.fromOfferCreated(offerCreated)));				
				}else{
					log("EDITED " + previousTxnId + " to " + txn.hash() + " \n" + offerCreated.prettyJSON());
					bus.send(new OnOfferEdited(ocAccId, txnHash, previousTxnId, RLOrder.fromOfferCreated(offerCreated)));
				}
			}		
			if (txn.account().address.equals(this.config.getCredentials().getAddress())) {
				FilterAutobridged fa = new FilterAutobridged();
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null) {
						fa.push(offer);
					}
				}
				oes.addAll(fa.process());
				if (offerCreated == null){
					ors.add(RLOrder.toBA(txn.get(Amount.TakerPays), txn.get(Amount.TakerGets), null, null));
				}else{
					ors.add(RLOrder.toBA(txn.get(Amount.TakerPays), txn.get(Amount.TakerGets), offerCreated.takerPays(), offerCreated.takerGets()));
				}
			}else{
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
						oes.add(RLOrder.fromOfferExecuted(offer, true));						
						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), finalFields.get(Amount.TakerPays), finalFields.get(Amount.TakerGets)));						
					}
					if (finalFields == null && offer.account().address.equals(this.config.getCredentials().getAddress())){
						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), null, null));
					}
				}
			}
		}			
		else if (txType.equals("Payment") && !txn.account().address.equals(config.getCredentials().getAddress())) {
			// we only care about payment not from ours.
			for (Offer offer : offersExecuteds) {
				STObject finalFields = offer.get(STObject.FinalFields);
				if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
					oes.add(RLOrder.fromOfferExecuted(offer, true));
					if (offer.get(STObject.FinalFields).get(Amount.TakerGets).value().compareTo(BigDecimal.ZERO) == 0){
						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), finalFields.get(Amount.TakerPays), finalFields.get(Amount.TakerGets)));
					}
				}
				if (finalFields == null && offer.account().address.equals(this.config.getCredentials().getAddress())){
					ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), null, null));
				}
			}
		}
		
		if (!oes.isEmpty()){
			final StringBuffer sb = new StringBuffer("OFFER EXECUTED\n");
			oes.forEach(oe -> {
				sb.append(oe.stringify());
			});
			log(sb.toString());
			bus.send(new OnOfferExecuted(oes));
		}
		if (!ors.isEmpty()){
			bus.send(new OnRemainder(ors));
		}
		
	}

	private static class FilterAutobridged {
		// pretty sure autobridge happens on OE belonging to others
		ArrayList<Offer> cache = new ArrayList<>();
		HashMap<String, ArrayList<Offer>> map = new HashMap<>();

		void push(Offer offer) {
			String pair = RLOrder.buildPair(offer);
			if (map.get(pair) == null) {
				map.put(pair, new ArrayList<>());
			}
			map.get(pair).add(offer);
			cache.add(offer);
		}

		List<RLOrder> process() {
			List<RLOrder> res = new ArrayList<>();
			if (map.size() <= 1) { // No Autobridge
				cache.stream().forEach(oe -> {
					res.add(RLOrder.fromOfferExecuted(oe, false));
				});
				return res;
			}
			res.addAll(RLOrder.fromAutobridge(map));
			return res;
		}
	}

	public static void testOfferQuality(String raw) throws Exception {
		System.out.println("\n");
		System.out.println("\n");
		System.out.println("****NEW TX****");

		JSONObject transaction = new JSONObject(raw);
		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));

		if (txn.get(Field.TransactionType).toString().equals("OfferCreate")) {
			Amount gets = txn.get(Amount.TakerGets);
			Amount pays = txn.get(Amount.TakerPays);

			System.out.println("---------------------------------------------------------------");
			System.out.println("OfferCreate ");
			System.out.println("---------------------------------------------------------------");
			System.out.println("Get/Pay:    " + gets.currencyString() + "/" + pays.currencyString());
			System.out.println("Bid:        " + gets.computeQuality(pays));
			System.out.println("TakerPays:  " + pays);
			System.out.println("TakerGets:  " + gets);
		} else if (txn.get(Field.TransactionType).toString().equals("OfferCancel")) {
			System.out.println("---------------------------------------------------------------");
			System.out.println("OfferCancel ");
			System.out.println("---------------------------------------------------------------");
		} else if (txn.get(Field.TransactionType).toString().equals("Payment")) {
			System.out.println("---------------------------------------------------------------");
			System.out.println("Payment ");
			System.out.println("---------------------------------------------------------------");

		} else {
			System.out.println("---------------------------------------------------------------");
			System.out.println(txn.get(Field.TransactionType).toString());
			System.out.println("---------------------------------------------------------------");
		}

		System.out.println(txn.prettyJSON());
		ArrayList<AffectedNode> deletedNodes = new ArrayList<>();
		ArrayList<Offer> offersExecuted = new ArrayList<>();

		for (AffectedNode node : meta.affectedNodes()) {

			if (!node.isCreatedNode()) {
				// Merge fields from node / node.FinalFields &&
				// node.PreviousFields
				// to determine state of node prior to transaction.
				// Any fields that were in PreviousFields will have their final
				// values
				// in a nested STObject keyed by FinalFields.
				LedgerEntry asPrevious = (LedgerEntry) node.nodeAsPrevious();
				// If it's an offer
				if (node.isDeletedNode()) {
					deletedNodes.add(node);
				}
				if (asPrevious instanceof Offer) {
					// We can down-cast this to use Offer specific methods
					offersExecuted.add((Offer) asPrevious);
				}
			} else {
				LedgerEntry asFinal = (LedgerEntry) node.nodeAsPrevious();
				if (asFinal instanceof Offer) {
					Offer offer = (Offer) asFinal;
					System.out.println("---------------------------------------------------------------");
					System.out.println("Offer Created");
					System.out.println("---------------------------------------------------------------");
					System.out.println("Final fields taker_pays " + offer.get(Amount.TakerPays));
					System.out.println("Final fields taker_gets " + offer.get(Amount.TakerGets));
					System.out.println("---------------------------------------------------------------");
					System.out.println("Get/Pay:    " + offer.getPayCurrencyPair());
					System.out.println("Bid:        " + offer.bidQuality());
					System.out.println("TakerPays:  " + offer.takerPays());
					System.out.println("TakerGets:  " + offer.takerGets());
					System.out.println("---------------------------------------------------------------");
					System.out.println(offer.prettyJSON());
				}
			}
		}

		deletedNodes.forEach(dn -> {
			LedgerEntry le = (LedgerEntry) dn.nodeAsFinal();
			STObject finalFields = dn.get(STObject.FinalFields);

			Hash256 previousTxnId = le.get(Hash256.PreviousTxnID);
			if (previousTxnId != null) {
				System.out.println(previousTxnId.toString());

			}
		});

		// Collections.sort(offersExecuted, Offer.qualityAscending);
		for (Offer offer : offersExecuted) {
			STObject finalFields = offer.get(STObject.FinalFields);
			if (finalFields == null) {
				System.out.println("FinalFields is null");
				// System.out.println(offer.prettyJSON());

			} else {

				STObject executed = offer.executed(offer.get(STObject.FinalFields));
				// This will be computed from the BookDirectory field
				System.out.println("---------------------------------------------------------------");
				System.out.println("Offer Executed");
				System.out.println("---------------------------------------------------------------");
				System.out.println("Final fields taker_pays " + finalFields.get(Amount.TakerPays));
				System.out.println("Final fields taker_gets " + finalFields.get(Amount.TakerGets));
				System.out.println("---------------------------------------------------------------");

				System.out.println("Get/Pay: " + offer.getPayCurrencyPair());
				System.out.println("Ask:     " + offer.directoryAskQuality().stripTrailingZeros().toPlainString());
				System.out.println("Paid:    " + executed.get(Amount.TakerPays));
				System.out.println("Got:     " + executed.get(Amount.TakerGets));
				System.out.println("---------------------------------------------------------------");
				System.out.println(offer.prettyJSON());
			}

		}
		System.out.println("****END TX****");
		System.out.println("\n");
		System.out.println("\n");

	}

	public static class OnOfferCanceled {
		public AccountID account;
		public Hash256 previousTxnId;
		public UInt32 sequence;

		public OnOfferCanceled(AccountID account, UInt32 sequence, Hash256 previousTxnId) {
			super();
			this.account = account;
			this.previousTxnId = previousTxnId;
			this.sequence = sequence;
		}
	}

	public static class OnOfferCreate {
		public AccountID account;
		public Hash256 hash;
		public UInt32 sequence;

		public OnOfferCreate(AccountID account, Hash256 hash, UInt32 sequence) {
			super();
			this.account = account;
			this.hash = hash;
			this.sequence = sequence;
		}
	}
	
	public static class OnOfferCreated {
		public final  AccountID txnAccount, ocAccount;
		public final  Hash256 prevTxnId;
		public final  RLOrder order;
		
		public OnOfferCreated(AccountID txnAccount, AccountID ocAccount, Hash256 prevTxnId, RLOrder order) {
			super();
			this.txnAccount = txnAccount;
			this.ocAccount = ocAccount;
			this.prevTxnId = prevTxnId;
			this.order = order;
		}
	}
	
	public static class OnOfferEdited {
		public final AccountID ocAccount;
		public final Hash256 newHash;
		public final Hash256 previousTxnId;
		public final RLOrder newOrder;
		
		public OnOfferEdited(AccountID ocAccount, Hash256 newHash, Hash256 previousTxnId, RLOrder newOrder) {
			super();
			this.ocAccount = ocAccount;
			this.newHash = newHash;
			this.previousTxnId = previousTxnId;
			this.newOrder = newOrder;
		}		
	}
	
	public static class OnOfferExecuted{
		public List<RLOrder> oes;

		public OnOfferExecuted(List<RLOrder> oes) {
			super();
			this.oes = oes;
		}
	}
	
	public static class OnRemainder {
		public List<BefAf> bas;

		public OnRemainder(List<BefAf> bas) {
			super();
			this.bas = bas;
		}	
	}
	
	public static class OnResponseTesSuccess{
		public AccountID accountID;
		public Hash256 hash;
		public UInt32 sequence;
		
		public OnResponseTesSuccess(AccountID accountID, Hash256 hash, UInt32 sequence) {
			super();
			this.accountID = accountID;
			this.hash = hash;
			this.sequence = sequence;
		}
	}
	
	public static class OnResponseFail{
		public String engineResult;
		public AccountID accountID;
		public Hash256 hash;
		public UInt32 sequence;
		public OnResponseFail(String engineResult, AccountID accountID, Hash256 hash, UInt32 sequence) {
			super();
			this.engineResult = engineResult;
			this.accountID = accountID;
			this.hash = hash;
			this.sequence = sequence;
		}		
	}
	
	public static class OnLedgerClosed{
		public final LedgerEvent ledgerEvent;
		
		public OnLedgerClosed(JSONObject root) {
			super();
			this.ledgerEvent = LedgerEvent.fromJSON(root);
		}
	}
	
	public static class OnAccountInfoSequence {
		public final UInt32 sequence;

		public OnAccountInfoSequence(UInt32 sequence) {
			super();
			this.sequence = sequence;
		} 			
	}
	
}