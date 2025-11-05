# Decibel Java Submit Order Example

This project demonstrates how to submit orders to the Decibel DEX using Java and the [Japtos SDK](https://github.com/aptos-labs/japtos).

## Overview

The example mirrors the TypeScript SDK's `placeOrder` functionality and e2e test setup, showing how to:
- Generate a new random Ed25519 account
- Fund the account with APT from the Netna faucet
- Mint USDC tokens to the account
- Deposit USDC to the primary subaccount
- Calculate derived addresses (subaccount, market)
- Build and submit a `place_order_to_subaccount` transaction to the APT_USDC market
- Wait for transaction confirmation

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- Access to the Decibel DEX deployment on Netna testnet

**Note:** No private key or pre-funded account required! The example automatically generates a new account and funds it.

## Project Structure

```
java/
├── pom.xml                                    # Maven configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── com/decibel/example/
│       │       └── SubmitOrder.java          # Main application
│       └── resources/
│           └── config.properties              # Configuration file
└── README.md                                  # This file
```

## Setup

### 1. Install Dependencies

The project uses Maven to manage dependencies. The Japtos SDK will be automatically downloaded when you build the project.

### 2. Adjust Order Parameters (Optional)

The example uses sensible defaults, but you can customize the order parameters by editing `config.properties`:

```properties
# APT_USDC market address on Netna
order.market.address=0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36

# Price in chain units with 6 decimals (default: 10000000 = $10)
order.price=10000000

# Size in chain units with 6 decimals (default: 100000 = 0.1 APT)
order.size=100000

# Buy (true) or Sell (false) - default: true
order.is.buy=true

# Time in force (default: 2=ImmediateOrCancel)
# 0=GoodTillCanceled, 1=PostOnly, 2=ImmediateOrCancel
order.time.in.force=2

# Reduce only order (default: false)
order.is.reduce.only=false
```

## Building

Compile and package the application:

```bash
cd java/
mvn clean package
```

This creates a self-contained JAR file in `target/decibel-java-example-1.0-SNAPSHOT.jar`.

## Running

### Using Maven

```bash
mvn exec:java -Dexec.mainClass="com.decibel.example.SubmitOrder"
```

### Using the JAR directly

```bash
java -jar target/decibel-java-example-1.0-SNAPSHOT.jar
```

## Expected Output

When successful, you should see output similar to:

```
INFO  - Connected to Aptos node: https://api.netna.staging.aptoslabs.com/v1
INFO  - Generated new account: 0x123abc...
INFO  - Requesting APT from faucet...
INFO  - ✅ Account funded with APT
INFO  - Minting 100.0 USDC...
INFO  - ✅ USDC minted
INFO  - Depositing 50.0 USDC to subaccount: 0x789ghi...
INFO  - ✅ USDC deposited to subaccount
INFO  - Market: 0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36
INFO  - Subaccount: 0x789ghi...
INFO  - Order: BUY 100000 @ 10000000 (TIF: 2, ReduceOnly: false)
INFO  - Submitting transaction...
INFO  - Transaction submitted: 0xabc123...
INFO  - Waiting for transaction to be committed...
INFO  - Transaction committed successfully!

✅ Order submitted successfully!
Transaction Hash: 0xabc123...

View on explorer:
https://explorer.aptoslabs.com/txn/0xabc123...?network=custom
```

## How It Works

### Automatic Setup

The example automatically sets up a complete testing environment:

1. **Account Generation**: Creates a new random Ed25519 account using `SecureRandom`
2. **APT Funding**: Requests 10 APT from the Netna faucet via HTTP POST
3. **USDC Minting**: Calls `package::usdc::mint` to mint 100 USDC to the account
4. **Subaccount Deposit**: Calls `package::dex_accounts::deposit_to_subaccount` to deposit 50 USDC

This mirrors the setup used in the TypeScript e2e tests.

### Address Derivation

The example derives the primary subaccount address using the same logic as the TypeScript SDK:

**Primary Subaccount Address**: Derived from the user's account address using the seed `"decibel_dex_primary"` with SHA3-256 hashing and the Aptos object address marker (`0xFE`).

The market address for APT_USDC is configured directly: `0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36`

### Transaction Structure

The transaction calls the entry function:

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

Optional parameters (stop price, take profit, stop loss, builder info) are set to `None` in this example.

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
