package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.Events.WSError;
import com.mbcu.mmm.main.Events.WSGotText;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
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

import io.reactivex.schedulers.Schedulers;

public class Common extends Base {
	private final static Logger LOGGER = MyLogger.getLogger(Common.class.getName());
	private RxBus bus = RxBusProvider.getInstance();
	private Config config;

	public Common(Config config) {
		this.config = config;
		String subscribeRequest = Subscribe.build(Command.SUBSCRIBE).withAccount(config.getCredentials().getAddress())
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

	private void reroute(String raw) {
		if (raw.contains("OfferCreate") || raw.contains("Payment") || raw.contains("OfferCancel")) {
			filterTx(raw);
		}

	}

	private void filterTx(String raw) {

		Offer offerCreated;
		ArrayList<Offer> notMyExecutedOffers = new ArrayList<>();
		ArrayList<Offer> myExecutedOffers = new ArrayList<>();

		JSONObject transaction = new JSONObject(raw);
		JSONObject metaJSON = (JSONObject) transaction.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));

		switch (txType) {

		case "OfferCancel":

			break;

		case "Payment":

			break;

		case "OfferCreate":
			Amount gets = txn.get(Amount.TakerGets);
			Amount pays = txn.get(Amount.TakerPays);
			System.out.println("---------------------------------------------------------------");
			System.out.println("OfferCreate ");
			System.out.println("---------------------------------------------------------------");
			System.out.println("Get/Pay:    " + gets.currencyString() + "/" + pays.currencyString());
			System.out.println("Bid:        " + gets.computeQuality(pays));
			System.out.println("TakerPays:  " + pays);
			System.out.println("TakerGets:  " + gets);
			break;

		default:
			break;
		}

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
			// bus.send(new Events.OnResponseOrderCancel(previousTxnId));
			return;
		}

		Collections.sort(offersExecuted, Offer.qualityAscending);
		for (Offer offer : offersExecuted) {
			STObject finalFields = offer.get(STObject.FinalFields);
			if (finalFields != null) {
				if (offer.account().address.equals(this.config.getCredentials().getAddress())) {
					myExecutedOffers.add(offer);
				} else {
					notMyExecutedOffers.add(offer);
				}
			}
		}

		if (offerCreated != null) {
			if (offerCreated.account().address.equals(this.config.getCredentials().getAddress())) {
				offerCreated.prettyJSON();
				if (previousTxnId != null) {
					if (txn.account().address.equals(config.getCredentials().getAddress())) {
						System.out.println("NEW HASH " + txn.hash());
						bus.send(new Events.onResponseOrderEdited());
					}					
				}
				
			}
		}

		
		switch (txType) {

		default:
			if (!myOfferCreateds.isEmpty()) {

				System.out.println("NEW HASH " + txn.hash());
				
					if (myExecutedOffers.isEmpty()) {
						System.out.println("EDIT : " + previousTxnId);

					} else {
						myExecutedOffers.forEach(myPartial -> {
							System.out.println("PREVIOUSTXN " + myPartial.previousTxnID());
							System.out.println(myPartial.prettyJSON());
						});
					}
				}
			} else {
				if (!myExecutedOffers.isEmpty()) {
					myExecutedOffers.forEach(myOC -> { // if other takes our
														// order
						// TakePays and Taker Gets should be 0 for fully
						// consumed
						System.out.println("MY ORDER IS TAKEN ");
						// In this case there is no new order created and seq
						// doesn't change
						STObject executed = myOC.executed(myOC.get(STObject.FinalFields));
						System.out.println("Get/Pay: " + myOC.getPayCurrencyPair());
						System.out.println("Ask:     " + myOC.directoryAskQuality().stripTrailingZeros().toPlainString());
						System.out.println("Paid:    " + executed.get(Amount.TakerPays));
						System.out.println("Got:     " + executed.get(Amount.TakerGets));
						System.out.println("PREVIOUS TX " + myOC.previousTxnID());

					});

				} else { // if we take other's order

					notMyExecutedOffers.forEach(oc -> {
						// This order is seen from the other guy's perspective
						// if FinalField's takerPays and TakerGets == 0 then we
						// fully consume the guy's order
						System.out.println("MY ORDER TAKES ");
						STObject executed = oc.executed(oc.get(STObject.FinalFields));
						System.out.println("Get/Pay: " + oc.getPayCurrencyPair());
						System.out.println("Ask:     " + oc.directoryAskQuality().stripTrailingZeros().toPlainString());
						System.out.println("Paid:    " + executed.get(Amount.TakerPays));
						System.out.println("Got:     " + executed.get(Amount.TakerGets));
						System.out.println("PREVIOUS TX " + oc.previousTxnID());

					});
				}
			}

		}

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
