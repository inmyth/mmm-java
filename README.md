##Ripple Market Making 

An automated program to market make on Ripple network.

Consists of:
Parser  : parses response from stream and listens to events of interest
Counter : counters each order in consumed with a new order
Balancer: maintains the number of orders in our orderbook 

How to use:
```
java -jar mmm.jar <path_to_config_file>
```
Config

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

```
quantity = orderQuantity - remainder
rate = botconfig seed rate
```
At 100 the bot will only counter an order if it's fully consumed.
At 40 the bot counters an order if the remaining order is less than 40% of original.
WARNING : when setting this parameter above 0, you should first cancel all orders in the orderbook.
  
v.016
- alpha remainder counter (counter if a percentage of original order is taken)
- tested on consume and consumed

v.015
- building whole counter and percentage of original order

v.014
- alpha. double orders are caused by resubmitting terPRE_SEQ.

v.013
-  bug1: Txc OnResponseFail was not matched against own hash or sequence. 
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
- improving getQuality (read notes on RJP/JPY) 

v.001
- added missing src/main/resources folder for Maven requirement
- added README.md

NOTES
- websocket offers high abstraction. tx sent is guaranteed to be tesSUCCESS and validated=true. 
- response contains hash so it can be used as key. This hash should be stored in "deleted" node. 
- Final field in stream indicates executed offer.
-- hash -> DeletedNodes.FinalFields.PreviousTxnID (old hash), hash (new hash) 
- tested sending 100 offers, all tesSUCCESS, validated=true
- offer create for RJP/JPY may not have PreviousFields. This causes parse error for FinalFields. It looks creating new offer also cancels some tx which results in this situation. We can saafel ignore it and use transaction information instead. 
- payment and OC belonging to others will result in many OEs. Find only those belonging for us
- do not instantiate Amount directly. Use RLOrder#amount
- pair in Config determines the unit of rate and gridSpace
- ter errors including terQUEUED and terPRE_SEQ behave like tesSUCCESS https://www.xrpchat.com/topic/2654-transaction-failed-with-terpre_seq-but-still-executed/?page=2

TODOS
- (done cancel-order.txt) see delete offer response 
- (offer-executed-bridged.txt) see executed offer response
- (done see notes/autobridge.txt) get response for autobridge
- (done) build parser to find previous tx by hash
- (canceled) parser returns currency pair, rate, quantity, and tx hash. 
- (done) filter returns onEdited, onCanceled, onNewOrder, onOffers
- the hash replaces previous hash (partial take, full take, cancel) or insert new (create new)
- partial take and reduces current offer in orderbook. Edit changes an order. Cancel and Full Take removes an order. 
- (done) autobridge tx should result in original rate. 
- our OEs should return FinalFields and hash they modify
- (done) OC should also return sequence
- (done) define a class to hold sequence, update and get it concurrently.
- (canceled, pointless) move all bus elements to the end of parser
- update hash for OC. It looks like OC we send doesn't have meta
- (done)define Counter class
- (fixed) v.009 value precision of 18 is greater than maximum iou precision of 16
- (v.011) test new counter on autobridge
- tefALREADY needs retrial ? 
- (done) build listener for remaining order after taken and original quantity and rate if it's fully taken
- list all OR in a list
- rearrange log
