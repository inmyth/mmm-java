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


#### Partial `partial`

Any order consumed will be immediately countered with new order equals to the amount that consumed it. The new unit price is spaced statically by gridSpace, e.g. if a buy order with price X is consumed then a new sell order with price X + gridSpace will be created. 

####  `fullfixed`

The bot will counter only if the order is fully consumed. The new unit price is spaced statically by gridSpace. The counter amount will obey sellOrderQuantity and buyOrderQuantity set in config.

#### Proportional `ppt`

In this mode both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5


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


