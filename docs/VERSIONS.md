## Version History

v.071
- fixed partial countering on full strategies
- some logic in deletednode (unfunded cancel, etc) was affected regardless of tx.account

v.07
- Common#filterStream2 all deletedNodes and modifiedNodes are first saved. If txn.account == myAddress then all these are significant (canceled offer due to lack of fund), if txn.account != myAddress, filter out offers which don't belong to myAddress 

v.0613 pre-merged, summary
- parser bug fixed (deletednode, creatednode, modifiednode)
- strategies rearranged
- network or websocket error will do system exit with error signal  
- RLORder.fromOfferCreate

v.0612
- fullfixed refactored, tested
- ppt tested 

v.0611
- working on fullfixed and partial
- RLORder.fromOfferCreate(Transaction txn) seemed to create wrong direction. I flipped it and also flipped yukiPct. 

v.0610
- websocket or network problem will cause system.exit(1) . This way supervisord will catch it and restart the bot. 

v.0609
- found errors in how DeletedNode was interpreted. DeletedNodes may contain orders which are canceled after they became unfunded. These orders are not OfferCancel. This is the cause of orders not removed from orderbook and therefore causing a lot of doubles (06001 to 06008). 
- amended 0608. No automatic edit event changes direction. This is side effect from the above error. 

v.0608
- edit can change the direction (buy <-> sell) (false)
- dangling order didn't get removed (log.missed_remove_order.txt.0)

v.0607
- partial consume removes the order from orderbook

v.0606
- both ways work
- testing error : log.rhXCRgFH7xdCfDBrpQSjUTfCnoiSUWeTMc.2017_12_27_13_22_45_997.txt 5538 -> 9565
- edit may not find sequence so obsolete sequence is left dangling in orderbook becoming anchor for seed. 
- attempt fix : edit on both sides everytime 

v.0605
- fixed orderbook seed generator (tested on both ways)

v.0604
- total1 = sqrt * q0 / u1 test this

v.0603
- buy is correct
- sell not correct
- both active not correct

v.0602
- sell and buy 80%
- every sellGridlevels there's a jump in price 

v.0601
- moving to pure pct strategy
- buy seed, counter 70% ok, check newrate when buys is not empty
- test RLOrder 454
- do the same for sell

v.060
- (06001) fixed fullrateseedpct counter
- streamed counter

v.059
- all trade settings should be customizable in botconfig
- describe them in README
- fixed partial counter
- change of config


v.058
- Seed amount should be changed by percentage too
- log if one side or both sides of orderbook are empty
- streamed pctseed

v.057
- fixed percentage mode on counters. 

v.056
- fixed percentage mode. Counter order is created from percentage of the rate consumed. 
- added log to drop below zero in buy seed and counter. 

v.055
- added seed by percentage gridspace
- balancer not adding missing orders ? 

v.054
- see in progress tasks
- Befaf now holds txnId for debugging. To be continued after txnId is captured for division by Zero.
- fixed precision for XRP too small error

v.053
- fixed account_offers for more than 200 orders

v.052
- fixed email sender. Supervisor runs on sudo so need to place credentials on the same level. 

v.051
- attempt to add maxFee by adding it in Transaction failed

v.050
- missing order error fix. Txs were inserted with the same sequence number (happens if there's a sequence refresh and counter). Now submission checks if seq exists.   
- removed seed in between orders.
- fixed double order issue. When one side in orderbook was empty, balancer would seed new orders from midPrice. This caused counters to fill already filled price level on the other side. Now when one side is empty, seed it from the best price of the other side with one gridlevel gap. 


v.049
- Account balance is sent periodically to emails.

v.048
- Attach wallet address on log

v.047
- Added non-tesSuccess handler in stream causing false OfferCreate. TecUnfunded and other errors will not register as OfferCreate anymore. 
- Added warning in Orderbook#generate. Order explosion should only come from this function. 

v.046
- Account balance check. 
- Change in `config.txt` (intervals, datanet)

v.045
- Email notification. Change in `config.txt`

v.044
- Fixed 04201. Possibly blank entry in wallet page. To avoid this, Common now checks if final node contains Account or not. False edit doesn't have account and has HighLimit, LowLimit although it has prevTxnId

v.043
v.042

v.041
- fixed 04101
- unwrap exceptions in all Observables

v.040
- removed generate seed from midprice
- error severe -1 (forgot to start balancer generate from 0 as I no longer seed from spread)

v.039
- balancer generate now seeds from startmiddle and between orders. 
- probably need to remove generate seed from middleprice (may end up with lots of counter orders attacking the other side).

v.038
- fixed 03701

v.037
- commented out balancer trim
- commented out orderbook edit (bug)

v.036
- fixed orderbook worst rates

v.035
- balancer interval changed to 4

v.034
- balancer sell fixed

v.033
- balancer tested
 
v.032
- balancer generate completed

v.031
- balancer trim completed
- changed String pair to Cpair class

v.030
- balancer waits until state is clear of pending tx. 

v.029
- Orderbook sum modified for sells. Need to check

v.028
- Made MaxLedger to call sequence sync, otherwise it will loop into terPreSeqs. 

v.027
- Made ter to behave like tesSuccess

v.026 
- Cleaned up post TakerGets error. 
- Disabled Balancer

v.025 
- Fixed no TakerGets error. This offer should be ignored https://github.com/ripple/rippled/issues/2199

v.024
- Orderbook can be built from stream events, tested on Order canceled, edited, and difference

v.023
- testing OnDifference

v.022
- whole order counter is adjusted with gridSpace
- normalized partial and whole counters

v.021
- orderbook on initiation

v.020
- balancer from orderbook start
- deactivated seed

v.019
- alpha replace order
- added isReplaceMode in bot config. 
- cleaned up replacement counter

v.018 
- fixed the remainder counter but partial counter for this strategy is not logical because at the same rate it cancels the previous order.

v.017
- fixed bug in tx submission. XRP should be sent as native without issuer  
- limited log files at 20MB with 20 rotations

v.016
- alpha remainder counter (counter if a percentage of original order is taken)
- tested on consume and consumed

v.015
- building whole counter and percentage of original order

v.014
- alpha. double orders are caused by resubmitting terPRE_SEQ.

v.013
- bug1: Txc OnResponseFail was not matched against own hash or sequence. 
- removed synchronized in State
- need to test on preSeq or pastSeq 

v.012
- pre-alpha State. This would be equal to mmm in node.js
- State manages order submission and trial

v.011
- tested on Arbitrage without autobridge.
- fixed some calculation errors.

v.010
- privatized NamePair
- used Observer on all sequences (intercept error)

v.009
- pre-alpha counter
- removed RLAmount.

v.008 
- debugging counter

v.007
- finalizing alpha counter

v.006
- completed parser for counter.

v.005
- parser now parsed Autobridge correctly. 

v.004
- improved parser so OE mimic history text response on wallet app

v.003
- alpha version of parser

v.002
- improving getQuality

v.001
- added missing src/main/resources folder for Maven requirement
- added README.md



### TODOS
- [x]  (done cancel-order.txt) see delete offer response 
- (offer-executed-bridged.txt) see executed offer response
- [x] get response for autobridge
- [x] (done) build parser to find previous tx by hash
- (canceled) parser returns currency pair, rate, quantity, and tx hash. 
- [x] (done) filter returns onEdited, onCanceled, onNewOrder, onOffers
- the hash replaces previous hash (partial take, full take, cancel) or insert new (create new)
- partial take and reduces current offer in orderbook. Edit changes an order. Cancel and Full Take removes an order. 
- [x] (done) autobridge tx should result in original rate. 
- our OEs should return FinalFields and hash they modify
- [x] (done) OC should also return sequence
- [x] (done) define a class to hold sequence, update and get it concurrently.
- (canceled, pointless) move all bus elements to the end of parser
- update hash for OC. It looks like OC we send doesn't have meta
- [x] (done)define Counter class
- [x] (fixed) v.009 value precision of 18 is greater than maximum iou precision of 16
- (v.011) test new counter on autobridge
- tefALREADY needs retry ? 
- [x] (done) build listener for remaining order after taken and original quantity and rate if it's fully taken
- [x] (done) list all OR in a list
- rearrange log
- [x] fix the remainder counter Check BeAf
- [x] partial remainder counter is not logical. If the partial amount is countered with the same rate then the previous order will be canceled. 
- (canceled) continue Txc so it disposes old disposable and turn it into orderbook item
- [x] get Amount from account_offers result
- [x] bus for account_offers should also have currency pair
- [x] intercept pairs on orderbook or balancer level
- [x] sort RLOrder by rate
- [x] write orderbook to files
- [x] BefAf needs sequence
- [x] Orderbook test onDelete
- [x] Orderbook test onEdit
- [x] Orderbook test onRemainder
- [x] get RLORder from State#pending and State#qWait for Orderbook balancer, synchronously
- [x] balancer can sum all orders, compare with settings
- [x] remove if sum is more than config
- [x] create if sum is less than config
- [x] cancel order mechanism
- [x] edit updates sequence
- [x] cancel updates sequence
- [x] compare orderbook sum with setting's sum
- [x] (bug) no TaketGets in PreviousFields  . Create null checker to ignore such offer.
- [x] ter in Txc sets isTesSuccess true because they have the same behavior.
- [x] as ter behaves like tesSUCCESS then MaxLedger passed should refresh sequence, otherwise it will go into terPastSeq loop. 
- [x] orderbook sum doesn't return the right sum
- [x] Common needs to pass onCreate id from our address only
- [x] Check anything affected by String pair
- [x] (bug) Orderbook buys is empty. 
- [x] check balancer generate for sells and both
- [x] turn off seed on balancer
- [x] save worst rates for every orderbook
- [x] any reseed should start from these prices
- [x] (bug) some canceled txs are resubmitted in infinity.
- [x] (bug) worst rate calculation must be done separately for buys and sells otherwise the rate will clock up when one of them is empty.
- [x] (bug) see 03701 sequence wasn't set in Common@271
- [x] find a way to unwrap exception
- [x] may need to cancel generate seed from middleprice
- [] bot creates buy orders down to 0 price
- [x] unknown edit (B078019B57E783C3467419BB4C6ED93770A60CB766ACF6A00A2AABF88D3BE98E). This is rolled into new OfferExecuted logic. 
- [x] add warning email system in case of any unhandled errors
- [x] automated balance check
- [x] error may arrive in stream not response. Handle this.
- [x] log files should be named per account
- [x] account balance sends email periodically
- [x] need to trace all retry tx
- [x] missing order error, check response_sample/missing_order.txt
- [x] log files should be named per start ts
- [x] 05001 ledger number contains holes
- [x] need to know if offerCreate comes from seed or counter
- ~~[] get reference market price~~ 
- [x] balancer seeder skips a rate if all order consumed. If the gap = 2*levels then it's correct. 
- [x] double orders on same price. when restarted, orders will start from startMiddle when empty. This will cause double orders on the other side
- ~~[] need maxFee.~~
- [x] move fee to config
- [x] test email sender with credentials in env. 
- [x] fix division by zero
- [x] capture txnId for BefAf
- [x] fix XRP too small
- [x] fix account_offers for more than 200 orders
- [x] add percentage counter 
- [] fix static counter
- [x] test counter. the range between buys and sells != gridspace
- [x] fix 06001
- [] build tx response from rpc tool to json interpreted by Common
- [x] wrong balancer post-delete
- [x] offercancel still results in unfunded offer message
- [] rearrange readme
- []  websocketclient exception check


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
