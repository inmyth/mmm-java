# PARSER

Always use https://xrpcharts.ripple.com/#/transactions to get a clear description of a tx

The most important element in a response is probably DeletedNode. DeletedNode tells if an order was executed, modified, canceled due to OfferCancel or lack of fund. 

### The Summary
Any request for transaction history either by websocket's account_tx or Data Api's /transactions command will return all transactions involving owner's account. For the sake of perspective owner will be referred to as us. 

Basically a typical transaction response consists of **tx** and **meta**. 

tx consists of original [transactions](https://ripple.com/build/transactions/) request.

meta consists of information of how this transaction affects our and other people's orders, balance, and also blockchain's ledger. 

Since the focus of this bot project is trade, the breakdown is this:

1. The only transactions affecting orders are : OfferCreate, OfferCancel, and Payment.

2. If we are the creator of offer then our address will be in tx.Account and tx.TransactionType will be OfferCreate or OfferCancel. 
All transactions that happen in meta are important to us. Other people's transactions that are consumed by our offer may appear in DeletedNodes as fully-filled order or ModifiedNodes as partially-filled order. A meta CreatedNode that contains our account is the order appears in the ledger. If our account shows up in DeletedNode some of our other orders have become unfunded by this new order and are automatically canceled. 

3. If we are not the creator of the transaction then tx.Account does not have our address. We will need to pay attention to tx.TransactionType equals OfferCreate and Payment. In meta, only transactions (in DeletedNodes or ModifiedNodes) with our address are important to us. These are our orders which were consumed. Transactions not with our address affect other people's orders and we don't care about those. 

## DeletedNodes 
- Only pay attention to node with LedgerEntryType="Offer"
- if tx.Account == our Address, all nodes are important. If tx.Account != our address, only consider the ones with our address. 

### Offer Executed
- DeletedNode has nested.PreviousFields
- if tx.Account == our Address, all DeletedNodes with other people's address are their filled orders. If it has our address read the part about "Offer Canceled Not By OfferCancel". 
If tx.Account != our address, then all DeletedNodes with our address are our filled orders. 
- check also ModifiedNodes for partially filled orders with the same rules.  

### Offer Modified
- modifying (or editing) an order will cancel and replace it with a new one
- tx.TransactionType is offerCreate and has key "OfferSequence" containing the old order's sequence
- DeletedNode contains the old pre-modified offer

### Offer Canceled Not By OfferCancel
- existing orders may be automatically canceled if no longer funded. This may happen when a new offer is created.
- in this particular case tx.TransactionType has OfferCreate, not OfferCancel

## ModifiedNodes
- contain partially filled orders
- contain changes in balance

## CreatedNode
-  contains created order which appears in the ledger (or orderbook). The amount will be equal to the original offerCreate or equal to the left over after it consumes some orders in the orderbook. 

### Samples
- partial filled, unfunded E356B7AA6ADBB90CB4BA280AA1FF6E92E6054A76192E40980F95C118629B4E15, E8378AEBC3B1B78CE0AE8219B603DBD6A18420004B615981A99F125332FC3702
- executed : 95674414E0824878892D8B6C518F35CC5667D8B0700B90189C2A01886462DE71
- offercancel : 9641D3F019A5177F9AB93B09250A311D927CD44E3EC8296B3C2EECE0AE377589
- edit : 2227DA34FB27F500F5E04052569D9B345668817FC449FE7BE0E5627D41A26C06 
- offercreate, full : no deletednode
- offerCreate, own, fully consumes  : 33F4D4FA8564B75C5DD13FE4B32C961DBE6C2009FCE30BB4C18200B598B3E6F7 (no created node) 
- offercreate, partial : 7931DC82FB680321C5A3949EC8B81CEE088A7928BBC170BE5816C39EEE716AF7  (txn has original request, one created node has offer and the remaining amount that enters the orderbook) 
- offerCreate, other, fully consumed : 568F82829CE9EEE5223ABC22AC63DF8550FA2E1F18A990A880466401D5CF6FB9 




# RANDOM NOTES

## RESPONSE
- response contains hash so it can be used as key. This hash should be stored in "deleted" node. 
- Final field in stream indicates executed offer.
-- hash -> DeletedNodes.FinalFields.PreviousTxnID (old hash), hash (new hash) 
- offer create for RJP/JPY may not have PreviousFields. This causes parse error for FinalFields. It looks creating new offer also cancels some tx which results in this situation. We can safely ignore it and use transaction information instead. 
- payment and OC belonging to others will result in many OEs. Find only those belonging to us
- do not instantiate Amount directly. Use RLOrder#amount
- pair in Config determines the unit of rate and gridSpace
- ter errors including terQUEUED and terPRE_SEQ behave like tesSUCCESS https://www.xrpchat.com/topic/2654-transaction-failed-with-terpre_seq-but-still-executed/?page=2
- any error in the code is wrapped in WebSocketClient#handleCallbackError . This can be parsing error (unhandled response type)
- sending counter order that crosses the rate of previous remaining order will cancel it. The counter will prevail. 


## ORDERS
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

## Autobridge
- autobridge rate (quality) never changes
- autobridge currency is XRP
- created rate and quantity may not be the same as create offer rate and quantity
- ~~Response must have two OEs each one against XRP.~~
- Response can have many OEs in either direction but the sum of XRP of them all must be equal.
- ~~Real rate = both of OEs' rates multiplied~~
- Real quantity = look at OE's non-XRP quantity
- OEs have different accounts than ours
- OEs' PreviousTxnID are not the same as OC's hash
- Ignore OC's with other's account. It might consume our order. 
- If order is not fully matched then autobridge tx will produce extra tx to sell or buy bridge currency (XRP). 
- Rate = sum of all principal quantities / sum of all principal's counter quantities.

tecUNFUNDED_OFFER
- happens on non-XRP currency 
- happens when the balance is 0
 
## Editing Order
- basically sending OfferCreate which has OfferSequence of the offer edited

## Balancer
- if both sides in orderbook are empty, seed from midPrice
- if order level is lower than config, seed more orders toward worse rate (buy -> cheaper rate, sell -> higher rate)
- if one side is empty, seed it with rate from the best price of the other side


## Others
- Sequence increases when we have OrderCreate, not necessarily OrderCreated
- Any FinalFields with prices zero means order fully consumed.
- Payment doesn't have our autobridge. But it has other people's autobridge with our order in it. 
- OC belonging to other don't have our autobridge either. See payment. 
- hash also appears in executedoffer so we can trace it back to orderbook
- if our consuming orders leaves a remainder and is consumed then it is treated like our order is consumed
- total consumed doesn't raise sequence
- Payments can have multiple same account OEs when it consumes.
- OE contains previousTxnId  
- It's possible to create new OC with more funds than the account has
