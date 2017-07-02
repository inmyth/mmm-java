package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	private final static Logger LOGGER = MyLogger.getLogger(Common.class.getName());
	private RxBus bus = RxBusProvider.getInstance();
	private Config config;
	
	private Common(Config config) {
		this.config = config;
		String subscribeRequest = Subscribe.build(Command.SUBSCRIBE).withAccount(config.getCredentials().getAddress()).stringify();

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
			testOfferQuality(raw);
			filterTx(raw);
		}

	}

	private static class FilterOfferExecuted {
		boolean isOwnOEs = false;
		ArrayList<Offer> cache = new ArrayList<>();
		HashMap<String, ArrayList<Offer>> map = new HashMap<>();

		void push(Offer offer, boolean isOwnOE) {
			String pair = RLOrder.buildPair(offer);
			if (map.get(pair) == null) {
				map.put(pair, new ArrayList<>());
			}
			map.get(pair).add(offer);
			cache.add(offer);
			this.isOwnOEs = isOwnOE;
		}

		List<RLOrder> process() {
			List<RLOrder> res = new ArrayList<>();
			if (map.size() <= 1) {
				cache.stream().forEach(oe -> {
					res.add(RLOrder.fromOfferExecuted(oe));
				});
				return res;
			}
			
			if (cache.size() == 2){
				/* A case where autobridge pairs to one majority. So we can't know which is the bridge transaction.
				 * Assuming that all one to one response shows up in order
				 * sell case: XRP/RJP then JPY/XRP (buy JPY sell RJP)
				 * buy case: XRP/JPY then RJP/XRP (buy RJP sell JPY) 
				 * then we can force direction buy with quantity on the second first element. 
				 */
				res.add(RLOrder.fromAutobridge(cache.get(1), cache.get(0)));
				return res;
			}
			
			ArrayList<Offer> majority = null;
			Offer bridge = null;			
			for (String pair : map.keySet()) {
				if (majority == null){
					majority = map.get(pair);
				}else{
					if (majority.size() < map.get(pair).size()){
						bridge = majority.get(0);
						majority = map.get(pair);
					}else{
						bridge = map.get(pair).get(0);
					}
				}   
			}			
			for (Offer oe : majority){
				res.add(RLOrder.fromAutobridge(oe, bridge));
			}
			return res;		
		}
	}

	public void filterTx(String raw) {

		Offer offerCreated = null;
		FilterOfferExecuted foe = new FilterOfferExecuted();

		JSONObject transaction = new JSONObject(raw);
		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));

		ArrayList<AffectedNode> deletedNodes = new ArrayList<>();
		ArrayList<Offer> offersExecuted = new ArrayList<>();

		for (AffectedNode node : meta.affectedNodes()) {
			if (!node.isCreatedNode()) {
				LedgerEntry asPrevious = (LedgerEntry) node.nodeAsPrevious();
				if (node.isDeletedNode()) {
					deletedNodes.add(node);
				}
				if (asPrevious instanceof Offer) {
					offersExecuted.add((Offer) asPrevious);
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
			System.out.println("CANCELED : " + previousTxnId);
			bus.send(new Events.OnResponseOfferCancel(previousTxnId));
			return;
		}

		Collections.sort(offersExecuted, Offer.qualityAscending);
		for (Offer offer : offersExecuted) {
			STObject finalFields = offer.get(STObject.FinalFields);
			if (finalFields != null) {
				foe.push(offer, offer.account().address.equals(this.config.getCredentials().getAddress()));
			}
		}

		if (txType.equals("OfferCreate")) {
			if (offerCreated != null) {
				if (offerCreated.account().address.equals(this.config.getCredentials().getAddress())) {
					if (previousTxnId == null) {
						// if
						// (txn.account().address.equals(config.getCredentials().getAddress()))
						// {
						bus.send(new Events.OnResponseNewOfferCreated(txn.hash(), RLOrder.fromOfferCreated(offerCreated)));
						// }
					} else {
						bus.send(new Events.onResponseOfferEdited(txn.hash(), previousTxnId, RLOrder.fromOfferCreated(offerCreated)));
						System.out.println("EDITED " + previousTxnId + " to " + txn.hash());
					}
				}
			}
			if (!foe.cache.isEmpty()){
				List<RLOrder> oes = foe.process();
				oes.forEach(oe -> {
					System.out.println(GsonUtils.toJson(oe));
				});
				bus.send(new Events.onResponseOfferExecuted(oes));
			}


		} else if (txType.equals("Payment") && txn.account().address.equals(config.getCredentials().getAddress())) {
			// we only care about payment not from ours

			List<RLOrder> oes = foe.process();
			oes.forEach(oe -> {
				System.out.println(GsonUtils.toJson(oe));
			});
			bus.send(new Events.onResponseOfferExecuted(oes));
		}

		/*
		 * This is OE from payment commited by other account. In this example
		 * A4069C4554C8BDB5F7E104EE187FB707FF225D70F7423821F257AF818804784E is order create buy 0.1 RJP / 325 JPY Payment
		 * can have multiple our OEs when it consumes --------------------------------------------------------------- Offer
		 * Executed --------------------------------------------------------------- Get/Pay: RJP/JPY Ask: 32560 Paid:
		 * -3256/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS Got: -0.1/RJP/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
		 * --------------------------------------------------------------- { "Account":
		 * "raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf", "FinalFields": { "TakerPays": { "currency": "JPY", "value": "0", "issuer":
		 * "rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS" }, "TakerGets": { "currency": "RJP", "value": "0", "issuer":
		 * "rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS" } }, "PreviousTxnLgrSeq": 30768597, "OwnerNode": "000000000000000F",
		 * "index": "9E71759EC723D1CC84D308EA631724960850BC5B88A6E67B58B84904875D2769", "PreviousTxnID":
		 * "A4069C4554C8BDB5F7E104EE187FB707FF225D70F7423821F257AF818804784E", "TakerGets": { "currency": "RJP", "value":
		 * "0.1", "issuer": "rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS" }, "Flags": 131072, "Sequence": 4765, "TakerPays": {
		 * "currency": "JPY", "value": "3256", "issuer": "rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS" }, "BookDirectory":
		 * "DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B915093638000", "LedgerEntryType": "Offer", "BookNode":
		 * "0000000000000000" }
		 * 
		 * 
		 */

		// switch (txType) {
		//
		// default:
		// if (offerCreated != null) {
		//
		// System.out.println("NEW HASH " + txn.hash());
		//
		// if (myExecutedOffers.isEmpty()) {
		// System.out.println("EDIT : " + previousTxnId);
		//
		// } else {
		// myExecutedOffers.forEach(myPartial -> {
		// System.out.println("PREVIOUSTXN " + myPartial.previousTxnID());
		// System.out.println(myPartial.prettyJSON());
		// });
		// }
		// }
		// } else {
		// if (!myExecutedOffers.isEmpty()) {
		// myExecutedOffers.forEach(myOC -> { // if other takes our
		// // order
		// // TakePays and Taker Gets should be 0 for fully
		// // consumed
		// System.out.println("MY ORDER IS TAKEN ");
		// // In this case there is no new order created and seq
		// // doesn't change
		// STObject executed = myOC.executed(myOC.get(STObject.FinalFields));
		// System.out.println("Get/Pay: " + myOC.getPayCurrencyPair());
		// System.out.println("Ask: " +
		// myOC.directoryAskQuality().stripTrailingZeros().toPlainString());
		// System.out.println("Paid: " + executed.get(Amount.TakerPays));
		// System.out.println("Got: " + executed.get(Amount.TakerGets));
		// System.out.println("PREVIOUS TX " + myOC.previousTxnID());
		//
		// });
		//
		// } else { // if we take other's order
		//
		// notMyExecutedOffers.forEach(oc -> {
		// // This order is seen from the other guy's perspective
		// // if FinalField's takerPays and TakerGets == 0 then we
		// // fully consume the guy's order
		// System.out.println("MY ORDER TAKES ");
		// STObject executed = oc.executed(oc.get(STObject.FinalFields));
		// System.out.println("Get/Pay: " + oc.getPayCurrencyPair());
		// System.out.println("Ask: " +
		// oc.directoryAskQuality().stripTrailingZeros().toPlainString());
		// System.out.println("Paid: " + executed.get(Amount.TakerPays));
		// System.out.println("Got: " + executed.get(Amount.TakerGets));
		// System.out.println("PREVIOUS TX " + oc.previousTxnID());
		//
		// });
		// }
		// }
		//
		// }

	}

	public static void testOfferQuality(String raw) throws Exception {
		System.out.println("\n");
		System.out.println("\n");
		System.out.println("****NEW TX****");

		JSONObject transaction = new JSONObject(raw);
		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));

		if (txn.get(Field.TransactionType).toString().equals("OfferCrate")) {
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
				// Merge fields from node / node.FinalFields && node.PreviousFields
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
		offerCreate.takerGets(new Amount(new BigDecimal(1.0d), Currency.fromString("JPY"), AccountID.fromString(config.getCredentials().getAddress())));
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
