# Fee Payer Pool

The Fee Payer Pool is a system for managing multiple fee payer accounts to distribute transaction fees across a pool of accounts, enabling high-throughput parallel transaction submission.

## Overview

The `FeePayerPool` class:
- Loads multiple fee payer accounts from an array of private keys
- Provides round-robin selection for transaction fee payment
- Enables parallel transaction submission without sequence number conflicts
- Thread-safe for concurrent use

## Configuration

Add these settings to your `config.properties`:

```properties
# Comma-separated list of fee payer private keys
fee_payer.private_keys=0x1234...,0x5678...,0x9abc...
```

**Note:** Fee payer accounts must be pre-funded with sufficient APT for gas fees.

## Usage

### Basic Setup

```java
import com.decibel.FeePayerPool;

// Initialize from an array of private keys
String[] feePayerPrivateKeys = {
    "0x1234567890abcdef...",
    "0x234567890abcdef1...",
    "0x34567890abcdef12..."
};

FeePayerPool feePayerPool = new FeePayerPool(client, feePayerPrivateKeys);
```

### Submitting Transactions

```java
// Get a random fee payer from the pool
Ed25519Account feePayer = feePayerPool.getNextFeePayer();

// Or get a specific fee payer by index
Ed25519Account feePayer0 = feePayerPool.getFeePayer(0);

// Submit transaction with automatic fee payer selection
String txHash = feePayerPool.submitWithFeePayer(senderAccount, rawTx);
```

## How It Works

### Account Loading

Fee payer accounts are loaded from an array of private key hex strings:

```java
for (String privateKeyHex : feePayerPrivateKeys) {
    // Remove 0x prefix if present
    if (privateKeyHex.startsWith("0x")) {
        privateKeyHex = privateKeyHex.substring(2);
    }
    
    // Convert to bytes and create account
    byte[] privateKeyBytes = hexToBytes(privateKeyHex);
    Ed25519Account feePayer = Ed25519Account.fromPrivateKey(privateKeyBytes);
    feePayers.add(feePayer);
}
```

**Important:** You must pre-fund these accounts with sufficient APT before using them as fee payers.

### Random Selection

The pool uses random selection for thread-safe fee payer distribution:
```java
int index = random.nextInt(feePayers.size());
return feePayers.get(index);
```

This distributes load randomly across all fee payers, minimizing contention.

## Example: Bulk Order Bot

The `BulkOrderExample` demonstrates fee payer pool usage:

```java
public class BulkOrderExample {
    private final FeePayerPool feePayerPool;
    
    public BulkOrderExample() throws Exception {
        // Load fee payer private keys from config
        String feePayerKeysStr = config.getProperty("fee_payer.private_keys");
        String[] feePayerPrivateKeys = feePayerKeysStr.split(",");
        
        // Trim whitespace from each key
        for (int i = 0; i < feePayerPrivateKeys.length; i++) {
            feePayerPrivateKeys[i] = feePayerPrivateKeys[i].trim();
        }
        
        // Initialize fee payer pool
        this.feePayerPool = new FeePayerPool(client, feePayerPrivateKeys);
    }
    
    public String submitBulkOrders(...) throws Exception {
        // Create transaction
        RawTransaction rawTx = new RawTransaction(...);
        
        // Submit with fee payer pool
        String txHash = feePayerPool.submitWithFeePayer(account, rawTx);
        return txHash;
    }
}
```

Run the example:
```bash
mvn clean package
java -cp target/decibel-java-example-1.0-SNAPSHOT.jar com.decibel.BulkOrderExample
```

## Architecture

```
┌─────────────────────────────────────┐
│        FeePayerPool                 │
│  ┌──────────────────────────────┐  │
│  │  Fee Payer Accounts (0-9)    │  │
│  │  - Account 0 (pre-funded)    │  │
│  │  - Account 1 (pre-funded)    │  │
│  │  - ...                        │  │
│  │  - Account 9 (pre-funded)    │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Transaction Submission             │
│  - Random fee payer selection       │
│  - Parallel transaction processing  │
│  - No sequence conflicts            │
└─────────────────────────────────────┘
```

## Benefits

1. **High Throughput**: Submit transactions in parallel without sequence number conflicts
2. **Simple Setup**: Just provide an array of private keys
3. **Flexible**: Use any number of accounts
4. **Thread-Safe**: Safe for concurrent use
5. **Production Ready**: Based on Rust keeper service patterns

## Configuration Examples

### Development (Low Volume)
```properties
# 3 fee payers
fee_payer.private_keys=0x1234...,0x5678...,0x9abc...
```

### Production (High Volume)
```properties
# 20 fee payers
fee_payer.private_keys=0x1111...,0x2222...,0x3333...,...,0xkkkk...
```

### Market Making Bot
```properties
# 10 fee payers
fee_payer.private_keys=0xaaaa...,0xbbbb...,0xcccc...,...,0xjjjj...
```

## Monitoring

Check fee payer balances:
```java
for (int i = 0; i < feePayerPool.getSize(); i++) {
    Ed25519Account feePayer = feePayerPool.getFeePayer(i);
    long balance = client.getAccountBalance(feePayer.getAccountAddress());
    System.out.println("Fee payer " + i + ": " + balance / 100_000_000.0 + " APT");
}
```

## Notes

- **Fee payer accounts must be pre-funded** with sufficient APT before use
- Monitor balances regularly to ensure accounts don't run out of gas
- Thread-safe for concurrent use
- Private keys should be stored securely (consider environment variables or secrets management)

## Reference Implementation

This implementation is based on the Rust keeper service:
- `/Users/kent/etna/rust/keeper-service/src/main.rs`
- Function: `init_and_maintain_worker_accounts`
- Pattern: Deterministic account derivation + background top-ups
