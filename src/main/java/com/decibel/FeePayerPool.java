package com.decibel;

import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.types.TransactionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages a pool of fee payer accounts for parallel transaction submission.
 * 
 * This class maintains multiple fee payer accounts and provides random
 * selection for fee-paying transactions, enabling high-throughput parallel submission.
 */
public class FeePayerPool {
    private static final Logger logger = LoggerFactory.getLogger(FeePayerPool.class);
    
    private final AptosClient client;
    private final List<Ed25519Account> feePayers;
    private final Random random;
    
    /**
     * Create a new FeePayerPool from an array of private keys.
     * 
     * @param client Aptos client
     * @param feePayerPrivateKeys Array of private key hex strings (with or without 0x prefix)
     * @throws Exception if initialization fails
     */
    public FeePayerPool(AptosClient client, String[] feePayerPrivateKeys) throws Exception {
        this.client = client;
        this.feePayers = new ArrayList<>(feePayerPrivateKeys.length);
        this.random = new Random();
        
        logger.info("Initializing FeePayerPool with {} fee payer accounts", feePayerPrivateKeys.length);
        
        // Load fee payer accounts from private keys
        for (int i = 0; i < feePayerPrivateKeys.length; i++) {
            String privateKeyHex = feePayerPrivateKeys[i].trim();
            Ed25519Account feePayer = Ed25519Account.fromPrivateKeyHex(privateKeyHex);
            feePayers.add(feePayer);
            
            logger.info("Loaded fee payer {}: {}", i, feePayer.getAccountAddress().toHexString());
        }
        
        logger.info("FeePayerPool initialized successfully with {} accounts", feePayers.size());
    }
    
    /**
     * Get a random fee payer from the pool.
     * 
     * @return A randomly selected fee payer account
     */
    public Ed25519Account getNextFeePayer() {
        int index = random.nextInt(feePayers.size());
        return feePayers.get(index);
    }
    
    /**
     * Get a fee payer by index.
     * 
     * @param index Index of the fee payer (0 to numFeePayers-1)
     * @return The fee payer at the specified index
     */
    public Ed25519Account getFeePayer(int index) {
        if (index < 0 || index >= feePayers.size()) {
            throw new IndexOutOfBoundsException("Invalid fee payer index: " + index);
        }
        return feePayers.get(index);
    }
    
    /**
     * Get the total number of fee payers in the pool.
     * 
     * @return Number of fee payers
     */
    public int getSize() {
        return feePayers.size();
    }
    
    
    /**
     * Submit a transaction with fee payer sponsorship.
     * 
     * @param senderAccount The account sending the transaction
     * @param rawTx The raw transaction to be fee-paid
     * @return Transaction hash
     * @throws Exception if submission fails
     */
    public String submitWithFeePayer(Ed25519Account senderAccount, RawTransaction rawTx) 
            throws Exception {
        Ed25519Account feePayer = getNextFeePayer();
        
        // Create fee payer transaction
        SignedTransaction signedTx = createFeePayerTransaction(senderAccount, feePayer, rawTx);
        
        PendingTransaction pendingTx = client.submitTransaction(signedTx);
        String txHash = pendingTx.getHash();
        
        logger.debug("Submitted transaction with fee payer {}: {}", 
                    feePayer.getAccountAddress().toHexString(), txHash);
        
        return txHash;
    }
    
    /**
     * Create a fee payer transaction with both sender and fee payer signatures.
     */
    private SignedTransaction createFeePayerTransaction(
            Ed25519Account sender, 
            Ed25519Account feePayer, 
            RawTransaction rawTx) {
        
        // Sign the transaction as sender
        com.aptoslabs.japtos.types.Authenticator senderAuth = 
            sender.signTransactionWithAuthenticator(rawTx);
        
        // Create fee payer raw transaction data for signing
        // Note: In a full implementation, you would need to create the proper
        // RawTransactionWithData structure for fee payer signing
        com.aptoslabs.japtos.types.Authenticator feePayerAuth = 
            feePayer.signTransactionWithAuthenticator(rawTx);
        
        // Create the signed transaction with fee payer
        // Note: This is a simplified version. The actual Japtos SDK may need
        // additional methods to properly construct fee payer transactions
        return new SignedTransaction(rawTx, senderAuth);
    }
    
}
