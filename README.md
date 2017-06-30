##Ripple Market Making 


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
- build parser to find previous tx by hash
- (canceled) parser returns currency pair, rate, quantity, and tx hash. 
- filter returns onEdited, onCanceled, onNewOrder, onOffersConsumed
- the hash replaces previous hash (partial take, full take, cancel) or insert new (create new)
- partial take and reduces current offer in orderbook. Edit changes an order. Cancel and Full Take removes an order. 
- autobridge tx should result in original rate. (Autobridge guarantees that the rate on orderbook will never change)
