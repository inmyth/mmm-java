package com.mbcu.mmm.utils;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.Credentials;
import com.mbcu.mmm.sequences.Counter;

public class Mock {


		public static void main(String[] args) throws Exception {
			testRaw();
		}
		
		private static void testRaw() throws Exception{
			String s =
"{\r\n   \"engine_result\": \"tesSUCCESS\",\r\n   \"engine_result_code\": 0,\r\n   \"engine_result_message\": \"The transaction was applied. Only final in a validated ledger.\",\r\n   \"ledger_hash\": \"CE19CA0188F6B01453E05A8B9FC6AE94FC260B5B5059B513EE2119199FDE9173\",\r\n   \"ledger_index\": 30754741,\r\n   \"meta\": {\r\n     \"AffectedNodes\": [{\r\n       \"CreatedNode\": {\r\n         \"LedgerEntryType\": \"Offer\",\r\n         \"LedgerIndex\": \"60B13F57D2B799D2C05EFB11C7C032D35D9320738AB07C6BDDAC3C10BBCFDFF1\",\r\n         \"NewFields\": {\r\n           \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n           \"BookDirectory\": \"7E984AD10069041B9B957C73D05AF2AB3D117C0CCAF7FDA5500AC40B67AED936\",\r\n           \"OwnerNode\": \"000000000000000F\",\r\n           \"Sequence\": 4740,\r\n           \"TakerGets\": {\r\n             \"currency\": \"JPY\",\r\n             \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n             \"value\": \"3300\"\r\n           },\r\n           \"TakerPays\": {\r\n             \"currency\": \"RJP\",\r\n             \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n             \"value\": \"0.1\"\r\n           }\r\n         }\r\n       }\r\n     }, {\r\n       \"ModifiedNode\": {\r\n         \"FinalFields\": {\r\n           \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n           \"Balance\": \"6060125313\",\r\n           \"Flags\": 0,\r\n           \"OwnerCount\": 10,\r\n           \"Sequence\": 4741\r\n         },\r\n         \"LedgerEntryType\": \"AccountRoot\",\r\n         \"LedgerIndex\": \"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\r\n         \"PreviousFields\": {\r\n           \"Balance\": \"6060125325\",\r\n           \"Sequence\": 4740\r\n         },\r\n         \"PreviousTxnID\": \"58950678AE345C974070D196D73F2244C5F8E90AF7C864D30087AE8F2560E697\",\r\n         \"PreviousTxnLgrSeq\": 30754511\r\n       }\r\n     }, {\r\n       \"CreatedNode\": {\r\n         \"LedgerEntryType\": \"DirectoryNode\",\r\n         \"LedgerIndex\": \"7E984AD10069041B9B957C73D05AF2AB3D117C0CCAF7FDA5500AC40B67AED936\",\r\n         \"NewFields\": {\r\n           \"ExchangeRate\": \"500AC40B67AED936\",\r\n           \"RootIndex\": \"7E984AD10069041B9B957C73D05AF2AB3D117C0CCAF7FDA5500AC40B67AED936\",\r\n           \"TakerGetsCurrency\": \"0000000000000000000000004A50590000000000\",\r\n           \"TakerGetsIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\",\r\n           \"TakerPaysCurrency\": \"000000000000000000000000524A500000000000\",\r\n           \"TakerPaysIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"\r\n         }\r\n       }\r\n     }, {\r\n       \"ModifiedNode\": {\r\n         \"FinalFields\": {\r\n           \"Flags\": 0,\r\n           \"IndexPrevious\": \"0000000000000006\",\r\n           \"Owner\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n           \"RootIndex\": \"C5383B94DDE4BE2A184757D80837080C4D1EAB80789FE39F7D7AECFB0D0304BB\"\r\n         },\r\n         \"LedgerEntryType\": \"DirectoryNode\",\r\n         \"LedgerIndex\": \"91565E4F1A8282718C8D6ED8EFEE2B5DC18101FC76D0AE436F46194F4567EF5E\"\r\n       }\r\n     }, {\r\n       \"DeletedNode\": {\r\n         \"FinalFields\": {\r\n           \"ExchangeRate\": \"590B6C0684F3F000\",\r\n           \"Flags\": 0,\r\n           \"RootIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B6C0684F3F000\",\r\n           \"TakerGetsCurrency\": \"000000000000000000000000524A500000000000\",\r\n           \"TakerGetsIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\",\r\n           \"TakerPaysCurrency\": \"0000000000000000000000004A50590000000000\",\r\n           \"TakerPaysIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"\r\n         },\r\n         \"LedgerEntryType\": \"DirectoryNode\",\r\n         \"LedgerIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B6C0684F3F000\"\r\n       }\r\n     }, {\r\n       \"DeletedNode\": {\r\n         \"FinalFields\": {\r\n           \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n           \"BookDirectory\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B6C0684F3F000\",\r\n           \"BookNode\": \"0000000000000000\",\r\n           \"Flags\": 131072,\r\n           \"OwnerNode\": \"000000000000000F\",\r\n           \"PreviousTxnID\": \"58950678AE345C974070D196D73F2244C5F8E90AF7C864D30087AE8F2560E697\",\r\n           \"PreviousTxnLgrSeq\": 30754511,\r\n           \"Sequence\": 4739,\r\n           \"TakerGets\": {\r\n             \"currency\": \"RJP\",\r\n             \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n             \"value\": \"6642980000000000e-26\"\r\n           },\r\n           \"TakerPays\": {\r\n             \"currency\": \"JPY\",\r\n             \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n             \"value\": \"0.000002135718\"\r\n           }\r\n         },\r\n         \"LedgerEntryType\": \"Offer\",\r\n         \"LedgerIndex\": \"FAE681F5F36A40EC2CB4C47AF50EBCFE646EC786B52C5003E7EC7469BC255475\"\r\n       }\r\n     }],\r\n     \"TransactionIndex\": 87,\r\n     \"TransactionResult\": \"tesSUCCESS\"\r\n   },\r\n   \"status\": \"closed\",\r\n   \"transaction\": {\r\n     \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n     \"Fee\": \"12\",\r\n     \"Flags\": 2147483648,\r\n     \"LastLedgerSequence\": 30754743,\r\n     \"Memos\": [{\r\n       \"Memo\": {\r\n         \"MemoData\": \"726D2D312E322E33\",\r\n         \"MemoType\": \"636C69656E74\"\r\n       }\r\n     }],\r\n     \"Sequence\": 4740,\r\n     \"SigningPubKey\": \"02E2F1208D1715E18B0957FC819546FA7434B4A19EE38321932D2ED28FA090678E\",\r\n     \"TakerGets\": {\r\n       \"currency\": \"JPY\",\r\n       \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n       \"value\": \"3300\"\r\n     },\r\n     \"TakerPays\": {\r\n       \"currency\": \"RJP\",\r\n       \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n       \"value\": \"0.1\"\r\n     },\r\n     \"TransactionType\": \"OfferCreate\",\r\n     \"TxnSignature\": \"30440220101ABC9FB31CFAC747C5B9833341C4832A194C2C57C3A8060E1D9EB2CD6938240220029CF0AB692146431AAB7B8766FD236DEF1D31945F689403917ECF2546B8430A\",\r\n     \"date\": 551676281,\r\n     \"hash\": \"9A5A9ECA97CD3F3E028F3BC663E336F7643A4A58749AD2A63D4399EE37175C1D\",\r\n     \"owner_funds\": \"9999.86990579025\"\r\n   },\r\n   \"type\": \"transaction\",\r\n   \"validated\": true\r\n }";

			String EF7BF9F523E74777027EE4765F542532FF89B6E1132685D6E75C4AFFF072151C  = "{\r\n  \"engine_result\": \"tesSUCCESS\",\r\n  \"engine_result_code\": 0,\r\n  \"engine_result_message\": \"The transaction was applied. Only final in a validated ledger.\",\r\n  \"ledger_hash\": \"1942CA83566B16802700FB537BAE8ED13EB9A6C5E79EF8072F633C3FF0076AFF\",\r\n  \"ledger_index\": 30752350,\r\n  \"meta\": {\r\n    \"AffectedNodes\": [{\r\n        \"ModifiedNode\": {\r\n          \"FinalFields\": {\r\n            \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n            \"Balance\": \"6060125289\",\r\n            \"Flags\": 0,\r\n            \"OwnerCount\": 9,\r\n            \"Sequence\": 4743\r\n          },\r\n          \"LedgerEntryType\": \"AccountRoot\",\r\n          \"LedgerIndex\": \"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\r\n          \"PreviousFields\": {\r\n            \"Balance\": \"6060125301\",\r\n            \"OwnerCount\": 10,\r\n            \"Sequence\": 4742\r\n          },\r\n          \"PreviousTxnID\": \"503DD0C7597051A4592E8DABC2BA4AA286231FFE3A30AB855D9A569CCB887132\",\r\n          \"PreviousTxnLgrSeq\": 30756200\r\n        }\r\n      },\r\n      {\r\n        \"DeletedNode\": {\r\n          \"FinalFields\": {\r\n            \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n            \"BookDirectory\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590BACC80FA93000\",\r\n            \"BookNode\": \"0000000000000000\",\r\n            \"Flags\": 131072,\r\n            \"OwnerNode\": \"000000000000000F\",\r\n            \"PreviousTxnID\": \"503DD0C7597051A4592E8DABC2BA4AA286231FFE3A30AB855D9A569CCB887132\",\r\n            \"PreviousTxnLgrSeq\": 30756200,\r\n            \"Sequence\": 4741,\r\n            \"TakerGets\": {\r\n              \"currency\": \"RJP\",\r\n              \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n              \"value\": \"0.1\"\r\n            },\r\n            \"TakerPays\": {\r\n              \"currency\": \"JPY\",\r\n              \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n              \"value\": \"3286.2\"\r\n            }\r\n          },\r\n          \"LedgerEntryType\": \"Offer\",\r\n          \"LedgerIndex\": \"76BEC8D4790AC90F0C9FCECFCBE9DC6969AD2866114362AE4AA2B0382F785B29\"\r\n        }\r\n      },\r\n      {\r\n        \"ModifiedNode\": {\r\n          \"FinalFields\": {\r\n            \"Flags\": 0,\r\n            \"IndexPrevious\": \"0000000000000006\",\r\n            \"Owner\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n            \"RootIndex\": \"C5383B94DDE4BE2A184757D80837080C4D1EAB80789FE39F7D7AECFB0D0304BB\"\r\n          },\r\n          \"LedgerEntryType\": \"DirectoryNode\",\r\n          \"LedgerIndex\": \"91565E4F1A8282718C8D6ED8EFEE2B5DC18101FC76D0AE436F46194F4567EF5E\"\r\n        }\r\n      },\r\n      {\r\n        \"DeletedNode\": {\r\n          \"FinalFields\": {\r\n            \"ExchangeRate\": \"590BACC80FA93000\",\r\n            \"Flags\": 0,\r\n            \"RootIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590BACC80FA93000\",\r\n            \"TakerGetsCurrency\": \"000000000000000000000000524A500000000000\",\r\n            \"TakerGetsIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\",\r\n            \"TakerPaysCurrency\": \"0000000000000000000000004A50590000000000\",\r\n            \"TakerPaysIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"\r\n          },\r\n          \"LedgerEntryType\": \"DirectoryNode\",\r\n          \"LedgerIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590BACC80FA93000\"\r\n        }\r\n      }\r\n    ],\r\n    \"TransactionIndex\": 0,\r\n    \"TransactionResult\": \"tesSUCCESS\"\r\n  },\r\n  \"status\": \"closed\",\r\n  \"transaction\": {\r\n    \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n    \"Fee\": \"12\",\r\n    \"Flags\": 2147483648,\r\n    \"LastLedgerSequence\": 30752352,\r\n    \"Memos\": [{\r\n      \"Memo\": {\r\n        \"MemoData\": \"726D2D312E322E33\",\r\n        \"MemoType\": \"636C69656E74\"\r\n      }\r\n    }],\r\n    \"Sequence\": 4734,\r\n    \"SigningPubKey\": \"02E2F1208D1715E18B0957FC819546FA7434B4A19EE38321932D2ED28FA090678E\",\r\n    \"TakerGets\": {\r\n      \"currency\": \"JPY\",\r\n      \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n      \"value\": \"159.25\"\r\n    },\r\n    \"TakerPays\": \"5000000\",\r\n    \"TransactionType\": \"OfferCreate\",\r\n    \"TxnSignature\": \"3044022032523881EED58446104B57C17EBF71D8125D7702DC054BA6B3EFDD8CA1755AF7022013A240D77602E319F67ED6B7AA7868A8E986586E1DF015833C829223EBA6A81C\",\r\n    \"date\": 551667961,\r\n    \"hash\": \"8E943C78B4783AF66571EC86331DC8C4444DD34A24A4A9D24748F633AE71ED5F\",\r\n    \"owner_funds\": \"7727.279530849417\"\r\n  },\r\n  \"type\": \"transaction\",\r\n  \"validated\": true\r\n}";
			
			String z = "{\r\n  \"engine_result\": \"tesSUCCESS\",\r\n  \"engine_result_code\": 0,\r\n  \"engine_result_message\": \"The transaction was applied. Only final in a validated ledger.\",\r\n  \"ledger_hash\": \"9193A88F249093A33D0DA5603A4762B1C4E4F8611BF8BBF091E1BC99CC19A79E\",\r\n  \"ledger_index\": 30764869,\r\n  \"meta\": {\r\n    \"AffectedNodes\": [{\r\n      \"DeletedNode\": {\r\n        \"FinalFields\": {\r\n          \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n          \"BookDirectory\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B51A66E412000\",\r\n          \"BookNode\": \"0000000000000000\",\r\n          \"Flags\": 131072,\r\n          \"OwnerNode\": \"000000000000000F\",\r\n          \"PreviousTxnID\": \"326DCCB5373A3D588E1870F3840CB4B3B62988D1C667389D9B63F6EE0E990A29\",\r\n          \"PreviousTxnLgrSeq\": 30764849,\r\n          \"Sequence\": 4758,\r\n          \"TakerGets\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          },\r\n          \"TakerPays\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          }\r\n        },\r\n        \"LedgerEntryType\": \"Offer\",\r\n        \"LedgerIndex\": \"121E7C39F62C053F0D66496A086573EA84DAFF022F3CC033998656B12D384C4C\",\r\n        \"PreviousFields\": {\r\n          \"TakerGets\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0.09652923889012509\"\r\n          },\r\n          \"TakerPays\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"3075.421551039386\"\r\n          }\r\n        }\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"10526.38159339581\"\r\n          },\r\n          \"Flags\": 1114112,\r\n          \"HighLimit\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          },\r\n          \"HighNode\": \"0000000000000399\",\r\n          \"LowLimit\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n            \"value\": \"100000\"\r\n          },\r\n          \"LowNode\": \"0000000000000001\"\r\n        },\r\n        \"LedgerEntryType\": \"RippleState\",\r\n        \"LedgerIndex\": \"21110D01A36A7EB20188B11D685B847322DE631145C31803E1760550FA399BAD\",\r\n        \"PreviousFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"7450.96004235643\"\r\n          }\r\n        },\r\n        \"PreviousTxnID\": \"326DCCB5373A3D588E1870F3840CB4B3B62988D1C667389D9B63F6EE0E990A29\",\r\n        \"PreviousTxnLgrSeq\": 30764849\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"4.875721472777413\"\r\n          },\r\n          \"Flags\": 1114112,\r\n          \"HighLimit\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          },\r\n          \"HighNode\": \"00000000000003FB\",\r\n          \"LowLimit\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n            \"value\": \"10000\"\r\n          },\r\n          \"LowNode\": \"0000000000000006\",\r\n          \"LowQualityIn\": 1000000000\r\n        },\r\n        \"LedgerEntryType\": \"RippleState\",\r\n        \"LedgerIndex\": \"4A8FE98984B510E3F0223CB8C9110FA370EC545BCD6CE6863C8405E5BC360423\",\r\n        \"PreviousFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"4.972443770145318\"\r\n          }\r\n        },\r\n        \"PreviousTxnID\": \"326DCCB5373A3D588E1870F3840CB4B3B62988D1C667389D9B63F6EE0E990A29\",\r\n        \"PreviousTxnLgrSeq\": 30764849\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Account\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n          \"Balance\": \"6061125097\",\r\n          \"Flags\": 0,\r\n          \"OwnerCount\": 9,\r\n          \"Sequence\": 4759\r\n        },\r\n        \"LedgerEntryType\": \"AccountRoot\",\r\n        \"LedgerIndex\": \"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD\",\r\n        \"PreviousFields\": {\r\n          \"OwnerCount\": 10\r\n        },\r\n        \"PreviousTxnID\": \"326DCCB5373A3D588E1870F3840CB4B3B62988D1C667389D9B63F6EE0E990A29\",\r\n        \"PreviousTxnLgrSeq\": 30764849\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"0.09652923889012509\"\r\n          },\r\n          \"Flags\": 1114112,\r\n          \"HighLimit\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          },\r\n          \"HighNode\": \"0000000000000338\",\r\n          \"LowLimit\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rsPrWzpYp5jj278vHVgp1tMvbKdHAZGFbi\",\r\n            \"value\": \"1000000000\"\r\n          },\r\n          \"LowNode\": \"0000000000000001\"\r\n        },\r\n        \"LedgerEntryType\": \"RippleState\",\r\n        \"LedgerIndex\": \"7BE65D79F3087B4870A4E8596F1533D9761D53C05B0C51C1DEB5B0DA6A58C27C\",\r\n        \"PreviousFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"RJP\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"0\"\r\n          }\r\n        },\r\n        \"PreviousTxnID\": \"B97E75D1336FA0455F3090A6653330879684420C50EBA3E4A34FE49E6C245095\",\r\n        \"PreviousTxnLgrSeq\": 30572935\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Account\": \"rsPrWzpYp5jj278vHVgp1tMvbKdHAZGFbi\",\r\n          \"Balance\": \"13973364092\",\r\n          \"Flags\": 0,\r\n          \"OwnerCount\": 37,\r\n          \"Sequence\": 6864595\r\n        },\r\n        \"LedgerEntryType\": \"AccountRoot\",\r\n        \"LedgerIndex\": \"8881335E10619D1D17D4EB1FEF2647C232C892D377D9A34998C361FFFC9C4CCC\",\r\n        \"PreviousFields\": {\r\n          \"Balance\": \"13973389092\",\r\n          \"Sequence\": 6864594\r\n        },\r\n        \"PreviousTxnID\": \"E7A9F315481A83F3CCE308298D0DEEC5A8AFEB3BE347E3046BFDFE0FB69DAF9A\",\r\n        \"PreviousTxnLgrSeq\": 30764869\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Flags\": 0,\r\n          \"IndexPrevious\": \"0000000000000006\",\r\n          \"Owner\": \"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf\",\r\n          \"RootIndex\": \"C5383B94DDE4BE2A184757D80837080C4D1EAB80789FE39F7D7AECFB0D0304BB\"\r\n        },\r\n        \"LedgerEntryType\": \"DirectoryNode\",\r\n        \"LedgerIndex\": \"91565E4F1A8282718C8D6ED8EFEE2B5DC18101FC76D0AE436F46194F4567EF5E\"\r\n      }\r\n    }, {\r\n      \"ModifiedNode\": {\r\n        \"FinalFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"656.124172136904\"\r\n          },\r\n          \"Flags\": 1114112,\r\n          \"HighLimit\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n            \"value\": \"0\"\r\n          },\r\n          \"HighNode\": \"0000000000000232\",\r\n          \"LowLimit\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rsPrWzpYp5jj278vHVgp1tMvbKdHAZGFbi\",\r\n            \"value\": \"1000000000\"\r\n          },\r\n          \"LowNode\": \"0000000000000000\"\r\n        },\r\n        \"LedgerEntryType\": \"RippleState\",\r\n        \"LedgerIndex\": \"ADCD69C9DAFD1076DA7075D0E53667CB67EDD58675BD653162A7EA483E392851\",\r\n        \"PreviousFields\": {\r\n          \"Balance\": {\r\n            \"currency\": \"JPY\",\r\n            \"issuer\": \"rrrrrrrrrrrrrrrrrrrrBZbvji\",\r\n            \"value\": \"3737.696566278369\"\r\n          }\r\n        },\r\n        \"PreviousTxnID\": \"7504557145245B931556B275388001423ACFA2EC886D8C5253B7D565550B2A4A\",\r\n        \"PreviousTxnLgrSeq\": 30764868\r\n      }\r\n    }, {\r\n      \"DeletedNode\": {\r\n        \"FinalFields\": {\r\n          \"ExchangeRate\": \"590B51A66E412000\",\r\n          \"Flags\": 0,\r\n          \"RootIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B51A66E412000\",\r\n          \"TakerGetsCurrency\": \"000000000000000000000000524A500000000000\",\r\n          \"TakerGetsIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\",\r\n          \"TakerPaysCurrency\": \"0000000000000000000000004A50590000000000\",\r\n          \"TakerPaysIssuer\": \"6F2531F2B8CDB96D6D986D9D75CC0156DF2C5387\"\r\n        },\r\n        \"LedgerEntryType\": \"DirectoryNode\",\r\n        \"LedgerIndex\": \"DCAAC0CDAC8DA851AC8E225CA9ED9305CB4BBE7E4E255AB3590B51A66E412000\"\r\n      }\r\n    }],\r\n    \"TransactionIndex\": 7,\r\n    \"TransactionResult\": \"tesSUCCESS\"\r\n  },\r\n  \"status\": \"closed\",\r\n  \"transaction\": {\r\n    \"Account\": \"rsPrWzpYp5jj278vHVgp1tMvbKdHAZGFbi\",\r\n    \"Fee\": \"25000\",\r\n    \"Flags\": 131072,\r\n    \"LastLedgerSequence\": 30764871,\r\n    \"Sequence\": 6864594,\r\n    \"SigningPubKey\": \"031161F2602A6746CA817D3EE4419F29641690CD613B015B923A2B8146BDEA7028\",\r\n    \"TakerGets\": {\r\n      \"currency\": \"JPY\",\r\n      \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n      \"value\": \"37391.9164490488\"\r\n    },\r\n    \"TakerPays\": {\r\n      \"currency\": \"RJP\",\r\n      \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n      \"value\": \"1.173162764054729\"\r\n    },\r\n    \"TransactionType\": \"OfferCreate\",\r\n    \"TxnSignature\": \"304502210081DFD7601B57813543FEF52F83818492704794D4E4C025C3CBF45CF63D8E741502206964B15D3D5912E284BC85D9ECD52600B6A3DA544D91A8A53E69360D472C4C42\",\r\n    \"date\": 551710353,\r\n    \"hash\": \"47B4F8DD0DE3D49AA47196C1CE6DB50231B90EA34A7D720F3AAE9CCE956624E9\",\r\n    \"owner_funds\": \"656.124172136904\"\r\n  },\r\n  \"type\": \"transaction\",\r\n  \"validated\": true\r\n}";
			
			
			Counter.testOfferQuality(z);
			
		}

}
