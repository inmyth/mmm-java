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