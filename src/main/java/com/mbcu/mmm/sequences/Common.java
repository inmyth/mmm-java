package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mbcu.mmm.helpers.TAccountOffer;
import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.main.WebSocketClient.WSGotText;
import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.Cpair;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.UInt16Field;
import com.ripple.core.fields.UInt32Field;
import com.ripple.core.serialized.enums.EngineResult;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;
import com.ripple.core.types.known.tx.txns.OfferCancel;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Common extends Base {

	private Common(Config config) {
		super(MyLogger.getLogger(Common.class.getName()), config);
		this.config = config;

		bus.toObservable().subscribeOn(Schedulers.newThread()).subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onNext(Object o) {
				BusBase base = (BusBase) o;
				try {
					if (base instanceof WebSocketClient.WSGotText) {
						WebSocketClient.WSGotText event = (WSGotText) o;
						reroute(event.raw);
					}
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);
					throw e;
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
		} else if (raw.contains("ledgerClosed")) {
			filterLedgerClosed(raw);
		}
	}

	private void filterLedgerClosed(String raw) {
		JSONObject whole = new JSONObject(raw);
		bus.send(new OnLedgerClosed(whole));
	}

	private void filterResponse(String raw) {
		JSONObject whole = new JSONObject(raw);
		JSONObject result = whole.optJSONObject("result");

		if (whole.getString("status").equals("error")) {
			StringBuilder er = new StringBuilder("ERROR :\n");
			er.append(raw);
			log(er.toString(), Level.SEVERE);
			return;
		}

		if (result.has("offers") && result.has("account")) {
			JSONArray offers = result.getJSONArray("offers");
			List<TAccountOffer> tAccOffs = new ArrayList<>();
			for (int i = 0; i < offers.length(); i++) {
				TAccountOffer offer = TAccountOffer.of((JSONObject) offers.get(i));
				tAccOffs.add(offer);
			}
			String marker = result.has("marker") ? result.getString("marker") : null;
			bus.send(new OnAccountOffers(tAccOffs, marker));
			return;
		}

		if (result.has("asks")) {
			System.out.println("Orderbook subscription");
			// System.out.println(result.getJSONArray("offers").toString());
			// bus.send(new OnOrderbook());
			return;
		}

		if (result.has("tx_json")) {
			if ((raw.contains("OfferCreate") || raw.contains("OfferCancel"))) {
				Hash256 hash = Hash256.fromHex(result.optJSONObject("tx_json").optString("hash"));
				AccountID accId = AccountID.fromAddress(result.optJSONObject("tx_json").optString("Account"));
				String engResult = result.optString("engine_result");
				UInt32 sequence = new UInt32(result.getJSONObject("tx_json").getInt("Sequence"));
				log(engResult + " " + accId + " " + hash + " " + sequence);
				if (engResult.equals(EngineResult.tesSUCCESS.toString())) {
					bus.send(new OnRPCTesSuccess(accId, hash, sequence));
				} else {
					bus.send(new OnRPCTesFail(engResult, accId, hash, sequence));
				}
			}
		} else if (result.has("account_data")) {
			UInt32 sequence = new UInt32(result.getJSONObject("account_data").getInt("Sequence"));
			bus.send(new OnAccountInfoSequence(sequence));
		} else if (result.has("validated_ledgers")) {
			bus.send(new OnLedgerClosed(result));
		}
	}

	public void filterStream2(String raw) {
		if (!raw.contains(config.getCredentials().getAddress())) {
			log("Not related to our order : " + raw, Level.WARNING);
			return;
		}

		log("Raw Stream :\n" + raw, Level.FINEST);
		if (!(raw.contains("OfferCreate") || raw.contains("Payment") || raw.contains("OfferCancel"))) {
			log("Stream not OfferCreate | Payment | OfferCancel : " + raw, Level.WARNING);
			return;
		}

		ArrayList<RLOrder>  oes = new ArrayList<>(); // Offer Executeds
		ArrayList<BefAf> 		ors = new ArrayList<>(); // Before and After
		Offer offerCreated 			= null;
		JSONObject transaction = new JSONObject(raw);

		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);

		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));
		AccountID txnAccId = txn.account();
		Hash256 txnHash = txn.hash();
		UInt32 txnSequence = txn.sequence();

		if (meta.engineResult().compareTo(EngineResult.tesSUCCESS) != 0) {
			log("Stream NOT TesSuccess : " + meta.engineResult().toString() + " " + txnHash + " " + txnSequence);
			bus.send(new OnRPCTesFail(meta.engineResult().toString(), txnAccId, txnHash, txnSequence));
			return;
		}

//		ArrayList<AffectedNode> deletedNodes 			= new ArrayList<>();
		ArrayList<Offer> offersExecuteds		 			= new ArrayList<>();
		ArrayList<OfferEdited> ofEditeds 					= new ArrayList<>();
		
		String txType = txn.get(Field.TransactionType).toString();

		boolean offerCancelFlag = false;
		if (txType.equals("OfferCancel") && txn.account().address.equals(this.config.getCredentials().getAddress())) {						
			offerCancelFlag = true;
		}
		
		UInt32 txnOfferSequence = txn.get(UInt32.OfferSequence); // Offer Edit flag, indicates old order's sequence
		
		for (AffectedNode node : meta.affectedNodes()) {
			if (!node.isCreatedNode()) {
				LedgerEntry asPrevious = (LedgerEntry) node.nodeAsPrevious();

				if (node.isDeletedNode()) {
					if (asPrevious instanceof Offer) {
						AccountID nodeAccount  = asPrevious.get(AccountID.Account);
						Hash256 previousTxnId  = asPrevious.get(Hash256.PreviousTxnID);
						UInt32 prevSeq	 			 = asPrevious.get(UInt32.Sequence);
						Offer o 							 = (Offer) asPrevious;
						//FinalFields.Flag can be 0 or  131072, this is unclear
						if (nodeAccount != null && nodeAccount.address.equals(this.config.getCredentials().getAddress()) && asPrevious.get(Field.LedgerEntryType) == LedgerEntryType.Offer) {
					  	if (offerCancelFlag) {
								log("OFFER CANCEL " + txn.sequence() + " " + txnHash + " canceling : " + previousTxnId + " " + txn.get(UInt32.OfferSequence));
								bus.send(new OnOfferCanceled(txn.account(), prevSeq, txn.sequence(), previousTxnId, LogNum.CANCELED_OFFERCANCEL));
								return;
					  	}						
							if (node.nested.get(Field.PreviousFields) != null) {
								offersExecuteds.add(o);
							}
							else {
					  		if (txnOfferSequence != null) {
							  	ofEditeds.add(new OfferEdited(txnHash, previousTxnId, prevSeq, txnSequence));
					  		}
					  		else {
									bus.send(new OnOfferCanceled(txn.account(), prevSeq, txn.sequence(), previousTxnId, LogNum.CANCELED_UNFUNDED));									
					  		}
							}
						}
					} 
//					else {
//						LedgerEntry le				 = (LedgerEntry) node.nodeAsFinal();
//						AccountID nodeAccount  = le.get(AccountID.Account);
//						Hash256 previousTxnId  = le.get(Hash256.PreviousTxnID);
//						UInt32 prevSeq	 			 = le.get(UInt32.Sequence);
//						if (nodeAccount != null && nodeAccount.address.equals(this.config.getCredentials().getAddress())	&& le.get(Field.LedgerEntryType) == LedgerEntryType.Offer && le.get(Field.Flags).toString().equals("0")) {
//
//					  
//						} 
//					}
				}			
				else if (node.isModifiedNode()) {
					// unused
				}
			} 
			else { // createdNode goes here
				LedgerEntry asFinal = (LedgerEntry) node.nodeAsPrevious();
				if (asFinal instanceof Offer) {
					Offer offer = (Offer) asFinal;
					offerCreated = offer;
				}
			}
		}

	
		RLOrder ourOfferCreate = null;
		if (txType.equals("OfferCreate")) {
			RLOrder offerCreate = RLOrder.fromOfferCreate(txn);
			log("OFFER CREATE Account: " + txnAccId + " Hash " + txnHash + " Sequence " + txnSequence + "\n" + offerCreate.stringify());
			// OnOfferCreate event is only needed to increment sequence.
			if (txn.account().address.equals(this.config.getCredentials().getAddress())) {
				ourOfferCreate = offerCreate;
				bus.send(new OnOfferCreate(txnAccId, txnHash, txnSequence));
			}

			if (offerCreated != null) {
				AccountID ocAccId = offerCreated.account();
				if (ofEditeds.isEmpty()) {
					if (ocAccId.address.equals(this.config.getCredentials().getAddress())) {
						log("OFFER CREATED TxnID " + txnAccId + " " + txnSequence + "\n"+ offerCreated.prettyJSON());
						bus.send(new OnOfferCreated(txnAccId, ocAccId, offerCreated.previousTxnID(), RLOrder.fromOfferCreated(offerCreated)));
					}
				} 
				else {
//					if (ocAccId.address.equals(this.config.getCredentials().getAddress())) {
					for (OfferEdited o : ofEditeds) {
						log("OFFER EDITED " + o.previousTxnId + " to " + txn.hash());
						BefAf ba = RLOrder.toBA(offerCreated.takerPays(), offerCreated.takerGets(), txn.get(Amount.TakerPays), txn.get(Amount.TakerGets), o.prevSeq, txnHash, ourOfferCreate);
						bus.send(new OnOfferEdited(ocAccId, txnHash, o.previousTxnId, o.prevSeq, txn.sequence(), ba));
					}
//					}
				}
			}
			if (txn.account().address.equals(this.config.getCredentials().getAddress())) {
				FilterAutobridged fa = new FilterAutobridged();
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null && isTakersExist(finalFields)) {
						fa.push(offer);
					}
				}
				oes.addAll(fa.process());
				Amount takerPays = offerCreated != null ? offerCreated.takerPays() : null;
				Amount takerGets = offerCreated != null ? offerCreated.takerGets() : null;
				ors.add(RLOrder.toBA(txn.get(Amount.TakerPays), txn.get(Amount.TakerGets), takerPays, takerGets, txnSequence, txnHash, ourOfferCreate));
			} 
			else {
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					UInt32 affectedSeq = offer.get(UInt32.Sequence);
					if (finalFields != null && isTakersExist(finalFields)) {
						oes.add(RLOrder.fromOfferExecuted(offer, true));
						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), finalFields.get(Amount.TakerPays), finalFields.get(Amount.TakerGets), affectedSeq, txnHash, ourOfferCreate));
					}
					if (finalFields == null) {
						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), null, null, offer.sequence(), txnHash, ourOfferCreate));
					}
				}
			}
		} 
		else if (txType.equals("Payment") && !txn.account().address.equals(config.getCredentials().getAddress())) {
			// we only care about payment not from ours.
			for (Offer offer : offersExecuteds) {
				STObject finalFields = offer.get(STObject.FinalFields);
				UInt32 affectedSeq = offer.get(UInt32.Sequence);

				if (finalFields != null && isTakersExist(finalFields)) {
					oes.add(RLOrder.fromOfferExecuted(offer, true));
					ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), finalFields.get(Amount.TakerPays), finalFields.get(Amount.TakerGets), affectedSeq, txnHash, ourOfferCreate));
				}
				if (finalFields == null) {
					ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), null, null, affectedSeq, txnHash, ourOfferCreate));
				}
			}
		}

		if (!oes.isEmpty()) {
			final StringBuffer sb = new StringBuffer("OFFER EXECUTED\n");
			oes.forEach(oe -> {
				sb.append(oe.stringify());
			});
			log(sb.toString());
			bus.send(new OnOfferExecuted(oes));
		}

//		if (!oes.isEmpty() && !ors.isEmpty()) {
//			final StringBuffer sb = new StringBuffer("OFFER EXECUTED\n");
//			ors.forEach(or -> {
//				if (or.after.getQuantity().value().compareTo(BigDecimal.ZERO) == 0) {
//					sb.append("Fully Taken\n");
//					sb.append(or.before.stringify());
//				} else {
//					sb.append("Partially Taken\n");
//					sb.append(or.after.stringify());
//				}
//				sb.append("seq :  ");
//				sb.append(or.befSeq.intValue());
//			});
//			log(sb.toString());
//			bus.send(new OnOfferExecuted(oes));
//		}

		bus.send(new OnDifference(ors));
	}

	private static class FilterAutobridged {
		// pretty sure autobridge happens on OE belonging to others
		ArrayList<Offer> cache = new ArrayList<>();
		HashMap<String, ArrayList<Offer>> map = new HashMap<>();

		void push(Offer offer) {
			Cpair cpair = Cpair.newInstance(offer);
			if (map.get(cpair.toString()) == null) {
				map.put(cpair.toString(), new ArrayList<>());
			}
			map.get(cpair.toString()).add(offer);
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

	public static boolean isTakersExist(STObject finalFields) {
		Amount testTakerPays = finalFields.get(Amount.TakerPays);
		Amount testTakerGets = finalFields.get(Amount.TakerGets);
		return testTakerGets != null && testTakerPays != null;
	}

	public static class OnOfferCanceled extends BusBase {
		public final AccountID account;
		public final Hash256 previousTxnId;
		public final UInt32 prevSeq;
		public final UInt32 newSeq;
		public final LogNum lognum;

		public OnOfferCanceled(AccountID account, UInt32 prevSeq, UInt32 newSeq, Hash256 previousTxnId, LogNum lognum) {
			super();
			this.account = account;
			this.previousTxnId = previousTxnId;
			this.prevSeq = prevSeq;
			this.newSeq = newSeq;
			this.lognum = lognum;
		}

	}

	public static class OnOfferCreate extends BusBase {
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

	public static class OnOfferCreated extends BusBase {
		public final AccountID txnAccount, ocAccount;
		public final Hash256 prevTxnId;
		public final RLOrder order;

		public OnOfferCreated(AccountID txnAccount, AccountID ocAccount, Hash256 prevTxnId, RLOrder order) {
			super();
			this.txnAccount = txnAccount;
			this.ocAccount = ocAccount;
			this.prevTxnId = prevTxnId;
			this.order = order;
		}
	}

	public static class OnOfferEdited extends OfferEdited {
		public final AccountID ocAccount;
		public final BefAf ba;

		public OnOfferEdited(AccountID ocAccount, Hash256 newHash, Hash256 previousTxnId, UInt32 prevSeq, UInt32 newSeq, BefAf ba) {
			super(newHash, previousTxnId, prevSeq, newSeq);
			this.ocAccount = ocAccount;
			this.ba = ba;
		}
	}

	public static class OfferEdited extends BusBase {
		public final Hash256 newHash;
		public final Hash256 previousTxnId;
		public final UInt32 newSeq;
		public final UInt32 prevSeq;

		public OfferEdited(Hash256 newHash, Hash256 previousTxnId, UInt32 prevSeq, UInt32 newSeq) {
			super();
			this.newHash = newHash;
			this.previousTxnId = previousTxnId;
			this.newSeq = newSeq;
			this.prevSeq = prevSeq;
		}
	}
	
	public static class OnOfferExecuted extends BusBase {
		public List<RLOrder> oes;

		public OnOfferExecuted(List<RLOrder> oes) {
			super();
			this.oes = oes;
		}
	}

	public static class OnDifference extends BusBase {
		public List<BefAf> bas;

		public OnDifference(List<BefAf> bas) {
			super();
			this.bas = bas;
		}
	}

	public static class OnRPCTesSuccess extends BusBase {
		public AccountID accountID;
		public Hash256 hash;
		public UInt32 sequence;

		public OnRPCTesSuccess(AccountID accountID, Hash256 hash, UInt32 sequence) {
			super();
			this.accountID = accountID;
			this.hash = hash;
			this.sequence = sequence;
		}
	}

	public static class OnRPCTesFail extends BusBase {
		public String engineResult;
		public AccountID accountID;
		public Hash256 hash;
		public UInt32 sequence;

		public OnRPCTesFail(String engineResult, AccountID accountID, Hash256 hash, UInt32 sequence) {
			super();
			this.engineResult = engineResult;
			this.accountID = accountID;
			this.hash = hash;
			this.sequence = sequence;
		}
	}

	public static class OnLedgerClosed extends BusBase {
		public final LedgerEvent ledgerEvent;

		public OnLedgerClosed(JSONObject root) {
			super();
			this.ledgerEvent = LedgerEvent.fromJSON(root);
		}
	}

	public static class OnAccountInfoSequence extends BusBase {
		public final UInt32 sequence;

		public OnAccountInfoSequence(UInt32 sequence) {
			super();
			this.sequence = sequence;
		}
	}

	public static class OnAccountOffers extends BusBase {
		public final List<TAccountOffer> accOffs;
		public final String marker;

		public OnAccountOffers(List<TAccountOffer> accOffs, String marker) {
			super();
			this.accOffs = accOffs;
			this.marker = marker;
		}
	}

}