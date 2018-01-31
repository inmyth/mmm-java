package com.mbcu.mmm.sequences.counters;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.sequences.Common;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public class YukiTest {
	private static final String configPath = "config.txt";
	private Yuki yuki;
	private Common c;

	@Before
	public void init() throws IOException {
		c = Common.newInstance(Config.build(configPath));
		yuki = Yuki.newInstance(Config.build(configPath));
	}

	/*
	 * 2017-07-12, 10:54:30 Made offer to buy 2 JPY.TokyoJPY at price 1.00579
	 * JPY.MrRipple bought 0.693638 JPY.TokyoJPY at price 0.050171 XRP bought
	 * 0.100348 XRP at price 19.8 JPY.MrRipple bought 1.30636 JPY.TokyoJPY at
	 * price 0.050175 XRP
	 * 
	 * buy quantity:-0.69363877168/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * totalPrice:-0.6890598/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * rate:0.9933744005069883 buy
	 * quantity:-1.30636122832/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * totalPrice:-1.2978306/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * rate:0.9934771700953337
	 * 
	 * for "pair":
	 * "JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/JPY.r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN",
	 * "gridSpace": 0.0005, matches buy
	 * quantity:0.6890598/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * totalPrice:0.69329424178/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * rate:1.006145245710169131909886485904416
	 * pair:JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/JPY.
	 * r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * 
	 * buy quantity:1.2978306/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * totalPrice:1.30571231302/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * rate:1.006072990589064551259617395367315
	 * pair:JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/JPY.
	 * r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * 
	 * for "pair":
	 * "JPY.r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN/JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS",
	 * "gridSpace": 0.0005 sell
	 * quantity:0.69363877168/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * totalPrice:0.68940661938584/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * rate:0.9938986220682132789743019862098721
	 * pair:JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/JPY.
	 * r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * 
	 * sell quantity:1.30636122832/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * totalPrice:1.29848378061416/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
	 * rate:0.9939699314897989470361595399005741
	 * pair:JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/JPY.
	 * r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
	 * 
	 */

	// @Test
	// public void testAutobridgeArbitrage(){
	// Amount q1 = RLOrder.amount(new BigDecimal("-0.69363877168"),
	// Currency.fromString("JPY"),
	// AccountID.fromAddress("r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN"));
	// Amount t1 = RLOrder.amount(new BigDecimal("-0.6890598"),
	// Currency.fromString("JPY"),
	// AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
	// Amount q2 = RLOrder.amount(new BigDecimal("-1.30636122832"),
	// Currency.fromString("JPY"),
	// AccountID.fromAddress("r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN"));
	// Amount t2 = RLOrder.amount(new BigDecimal("-1.2978306"),
	// Currency.fromString("JPY"),
	// AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
	// RLOrder r1 = RLOrder.rateUnneeded(Direction.BUY, q1, t1);
	// RLOrder r2 = RLOrder.rateUnneeded(Direction.BUY, q2, t2);
	//
	// List<RLOrder> list = new ArrayList<>();
	// list.add(r1);
	// list.add(r2);
	// list.forEach(oe -> {
	//
	// System.out.println(yuki.buildCounter(oe).stringify());
	// });
	//
	//
	//
	// }
	//

	// @Test
	// public void testORConsume(){
	//// String consumeOneHalf =
	// "{\"engine_result\":\"tesSUCCESS\",\"engine_result_code\":0,\"engine_result_message\":\"The
	// transaction was applied. Only final in a validated
	// ledger.\",\"ledger_hash\":\"A53C42A135396D1C9B7708E96977CB71454F4AE6C7900D0CF30B62AEC3FE5851\",\"ledger_index\":30854512,\"meta\":{\"AffectedNodes\":[{\"DeletedNode\":{\"FinalFields\":{\"ExchangeRate\":\"500A7AB7B21D6FCE\",\"Flags\":0,\"RootIndex\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\",\"TakerGetsCurrency\":\"0000000000000000000000000000000000000000\",\"TakerGetsIssuer\":\"0000000000000000000000000000000000000000\",\"TakerPaysCurrency\":\"0000000000000000000000004A50590000000000\",\"TakerPaysIssuer\":\"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"},\"LedgerEntryType\":\"DirectoryNode\",\"LedgerIndex\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\"}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"25493.59330038829\"},\"Flags\":1114112,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"HighNode\":\"0000000000000399\",\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"value\":\"100000\"},\"LowNode\":\"0000000000000001\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"21110D01A36A7EB20188B11D685B847322DE631145C31803E1760550FA399BAD\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"25552.70487260873\"}},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}},{\"ModifiedNode\":{\"FinalFields\":{\"Flags\":0,\"IndexPrevious\":\"0000000000001044\",\"Owner\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"RootIndex\":\"718BBB28EE36A3AC1DCBC75790357E2057015302DBA637DFD7851E820B5583BA\"},\"LedgerEntryType\":\"DirectoryNode\",\"LedgerIndex\":\"3B2BE34F74E8E10113F2D4C84053344F678D64E12D537D8D931D8981DF3AC7F4\"}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"Balance\":\"2517023578\",\"Flags\":0,\"OwnerCount\":9,\"Sequence\":5451},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\"PreviousFields\":{\"Balance\":\"2515023698\",\"Sequence\":5450},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"Balance\":\"1686510443008\",\"Flags\":0,\"OwnerCount\":2546,\"Sequence\":229780},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"7F28475BBAE8E0654C4D4C876DFBF7A3C40DEFEB6DF2F4C301F7F7F3514F255F\",\"PreviousFields\":{\"Balance\":\"1686512443008\",\"OwnerCount\":2547},\"PreviousTxnID\":\"9563022137D4F43F080FE59F74847D41C4C3222DB4FC0A70560AFDC33771712A\",\"PreviousTxnLgrSeq\":30854493}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"BookDirectory\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7C76B2D09\",\"BookNode\":\"0000000000000000\",\"Flags\":131072,\"OwnerNode\":\"0000000000001045\",\"Sequence\":229778,\"TakerGets\":\"7885940\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"232.6099563824587\"}},\"LedgerEntryType\":\"Offer\",\"LedgerIndex\":\"83F71EE4B60B9016D25138FCE5D0D1E8DEF8C794940B42B53C8082516F1BEF40\",\"PreviousFields\":{\"TakerGets\":\"8442902\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"249.0385503771742\"}},\"PreviousTxnID\":\"5A2B58D139CDEADCD4B1F6DDE2CB766E5C9BECCD893AEB6747558D76E6586785\",\"PreviousTxnLgrSeq\":30854493}},{\"DeletedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"BookDirectory\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\",\"BookNode\":\"0000000000000000\",\"Flags\":131072,\"OwnerNode\":\"0000000000001045\",\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492,\"Sequence\":229774,\"TakerGets\":\"0\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"}},\"LedgerEntryType\":\"Offer\",\"LedgerIndex\":\"85794675649594FC8F8595FB5BB02342ABA3D5801DA543C13BCE89558BA3021C\",\"PreviousFields\":{\"TakerGets\":\"1443038\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"42.56499105563746\"}}}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"-25536929.46912782\"},\"Flags\":2228224,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"value\":\"10000000000\"},\"HighNode\":\"0000000000000000\",\"HighQualityIn\":1000000000,\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"LowNode\":\"0000000000000001\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"FB9399D7485EA21D73E649E9F821B8C10A1008285E32107B6F30F934723EB5C8\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"-25536870.47554278\"}},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}}],\"TransactionIndex\":4,\"TransactionResult\":\"tesSUCCESS\"},\"status\":\"closed\",\"transaction\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"Fee\":\"120\",\"Flags\":2147483648,\"LastLedgerSequence\":30854514,\"Memos\":[{\"Memo\":{\"MemoData\":\"726D2D312E322E33\",\"MemoType\":\"636C69656E74\"}}],\"Sequence\":5450,\"SigningPubKey\":\"02E2F1208D1715E18B0957FC819546FA7434B4A19EE38321932D2ED28FA090678E\",\"TakerGets\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"60\"},\"TakerPays\":\"2000000\",\"TransactionType\":\"OfferCreate\",\"TxnSignature\":\"3045022100EE1BC622B0768D2F01515AE8BC8157EA3EF361BB65C9E680C44CE7C87066CE4402207594042DD3E0FD7C8D0B59B84C96ECD0BF8780CB85B893FD5626F877CB3FB6F5\",\"date\":552025652,\"hash\":\"024C1B17CE1C35425550B6CC5676230B488C5680CADE21D226F8EA2D3594204D\",\"owner_funds\":\"25493.59330038829\"},\"type\":\"transaction\",\"validated\":true}\r\n";
	//// c.filterStream2(consumeOneHalf);
	// String consumeOneHalf2 =
	// "{\"engine_result\":\"tesSUCCESS\",\"engine_result_code\":0,\"engine_result_message\":\"The
	// transaction was applied. Only final in a validated
	// ledger.\",\"ledger_hash\":\"A53C42A135396D1C9B7708E96977CB71454F4AE6C7900D0CF30B62AEC3FE5851\",\"ledger_index\":30854512,\"meta\":{\"AffectedNodes\":[{\"DeletedNode\":{\"FinalFields\":{\"ExchangeRate\":\"500A7AB7B21D6FCE\",\"Flags\":0,\"RootIndex\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\",\"TakerGetsCurrency\":\"0000000000000000000000000000000000000000\",\"TakerGetsIssuer\":\"0000000000000000000000000000000000000000\",\"TakerPaysCurrency\":\"0000000000000000000000004A50590000000000\",\"TakerPaysIssuer\":\"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"},\"LedgerEntryType\":\"DirectoryNode\",\"LedgerIndex\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\"}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"25493.59330038829\"},\"Flags\":1114112,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"HighNode\":\"0000000000000399\",\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"value\":\"100000\"},\"LowNode\":\"0000000000000001\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"21110D01A36A7EB20188B11D685B847322DE631145C31803E1760550FA399BAD\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"25552.70487260873\"}},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}},{\"ModifiedNode\":{\"FinalFields\":{\"Flags\":0,\"IndexPrevious\":\"0000000000001044\",\"Owner\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"RootIndex\":\"718BBB28EE36A3AC1DCBC75790357E2057015302DBA637DFD7851E820B5583BA\"},\"LedgerEntryType\":\"DirectoryNode\",\"LedgerIndex\":\"3B2BE34F74E8E10113F2D4C84053344F678D64E12D537D8D931D8981DF3AC7F4\"}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"Balance\":\"2517023578\",\"Flags\":0,\"OwnerCount\":9,\"Sequence\":5451},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\"PreviousFields\":{\"Balance\":\"2515023698\",\"Sequence\":5450},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"Balance\":\"1686510443008\",\"Flags\":0,\"OwnerCount\":2546,\"Sequence\":229780},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"7F28475BBAE8E0654C4D4C876DFBF7A3C40DEFEB6DF2F4C301F7F7F3514F255F\",\"PreviousFields\":{\"Balance\":\"1686512443008\",\"OwnerCount\":2547},\"PreviousTxnID\":\"9563022137D4F43F080FE59F74847D41C4C3222DB4FC0A70560AFDC33771712A\",\"PreviousTxnLgrSeq\":30854493}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"BookDirectory\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7C76B2D09\",\"BookNode\":\"0000000000000000\",\"Flags\":131072,\"OwnerNode\":\"0000000000001045\",\"Sequence\":229778,\"TakerGets\":\"7885940\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"232.6099563824587\"}},\"LedgerEntryType\":\"Offer\",\"LedgerIndex\":\"83F71EE4B60B9016D25138FCE5D0D1E8DEF8C794940B42B53C8082516F1BEF40\",\"PreviousFields\":{\"TakerGets\":\"8442902\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"249.0385503771742\"}},\"PreviousTxnID\":\"5A2B58D139CDEADCD4B1F6DDE2CB766E5C9BECCD893AEB6747558D76E6586785\",\"PreviousTxnLgrSeq\":30854493}},{\"DeletedNode\":{\"FinalFields\":{\"Account\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"BookDirectory\":\"11EEBF7DFC0076D299322039F1493C921CAAEFE85B322E5D500A7AB7B21D6FCE\",\"BookNode\":\"0000000000000000\",\"Flags\":131072,\"OwnerNode\":\"0000000000001045\",\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492,\"Sequence\":229774,\"TakerGets\":\"0\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"}},\"LedgerEntryType\":\"Offer\",\"LedgerIndex\":\"85794675649594FC8F8595FB5BB02342ABA3D5801DA543C13BCE89558BA3021C\",\"PreviousFields\":{\"TakerGets\":\"1443038\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"42.56499105563746\"}}}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"-25536929.46912782\"},\"Flags\":2228224,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rLjDNH9g1AajRT2pxALZmwesd64p4x9XZJ\",\"value\":\"10000000000\"},\"HighNode\":\"0000000000000000\",\"HighQualityIn\":1000000000,\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"LowNode\":\"0000000000000001\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"FB9399D7485EA21D73E649E9F821B8C10A1008285E32107B6F30F934723EB5C8\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"-25536870.47554278\"}},\"PreviousTxnID\":\"D3A84F7F810AC01AE88B7C120A01DB5694DE4775AE3DBE2613C849EBBA75C6F3\",\"PreviousTxnLgrSeq\":30854492}}],\"TransactionIndex\":4,\"TransactionResult\":\"tesSUCCESS\"},\"status\":\"closed\",\"transaction\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"Fee\":\"120\",\"Flags\":2147483648,\"LastLedgerSequence\":30854514,\"Memos\":[{\"Memo\":{\"MemoData\":\"726D2D312E322E33\",\"MemoType\":\"636C69656E74\"}}],\"Sequence\":5450,\"SigningPubKey\":\"02E2F1208D1715E18B0957FC819546FA7434B4A19EE38321932D2ED28FA090678E\",\"TakerGets\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"60\"},\"TakerPays\":\"2000000\",\"TransactionType\":\"OfferCreate\",\"TxnSignature\":\"3045022100EE1BC622B0768D2F01515AE8BC8157EA3EF361BB65C9E680C44CE7C87066CE4402207594042DD3E0FD7C8D0B59B84C96ECD0BF8780CB85B893FD5626F877CB3FB6F5\",\"date\":552025652,\"hash\":\"024C1B17CE1C35425550B6CC5676230B488C5680CADE21D226F8EA2D3594204D\",\"owner_funds\":\"25493.59330038829\"},\"type\":\"transaction\",\"validated\":true}\r\n";
	// c.filterStream2(consumeOneHalf2);
	// }

	@Test
	public void testORConsumed() {

		/*
		 * { "pair": "XRP/JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS",
		 * "startMiddlePrice": 20.3, "gridSpace": 0.1, "buyGridLevels": 10,
		 * "sellGridLevels": 10, "buyOrderQuantity": 2, "sellOrderQuantity": 2 }
		 * 
		 */

		/*
		 * percentToCounter 50
		 */
		String consumedHalf = "{\"engine_result\":\"tesSUCCESS\",\"engine_result_code\":0,\"engine_result_message\":\"The transaction was applied. Only final in a validated ledger.\",\"ledger_hash\":\"23B2DF6090F92CF440EE8F3B9A57BC9855DBC7FF8D1EF3DA6ADEAC0690C3EF51\",\"ledger_index\":30856627,\"meta\":{\"AffectedNodes\":[{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"BookDirectory\":\"A7A2258942BF79A1C2A55DA88560879DC312966AFDD1CD05590C03BC0A6F7FDE\",\"BookNode\":\"0000000000000000\",\"Flags\":0,\"OwnerNode\":\"0000000000000018\",\"Sequence\":5456,\"TakerGets\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"88.71\"},\"TakerPays\":\"3000000\"},\"LedgerEntryType\":\"Offer\",\"LedgerIndex\":\"056871F43F37D6B924F7D35F4D2C0DF6D1E02A71E638EA5D454F5211FBC21420\",\"PreviousFields\":{\"TakerGets\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"147.85\"},\"TakerPays\":\"5000000\"},\"PreviousTxnID\":\"677CA05F654AE8A07936A7205C6925746CB00A6B615105639C428857096963B3\",\"PreviousTxnLgrSeq\":30856614}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"24903.01942038829\"},\"Flags\":1114112,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"HighNode\":\"0000000000000399\",\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"value\":\"100000\"},\"LowNode\":\"0000000000000001\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"21110D01A36A7EB20188B11D685B847322DE631145C31803E1760550FA399BAD\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"24962.27770038829\"}},\"PreviousTxnID\":\"7FE4EDDC776B3ECC777444F5DF7CCB3AC7CC6C105B935BC72A63993DD9C86347\",\"PreviousTxnLgrSeq\":30856584}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\"Balance\":\"2537023506\",\"Flags\":0,\"OwnerCount\":12,\"Sequence\":5457},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\"PreviousFields\":{\"Balance\":\"2535023506\"},\"PreviousTxnID\":\"677CA05F654AE8A07936A7205C6925746CB00A6B615105639C428857096963B3\",\"PreviousTxnLgrSeq\":30856614}},{\"ModifiedNode\":{\"FinalFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"5226.319164203065\"},\"Flags\":1114112,\"HighLimit\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"0\"},\"HighNode\":\"00000000000001BC\",\"LowLimit\":{\"currency\":\"JPY\",\"issuer\":\"rHMjZANhquizS74FutV3CNoWfQcoVkBxf\",\"value\":\"10000000000\"},\"LowNode\":\"0000000000000000\"},\"LedgerEntryType\":\"RippleState\",\"LedgerIndex\":\"9E03D12B7281B41A487F01494BCE1A7099624E50E4FEF5ACE11B2D5BB3C353FB\",\"PreviousFields\":{\"Balance\":{\"currency\":\"JPY\",\"issuer\":\"rrrrrrrrrrrrrrrrrrrrBZbvji\",\"value\":\"5167.179164203065\"}},\"PreviousTxnID\":\"7FE4EDDC776B3ECC777444F5DF7CCB3AC7CC6C105B935BC72A63993DD9C86347\",\"PreviousTxnLgrSeq\":30856584}},{\"ModifiedNode\":{\"FinalFields\":{\"Account\":\"rHMjZANhquizS74FutV3CNoWfQcoVkBxf\",\"Balance\":\"3419983825\",\"Flags\":0,\"OwnerCount\":2,\"Sequence\":43},\"LedgerEntryType\":\"AccountRoot\",\"LedgerIndex\":\"E828FC718630DEC1E542D4D2A50074CDB7B3183062FBC59D6E88B63198BF27BF\",\"PreviousFields\":{\"Balance\":\"3421983837\",\"Sequence\":42},\"PreviousTxnID\":\"7FE4EDDC776B3ECC777444F5DF7CCB3AC7CC6C105B935BC72A63993DD9C86347\",\"PreviousTxnLgrSeq\":30856584}}],\"TransactionIndex\":32,\"TransactionResult\":\"tesSUCCESS\"},\"status\":\"closed\",\"transaction\":{\"Account\":\"rHMjZANhquizS74FutV3CNoWfQcoVkBxf\",\"Fee\":\"12\",\"Flags\":2148007936,\"LastLedgerSequence\":30856629,\"Memos\":[{\"Memo\":{\"MemoData\":\"726D2D312E322E33\",\"MemoType\":\"636C69656E74\"}}],\"Sequence\":42,\"SigningPubKey\":\"03E8BBF8387DCDCE12F8B8F3CAC425D2587548E6510659E27F74E02EC7FE1DAE0E\",\"TakerGets\":\"2000000\",\"TakerPays\":{\"currency\":\"JPY\",\"issuer\":\"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\"value\":\"59.14\"},\"TransactionType\":\"OfferCreate\",\"TxnSignature\":\"3044022045BE95C9BC4AEDABF74AA052E72394819A1B9D7374E7FDD0F92C35F438290E45022064DF94A41EBCAFCCE246E27EE336A97D669CA1D26085D4E1B2D2940E394AD6D6\",\"date\":552033351,\"hash\":\"FC1649A761A589BAE43B22657B1A6A41942818E92EB7715C98A3B382DAED9879\",\"owner_funds\":\"3389983825\"},\"type\":\"transaction\",\"validated\":true}\r\n";
		c.filterStream2(consumedHalf, "rHMjZANhquizS74FutV3CNoWfQcoVkBxf");
	}
}
