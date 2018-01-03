# Java Ripple Market Maker

An automated program to market make on Ripple network with grid trading strategy. 

Requires [ripple-lib-java](https://github.com/ripple/ripple-lib-java ) to build. 


Consists of:

**Parser**  : parses response from stream and listens to events of interest

**Counter** : counters each order consumed with a new order

**Balancer**: maintains the number of orders in orderbook as set by config

**State**   : keeps track of account sequence number which is used to send orders 


To use:
```
java -jar <the_build>.jar <path_to_config_file>
```

### Strategies

In general the principle of grid spacing is simple. Seed orderbook with orders spaced by price ("seed"). If an order is consumed, place a new order with a new rate calculated from the consumed rate ("counter"). As we want profit, when a buy order is consumed, sell it back at higher price, when a sell order is consumed, buy it back at lower price. Every a few ledgers, the bot will check the orderbook and add missing orders on either side. 

The IOU to trade, number of orders, grid space, amount, etc are defined in the config. 


#### Partial `partial`

Any order consumed will be immediately countered with new order equals to the amount that consumed it. The new unit price is spaced statically by gridSpace, e.g. if a buy order with price X is consumed then a new sell order with price X + gridSpace will be created. 

####  `fullfixed`

The bot will counter only if the order is fully consumed. The new unit price is spaced statically by gridSpace. The counter amount will obey sellOrderQuantity and buyOrderQuantity set in config.

#### Full Percentage Rate `proportional`

In this mode both base quantity and unit price are spaced by percentage of the previous offerCreate level.
For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5
For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5


### Config

#### General configuration

`feeXRP` : *String*

Base fee in XRP (default "0.000012")

`datanet` : *String*

Historical Data API url. 

`emails` : *String*

Contact emails the bot will send reports to. 

Emails are sent from AWS SES. To use this feature you need to :
- Register the emails in SES Sandbox Mode. These emails will be used as both recipient and sender.
- Set up the SES credentials in your environment. You can put the credentials in ~/.aws/credentials or export them to environment variables.

#### Intervals (optional)

Intervals are the numbers of elapsed ledger validated events which will trigger following actions.

`balancer` : *int*

Balancer checks our orders and seeds missing orders according to bot configuration (default = 4)

`accountBalance` : *int*

Checks how much IOU the account has (default = 10)

#### Bot configuration

`pair` : *String* 

Currency or IOU with base.baseIssuer/quote.quoteIssuer format.

`startMiddlePrice` : *float*

Starting rate for seeder.

`gridSpace` : *float*

Price level between orders, behavior determined by strategy. 

`buyGridLevels` and `sellGridLevels` : *int*

Number of seed orders for buy and sell sides respectively. 

`buyOrderQuantity` and `sellOrderQuantity` : *float*

Amount of seed and counter order. This value is used for any strategy beside *partial*

`strategy` : *String*

Strategy to be used. Refer to strategy section for valid names. 


 

Version Log

01701 : sending XRP tx with form <val>/XRP/rrrrrrrrrrrrrrrrrrrrrhoLvTp will not work. It has to be done natively by the lib. 
03701 : 
OFFER EDITED 14FDEE8C8EA4408B39FEE0C3A43354B570ABF263939183EA7B67BA3F365AF71D to FDFEB58D863867A841741C02BE2986F66E026A9EEB2D0E5C279214BA13EE0D4B
Aug 23, 2017 3:35:26 PM com.mbcu.mmm.main.WebSocketClient$1 handleCallbackError
SEVERE: null
04001 :
tefMAX_LEDGER raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf 82DC1F52F51E697A32DF3E6D84FA78B4D5A48E65EA33C267A29CE6A857C5D0DE 30142
8 25, 2017 1:04:51 午前 com.mbcu.mmm.main.WebSocketClient$1 handleCallbackError
SEVERE: JSONObject["validated_ledgers"] not found.
04201 : Response showing edit although there was no edit, blank entry in history tab.
False edits:
61D3DC620E3BF2B36B936B8509BB68AB703041B1A80454E9AE39F0AE5F453CFE
E94C67ECAB3483015D545C96FD84DFDED6B2D789A1E6CEBD0B330D0855B3C315
Real edit:
7DF67EB046C88C49D8D2DFFF4B11339640D0ABFDF26182539E7712DD83530C84
04601
Unfunded offer arriving in stream:
{"engine_result":"tecUNFUNDED_OFFER","engine_result_code":103,"engine_result_message":"Insufficient balance to fund created offer.","ledger_hash":"1DDC59A766EE8C6EEACBBA7B08E967148FD2B86ED86AC8493F2785796CC06CCE","ledger_index":32729222,"meta":{"AffectedNodes":[{"ModifiedNode":{"FinalFields":{"Account":"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf","Balance":"1256217681","Flags":0,"OwnerCount":30,"Sequence":31392},"LedgerEntryType":"AccountRoot","LedgerIndex":"72A59CDF8FFBF65C20D01D3A3D5DA5BAE3158A3881E7AEE525A01B1CC73D32DD","PreviousFields":{"Balance":"1256217696","Sequence":31391},"PreviousTxnID":"450ACD8995147924A04AB3E10F07F252D0209754B12B4ACD7910387644BD2128","PreviousTxnLgrSeq":32729167}}],"TransactionIndex":5,"TransactionResult":"tecUNFUNDED_OFFER"},"status":"closed","transaction":{"Account":"raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf","Fee":"15","Flags":2147483648,"LastLedgerSequence":32729226,"Sequence":31391,"SigningPubKey":"02E2F1208D1715E18B0957FC819546FA7434B4A19EE38321932D2ED28FA090678E","TakerGets":{"currency":"DOG","issuer":"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS","value":"1060"},"TakerPays":"10000000","TransactionType":"OfferCreate","TxnSignature":"304402204A9C478A41102D1BECD3409E9F125FE091B4F4B597DA4965964C1CBCE61BC1EB022051D4FD1ACA89B8E4276FB7A843C0862B96C09254F023E1F2CD2E5EDB131A8F54","date":558603441,"hash":"3E528E50A60CF4E582BE70976D9177CB162E5CAFA493BAF391ACAC68C4DC54D4","owner_funds":"0"},"type":"transaction","validated":true}
05001
{
  "raw": "{\"fee_base\":10,\"fee_ref\":10,\"ledger_hash\":\"0A9103E0AE7724FA2E355A6B906306A97668A3A6955311C3261F7315148B6172\",\"ledger_index\":419711,\"ledger_time\":558842422,\"reserve_base\":20000000,\"reserve_inc\":5000000,\"txn_count\":0,\"type\":\"ledgerClosed\",\"validated_ledgers\":\"158188-419709,419711\"}\n"
}
object-info end
trace start
java.lang.NumberFormatException: For input string: "419709,419711"
	at java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)
	at java.lang.Integer.parseInt(Integer.java:580)
	at java.lang.Integer.parseInt(Integer.java:615)
	at com.mbcu.mmm.models.internal.LedgerEvent.fromJSON(LedgerEvent.java:28)
	at com.mbcu.mmm.sequences.Common$OnLedgerClosed.<init>(Common.java:583)
	at com.mbcu.mmm.sequences.Common.filterLedgerClosed(Common.java:96)
	at com.mbcu.mmm.sequences.Common.reroute(Common.java:90)
	at com.mbcu.mmm.sequences.Common.access$0(Common.java:82)
	at com.mbcu.mmm.sequences.Common$1.onNext(Common.java:59)
06001
Problem with XRP/ETC.MRR: too many orders
    	{
            "pair": "XRP/ETC.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS",
            "startMiddlePrice": 0.0127,
            "gridSpace": 1,
            "buyGridLevels": 2,
            "sellGridLevels": 2,
            "buyOrderQuantity": 2,
            "sellOrderQuantity": 1,
            "strategy" : "fullrateseedpct"      
        }
06008 : error_logs
