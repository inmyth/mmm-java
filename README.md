# Java Ripple Market Maker

An automated program to market make on Ripple network.

Consists of:

**Parser**  : parses response from stream and listens to events of interest

**Counter** : counters each order in consumed with a new order

**Balancer**: maintains the number of orders in our orderbook 

How to use:
```
java -jar mmm.jar <path_to_config_file>
```
**Config**

`pair` : String 

Currency or IOU with base.baseIssuer/quote.quoteIssuer format.

`startMiddlePrice` : float

Starting rate for seeder.

`gridSpace` : float

Margin between seed and counter orders. 

`buyGridLevels` and `sellGridLevels` : int

Number of seed orders for each side.

`buyOrderQuantity` and `sellOrderQuantity` : float

Amount of seed or counter order.

`percentToCounter` : int (from 0 to 100)

Percentage of order consumed before it is countered. 
At 0 the bot will counter every consumed order regardless of size with rates of the consumed orders. 
If not zero then all orders will be countered with seed rate from botconfig. 

> quantity = orderQuantity - remainder

> rate = botconfig seed rate

At 100 the bot will only counter an order if it's fully consumed.
At 40 the bot counters an order if the remaining order is less than 40% of original.

**WARNING** : when setting this parameter above 0, you should first cancel all orders in the orderbook.

**Version History** 

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



TODOS
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
- tefALREADY needs retrial ? 
- [x] (done) build listener for remaining order after taken and original quantity and rate if it's fully taken
- [x] (done) list all OR in a list
- rearrange log
- [] fix the remainder counter Check BeAf
- [] partial remainder counter is not logical. If the partial amount is countered with the same rate then the previous order will
be canceled. 


## NOTES
RESPONSE
- (FALSE !) websocket offers high abstraction. tx sent is guaranteed to be tesSUCCESS and validated=true. This is because I set the fees at 12 XRP not 12 drops !!!
- response contains hash so it can be used as key. This hash should be stored in "deleted" node. 
- Final field in stream indicates executed offer.
-- hash -> DeletedNodes.FinalFields.PreviousTxnID (old hash), hash (new hash) 
- offer create for RJP/JPY may not have PreviousFields. This causes parse error for FinalFields. It looks creating new offer also cancels some tx which results in this situation. We can saafel ignore it and use transaction information instead. 
- payment and OC belonging to others will result in many OEs. Find only those belonging for us
- do not instantiate Amount directly. Use RLOrder#amount
- pair in Config determines the unit of rate and gridSpace
- ter errors including terQUEUED and terPRE_SEQ behave like tesSUCCESS https://www.xrpchat.com/topic/2654-transaction-failed-with-terpre_seq-but-still-executed/?page=2
- any error in the code is wrapped in WebSocketClient#handleCallbackError . This can be parsing error (unhandled response type)
- sending counter order that crosses the rate of previous remaining order will cancel it. The counter will prevail. 


ORDERS
When our order consumes
- OC belongs to us
- OE belongs to others. 
- If the order is fully consumed in the process then no order is created
- If the order is partially consumed in the process then new order with remainder as quantity is created
- Sequence always increases
- OE with other account and FinalFields.TakerPays and TakerGets == 0 means their order is fully consumed, but we don't care
- Counter : OE belonging to others

When our order is consumed
- OC belongs to others. Can be payment which belongs to others. 
- When consumed no new order is created
- Sequence does not change if taken or partially taken. Only our first OC adds the sequence. 
- Counter : OE with our account
- OE with our account and FinalFields.TakerPays and TakerGets == 0 means fully consumed

Autobridge
- autobridge rate (quality) never changes
- autobridge currency is XRP
- created rate and quantity may not be the same as create offer rate and quantity
- (false) Response must have two OEs each one against XRP. 
(true) Response can have many OEs in either direction but the sum of XRP of them all must be equal.
- (false) Real rate = both of OEs' rates multiplied
- Real quantity = look at OE's non-XRP quantity
- OEs have different accounts than ours
- OEs' PreviousTxnID are not the same as OC's hash
- Ignore OC's with other's account. It might consume our order. 
- If order is not fully matched then autobridge tx will produce extra tx to sell or buy bridge currency (XRP). 
- Rate = sum of all principal quantities / sum of all principal's counter quantities. 

Summary
- Sequence increases when we have OrderCreate, not necessarily OrderCreated
- Any FinalFields with prices zero means order fully consumed.
- Payment doesn't have our autobridge. But it has other people's autobridge with our order in it. 
- OC belogning to other don't have our autobridge either. See payment. 
- hash also appears in executedoffer so we can trace it back to orderbook
- if our consuming orders leaves a remainder and is consumed then it is treated like our order is consumed
- total consumed doesn't raise sequence
- Payments can have multiple same account OEs when it consumes.
- OE contains previousTxnId  
- It's possible to create new OC with more funds than the account has

Version Log

01701 : sending XRP tx with form <val>/XRP/rrrrrrrrrrrrrrrrrrrrrhoLvTp will not work. It has to be done natively by the lib. 
