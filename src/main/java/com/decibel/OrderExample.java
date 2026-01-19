package com.decibel;

import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.decibel.DecibelTransactions;
import com.decibel.DecibelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Example Java application demonstrating how to submit a single order to Decibel DEX using Japtos SDK.
 */
public class OrderExample {
    private static final Logger logger = LoggerFactory.getLogger(OrderExample.class);
    
    private final AptosClient client;
    private final Properties config;
    private final Ed25519Account account;
    
    public OrderExample() throws Exception {
        // Load configuration
        this.config = InputUtils.loadConfig();
        
        // Initialize Aptos client
        String fullnodeUrl = config.getProperty("aptos.fullnode.url");
        this.client = new AptosClient(fullnodeUrl);
        
        // Initialize account (load from config or generate new)
        this.account = InputUtils.initializeAccount(client, config);
    }
    
    public String submitOrder() throws Exception {
        String packageAddr = config.getProperty("deployment.package");
        int chainId = Integer.parseInt(config.getProperty("chain.id"));
        
        // Hardcoded order parameters
        long price = 10000000L;
        long size = 100000L;
        boolean isBuy = true;
        int timeInForce = 2; // IOC
        boolean isReduceOnly = false;
        
        AccountAddress packageAddress = AccountAddress.fromHex(packageAddr);
        // APT-PERP market address
        AccountAddress marketAddress = AccountAddress.fromHex("0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36");
        AccountAddress subaccountAddr = DecibelUtils.getPrimarySubaccountAddr(packageAddress, account.getAccountAddress());
        
        logger.info("Market: {}", marketAddress);
        logger.info("Subaccount: {}", subaccountAddr);
        logger.info("Order: {} {} @ {} (TIF: {}, ReduceOnly: {})", 
                   isBuy ? "BUY" : "SELL", size, price, timeInForce, isReduceOnly);
        
        logger.info("Submitting transaction...");
        String txHash = DecibelTransactions.placeOrder(
            client, account, packageAddress, subaccountAddr, marketAddress, 
            price, size, isBuy, timeInForce, isReduceOnly, chainId);
        logger.info("Transaction submitted: {}", txHash);
        logger.info("Transaction committed successfully!");
        
        return txHash;
    }
    
    public static void main(String[] args) {
        try {
            OrderExample example = new OrderExample();
            
            // Submit single order
            String txHash = example.submitOrder();
            System.out.println("\n✅ Single order submitted successfully!");
            System.out.println("Transaction Hash: " + txHash);
            System.out.println("View on explorer:");
            System.out.println("https://explorer.aptoslabs.com/txn/" + txHash + "?network=decibel");
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Failed to submit order", e);
            System.err.println("\n❌ Failed to submit order: " + e.getMessage());
            System.exit(1);
        }
    }
}
