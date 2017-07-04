package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.Events.WSError;
import com.mbcu.mmm.main.Events.WSGotText;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.utils.GsonUtils;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCreate;

public class Common extends Base {
	public enum TxType {
		CANCEL,
		EDIT, 
		OC,
		OE				
	}
	
	private final static Logger LOGGER = MyLogger.getLogger(Common.class.getName());
	private RxBus bus = RxBusProvider.getInstance();
	private Config config;

	private Common(Config config) {
		this.config = config;
		String subscribeRequest = Subscribe.build(Command.SUBSCRIBE).withAccount(config.getCredentials().getAddress())
				// .withOrderbookFromConfig(config)
				.stringify();

		bus.toObservable()
				// .subscribeOn(Schedulers.newThread())

				.subscribe(o -> {
					if (o instanceof Events.WSConnected) {
						LOGGER.fine("connected");
						LOGGER.fine("Sending subsribe request");
						LOGGER.finer(subscribeRequest);
						bus.send(new Events.WSRequestSendText(subscribeRequest));
					} else if (o instanceof Events.WSDisconnected) {
						LOGGER.fine("disconnected");
					} else if (o instanceof Events.WSError) {
						Events.WSError event = (WSError) o;
						LOGGER.severe(event.e.getMessage());
					} else if (o instanceof Events.WSGotText) {
						Events.WSGotText event = (WSGotText) o;
						LOGGER.finer(event.raw);
						reroute(event.raw);
					}
				});

	}

	public static Common newInstance(Config config) {
		return new Common(config);
	}

	private void reroute(String raw) throws Exception {
		if (raw.contains("OfferCreate") || raw.contains("Payment") || raw.contains("OfferCancel")) {
			if (raw.contains(config.getCredentials().getAddress())) {
				testOfferQuality(raw);
				filterTx(raw);
			}
		}
	}

	

	public void filterTx(String raw) {
		boolean offerCreateFlag;
		boolean offerCreatedFlag;
//		boolean offer
		ArrayList<RLOrder> rawCounters = new ArrayList<>();
		
		Offer offerCreated = null;

		JSONObject transaction = new JSONObject(raw);
		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));

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
			System.out.println("CANCELED : " + txn.previousTxnID());
			bus.send(new Events.OnResponseOfferCancel(txn.previousTxnID()));
			return;
		}

		if (txType.equals("OfferCreate")) {
			if (txn.account().address.equals(this.config.getCredentials().getAddress())){
				if (offerCreated != null) {
					if (previousTxnId == null) {
						bus.send(new Events.OnResponseNewOfferCreated(txn.hash(), RLOrder.fromOfferCreated(offerCreated)));					
					} else {
						System.out.println("EDITED " + previousTxnId + " to " + txn.hash());
						bus.send(new Events.onResponseOfferEdited(txn.hash(), previousTxnId, RLOrder.fromOfferCreated(offerCreated)));
					}				
				}
				FilterAutobridged fa = new FilterAutobridged();

				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null) {
						fa.push(offer);
					}
				}			
				rawCounters.addAll(fa.process());			
			}else{
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
						rawCounters.add(RLOrder.fromOfferExecuted(offer, true));
					}
				}		
			}							
			if (!rawCounters.isEmpty()) {
				rawCounters.forEach(oe -> {
					System.out.println(GsonUtils.toJson(oe));
				});
				bus.send(new Events.onResponseOfferExecuted(rawCounters));
			}
		} else if (txType.equals("Payment") && !txn.account().address.equals(config.getCredentials().getAddress())) {
			// we only care about payment not from ours. 
			for (Offer offer : offersExecuteds) {
				STObject finalFields = offer.get(STObject.FinalFields);
				if (finalFields != null && offer.account().address.equals(this.config.getCredentials().getAddress())) {
					rawCounters.add(RLOrder.fromOfferExecuted(offer, false));
				}
			}				
			rawCounters.forEach(oe -> {
				System.out.println(GsonUtils.toJson(oe));
			});
			bus.send(new Events.onResponseOfferExecuted(rawCounters));
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

			ArrayList<Offer> majorities = null;
			ArrayList<Offer> minorities = null;

			for (ArrayList<Offer> offers : map.values()){
				if (majorities == null && minorities == null){
					majorities = offers;
					minorities = offers;
				}else{
					if (offers.size() > majorities.size()){
						majorities = offers;
					}else{
						minorities = offers;
					}
				}
			}

			res.addAll(RLOrder.fromAutobridge(majorities, minorities));
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

		Collections.sort(offersExecuted, Offer.qualityAscending);
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

	public static String sign(int seq, Config config) {

		OfferCreate offerCreate = new OfferCreate();
		offerCreate.takerGets(new Amount(new BigDecimal(1.0d), Currency.fromString("JPY"),
				AccountID.fromString(config.getCredentials().getAddress())));
		offerCreate.takerPays(new Amount(new BigDecimal(1.0d)));
		offerCreate.sequence(new UInt32(new BigInteger(String.valueOf(seq))));
		offerCreate.fee(new Amount(new BigDecimal(12)));
		offerCreate.account(AccountID.fromAddress(config.getCredentials().getAddress()));

		SignedTransaction signed = offerCreate.sign(config.getCredentials().getSecret());
		System.out.println(offerCreate.prettyJSON());

		System.out.println(signed.hash);
		System.out.println(signed.tx_blob);

		return Submit.build(signed.tx_blob).stringify();

	}
}
