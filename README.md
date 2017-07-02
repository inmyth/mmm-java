##Ripple Market Making 

An automated program to market make on Ripple network.

Consists of:
Parser  : parses response from stream and listens to events of interest
Counter : counters each order in consumed with a new order
Balancer: maintains the number of orders in our orderbook 

How to use:
```
java -jar mmm.jar <path_to_config>
```

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


TODOS
- (done cancel-order.txt) see delete offer response 
- (offer-executed-bridged.txt) see executed offer response
- (done see notes/autobridge.txt) get response for autobridge
- (dpne) build parser to find previous tx by hash
- (canceled) parser returns currency pair, rate, quantity, and tx hash. 
- (done) filter returns onEdited, onCanceled, onNewOrder, onOffers
- the hash replaces previous hash (partial take, full take, cancel) or insert new (create new)
- partial take and reduces current offer in orderbook. Edit changes an order. Cancel and Full Take removes an order. 
- (done) autobridge tx should result in original rate. (Autobridge guarantees that the rate on orderbook will never change)
- payment and OC belonging to others will result in many OEs. Find only those belongong for us
- our OEs should return FinalFields and hash they modify
- OC should also return sequence
- define a class to hold sequence, update and get it concurrently.