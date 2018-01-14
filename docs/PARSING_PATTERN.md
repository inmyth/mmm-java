# PARSER

Always use https://xrpcharts.ripple.com/#/transactions to get a clear description of a tx

The most important element in a response is probably DeletedNode. DeletedNode tells if an order was executed, modified, canceled due to OfferCancel or lack of fund. 

## DeletedNodes 
- For the bot, only consider offer with Account equals our address
- Only consider LedgerEntryType="Offer"

### Offer Executed
- our offer takes or is taken by other's offer. 
- DeletedNode has nested.PreviousFields

### Offer Edited
- modifying or editing an order will cancel it and replace it with a new one
- txn is offerCreate and has offerSequence from the old order
- DeletedNode contains old order

### Offer Canceled
- if canceled by OfferCreate then txn will have type OfferCreate
- orders may be automatically canceled if no longer funded. This happens when new order is created. In this case txn contains OfferCreate

## ModifiedNodes
- partially filled order goes here

## CreatedNode
- full order creation goes here

### Samples
- partial filled, unfunded E356B7AA6ADBB90CB4BA280AA1FF6E92E6054A76192E40980F95C118629B4E15, E8378AEBC3B1B78CE0AE8219B603DBD6A18420004B615981A99F125332FC3702
- executed : 95674414E0824878892D8B6C518F35CC5667D8B0700B90189C2A01886462DE71
- offercancel : 9641D3F019A5177F9AB93B09250A311D927CD44E3EC8296B3C2EECE0AE377589
- edit : 2227DA34FB27F500F5E04052569D9B345668817FC449FE7BE0E5627D41A26C06 
- offercreate, full : no deletednode
- offerCreate, own, fully consumes  : 33F4D4FA8564B75C5DD13FE4B32C961DBE6C2009FCE30BB4C18200B598B3E6F7 (no created node) 
- offercreate, partial : 7931DC82FB680321C5A3949EC8B81CEE088A7928BBC170BE5816C39EEE716AF7  (txn has original request, one created node has offer and the remaining amount that enters the orderbook) 
- offerCreate, other, fully consumed : 568F82829CE9EEE5223ABC22AC63DF8550FA2E1F18A990A880466401D5CF6FB9 


## RESPONSE
- response contains hash so it can be used as key. This hash should be stored in "deleted" node. 
- Final field in stream indicates executed offer.
-- hash -> DeletedNodes.FinalFields.PreviousTxnID (old hash), hash (new hash) 
- offer create for RJP/JPY may not have PreviousFields. This causes parse error for FinalFields. It looks creating new offer also cancels some tx which results in this situation. We can safely ignore it and use transaction information instead. 
- payment and OC belonging to others will result in many OEs. Find only those belonging for us
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
