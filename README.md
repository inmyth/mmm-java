# Java Ripple Market Maker

An automated program to market make on Ripple network with grid method. 

The bot prioritizes speed: any order will be pushed immediately to the ledger and retried if needed until it's in.

Requires [ripple-lib-java](https://github.com/ripple/ripple-lib-java ) to build. 


Principal elements:

**Parser**  : parses response from stream and grabs events of interest

**Counter** : counters each order consumed with a new order

**Balancer**: maintains the number of orders and their original information in orderbook

**State**   : keeps track of account sequence number which is used to send orders 


To use:
```
java -jar <the_build>.jar <path_to_config_file>
```

Documentations:
[Parsing pattern](docs/PARSING_PATTERN)
[Version log](docs/VER_LOG)


## Strategies

In general the principle of grid spacing is simple. Seed orderbook with orders spaced by price ("seed"). If an order is consumed, place a new order with a new rate calculated from the consumed rate ("counter"). As we want profit, when a buy order is consumed, sell it back at higher price, when a sell order is consumed, buy it back at lower price. Every a few ledgers, the bot will check the orderbook and add missing orders on either side. 

The IOU to trade, number of orders, grid space, amount, etc are defined in the config. The following strategies determine order countering:


#### Fixed Partial `partial`

Any order consumed will be immediately countered with new order equals to the amount that consumed it. The new unit price is spaced rigidly by gridSpace, e.g. if a buy order with price X is consumed then a new sell order selling the same quantity with price X + gridSpace will be created. 

#### Fixed Full `fullfixed`

The same as fixed partial but the bot will counter only if the order is fully consumed. 

#### Proportional `ppt`

In this mode both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5


#### Attention
The bot uses the initial offerCreate as reference for next order either as seed or counter. Preferably the bot should start when orderbook is empty. If not then it will assume any order in the orderbook as the original offerCreate. If you start the bot this way, *make sure partially filled orders in the orderbook are deleted* for they don't reflect their offerCreates.  

## Config

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

## When Error Happens

### Error by Rippled 

As a blockchain rippled does not guarantee each order sent to be included in the ledger. The bot will act depending response from the server. In general responses are categorized into :  
- Quasi-success: tesSUCCESS, ter_ (i.e. terQUEUED and terPRE_SEQ) are orders that have the chance to get into the ledger. If successful, server will send stream data about the order on the subscribed channel up to the order's maxledger. If maxledger is passed without any such data, the bot will resubmit the same order. 
- Immediate-fail : tefPAST_SEQ and errors indicating that the order was ok, but submitted with wrong sequence number or under a circumstance that otherwise would be fine at different time. Such order is immediately resubmitted after bot's sequence number is synchronized. 
- Fund-related fail : if the wallet no longer has enough fund to push an order then the order will be discarded. One dangerous error is terINSUF_FEE_B in which the bot no longer has enough XRP. The bot will not stop although no further order can be created. 

### Error by Network

Any network error and websocket disconnect will cause system exit with signal 1. A process control system like supervisord may catch this signal and restart the bot. 








