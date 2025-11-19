# Decibel Java Submit Order Example

This project demonstrates how to submit orders to the Decibel DEX using Java and the [Japtos SDK](https://github.com/aptos-labs/japtos).

## Overview

This project includes two example applications:

1. **OrderExample**: Submit a single order to the APT-PERP market
2. **BulkOrderExample**: Interactive bulk order management bot that maintains a spread around a mid price

Both examples demonstrate:
- Account initialization (from private key or new generation)
- Automatic funding via Netna faucet
- USDC minting and deposit to subaccount
- Derived address calculation (subaccount, market)
- Transaction building and submission using the Japtos SDK

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- Access to the Decibel DEX deployment on Netna testnet

**Note:** No private key or pre-funded account required! The example automatically generates a new account and funds it.

## Project Structure

```
.
├── pom.xml                                    # Maven configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── com/decibel/
│       │       ├── OrderExample.java          # Single order example
│       │       ├── BulkOrderExample.java      # Bulk order bot example
│       │       ├── DecibelTransactions.java   # Transaction utilities
│       │       ├── DecibelUtils.java          # Address derivation & utils
│       │       └── InputUtils.java            # Config & account loading
│       └── resources/
│           └── config.properties.example      # Configuration template
└── README.md                                  # This file
```

## Setup

### 1. Install Dependencies

The project uses Maven to manage dependencies. The Japtos SDK will be automatically downloaded when you build the project.

### 2. Configure Your Account (Optional)

Copy the example configuration file:

```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```

Edit `config.properties` to customize:

```properties
# Aptos network configuration
aptos.fullnode.url=https://api.netna.staging.aptoslabs.com/v1
chain.id=204

# Decibel deployment package address
deployment.package=0x1234...

# Optional: Provide a private key (if not provided, a new account is generated)
private.key=0xYOUR_PRIVATE_KEY_HEX

# Trading API URL (for bulk orders)
trading.api.url=https://api.netna.aptoslabs.com/decibel
```

**Note:** If you don't provide a private key, a new account will be automatically generated and funded.

## Building

Compile and package the application:

```bash
mvn clean package
```

This creates a self-contained JAR file in `target/decibel-java-example-1.0-SNAPSHOT.jar`.

## Running

### Single Order Example

Submit a single order to the APT-PERP market:

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.decibel.OrderExample"

# Or using the JAR directly
java -cp target/decibel-java-example-1.0-SNAPSHOT.jar com.decibel.OrderExample
```

### Bulk Order Example

Run the interactive bulk order bot:

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.decibel.BulkOrderExample"

# Or using the JAR directly
java -cp target/decibel-java-example-1.0-SNAPSHOT.jar com.decibel.BulkOrderExample
```

The bulk order bot provides an interactive interface:
- Press `1` + ENTER to move all orders UP 1%
- Press `2` + ENTER to move all orders DOWN 1%
- Press `f` + ENTER to fund account (faucet + mint + deposit)
- Press `x` + ENTER to cancel orders and exit

## Expected Output

### Single Order Example

```
INFO  - Loaded account from private key: 0x123abc...
INFO  - Market: 0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36
INFO  - Subaccount: 0x789ghi...
INFO  - Order: BUY 100000 @ 10000000 (TIF: 2, ReduceOnly: false)
INFO  - Submitting transaction...
INFO  - Transaction submitted: 0xabc123...
INFO  - Transaction committed successfully!

✅ Single order submitted successfully!
Transaction Hash: 0xabc123...
View on explorer:
https://explorer.aptoslabs.com/txn/0xabc123...?network=decibel
```

### Bulk Order Example

```
🤖 Interactive Bulk Order Bot
==============================
Mid Price: $2.60
Spread: ±1% and ±2%

Press '1' + ENTER to move all orders UP 1% (↑)
Press '2' + ENTER to move all orders DOWN 1% (↓)
Press 'f' + ENTER to fund account (faucet + mint + deposit)
Press 'x' + ENTER to cancel orders and exit

Enter command (1/2/f/x): 1
↑ Moving UP to mid $2.63
✅ Orders updated | Tx: 0xdef456...

Enter command (1/2/f/x): x
🛑 Cancelling orders and stopping bot...
✅ Orders cancelled | Tx: 0x789abc...
```

## How It Works

### Account Initialization

The examples support two modes:

1. **Existing Account**: Load from `private.key` in `config.properties`
2. **New Account**: Generate a new Ed25519 account and automatically fund it

When generating a new account, the code:
- Creates a random Ed25519 account using `SecureRandom`
- Requests APT from the Netna faucet
- Mints 100 USDC to the account
- Deposits 50 USDC to the primary subaccount

### Bulk Order Bot

The `BulkOrderExample` maintains a spread around a mid price:
- Fetches the current bulk order sequence number from the trading API
- Places 2 bids (1% and 2% below mid) and 2 asks (1% and 2% above mid)
- Allows interactive adjustment of the mid price up/down by 1%
- Automatically cancels and replaces orders with each update
- Increments the sequence number to avoid conflicts

### Address Derivation

The example derives the primary subaccount address using the same logic as the TypeScript SDK:

**Primary Subaccount Address**: Derived from the user's account address using the seed `"decibel_dex_primary"` with SHA3-256 hashing and the Aptos object address marker (`0xFE`).

The market address for APT_USDC is configured directly: `0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36`

### Transaction Functions

The examples use several Move entry functions:

**Single Order:**
```
<package>::dex_accounts::place_order_to_subaccount(
    subaccount_addr: address,
    market_addr: address,
    price: u64,
    size: u64,
    is_buy: bool,
    time_in_force: u8,
    is_reduce_only: bool,
    client_order_id: Option<u64>,
    stop_price: Option<u64>,
    tp_trigger_price: Option<u64>,
    tp_limit_price: Option<u64>,
    sl_trigger_price: Option<u64>,
    sl_limit_price: Option<u64>,
    builder_addr: Option<address>,
    builder_fee: Option<u64>
)
```

**Bulk Orders:**
```
<package>::dex_accounts::place_bulk_order_to_subaccount(
    market_addr: address,
    sequence_num: u64,
    bid_prices: vector<u64>,
    bid_sizes: vector<u64>,
    ask_prices: vector<u64>,
    ask_sizes: vector<u64>
)
```

**Cancel Bulk Orders:**
```
<package>::dex_accounts::cancel_bulk_order_to_subaccount(
    market_addr: address,
    sequence_num: u64
)
```

## Japtos SDK

This project uses the [Japtos SDK](https://github.com/aptos-labs/japtos) which provides:

- **Ed25519 Cryptography**: Account creation and transaction signing
- **Transaction Building**: Raw transaction construction and BCS serialization
- **HTTP Client**: Interaction with Aptos fullnode APIs
- **Type Safety**: Strongly-typed transaction arguments and payloads

## Network Configuration

By default, the example connects to the Netna testnet. To use a different network:

1. Update `aptos.fullnode.url` in `config.properties`
2. Update `chain.id` to match the target network
3. Update `deployment.package` if using a different contract deployment

### Available Networks

- **Netna (default)**: `https://api.netna.staging.aptoslabs.com/v1` (Chain ID: 204)
- **Local**: `http://localhost:8080/v1` (use `LOCAL_DEPLOYMENT` addresses)
- **Mainnet**: Configure accordingly (coming soon)

## Troubleshooting

### "Faucet request failed"

The Netna faucet might be rate-limited or temporarily unavailable. Wait a few minutes and try again.

### "Insufficient USDC balance"

The example mints 100 USDC and deposits 50 to the subaccount. If you get balance errors, check that the minting and deposit transactions completed successfully.

### "Transaction simulation failed"

This usually means the market doesn't exist or the order parameters are invalid. Make sure you're using the correct market address in `config.properties`.

### "SHA3-256 algorithm not available"

The Java runtime should include SHA3-256 support by default in Java 9+. If you're using an older version, you may need to add a cryptography provider.

## Related Resources

- [Decibel DEX Documentation](../../README.md)
- [Japtos SDK GitHub](https://github.com/aptos-labs/japtos)
- [Aptos Documentation](https://aptos.dev)
- [TypeScript SDK Reference](../typescript/packages/sdk/)

## License

This example is part of the Decibel DEX project. See the main repository for license information.
