package com.decibel;

import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.types.MoveOption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transaction methods for Decibel DEX operations.
 */
public class DecibelTransactions {
    
    /**
     * Mint USDC tokens to an account.
     */
    public static void mintUsdc(
            AptosClient client,
            Ed25519Account account,
            AccountAddress packageAddress,
            AccountAddress toAddr,
            long amount,
            int chainId) throws Exception {
        
        ModuleId moduleId = new ModuleId(packageAddress, new Identifier("usdc"));
        
        List<TransactionArgument> functionArgs = new ArrayList<>();
        functionArgs.add(new TransactionArgument.AccountAddress(toAddr));
        functionArgs.add(new TransactionArgument.U64(amount));
        
        TransactionPayload payload = new EntryFunctionPayload(
            moduleId,
            new Identifier("mint"),
            Arrays.asList(),
            functionArgs
        );
        
        long sequenceNumber = client.getNextSequenceNumber(account.getAccountAddress());
        
        RawTransaction rawTx = new RawTransaction(
            account.getAccountAddress(),
            sequenceNumber,
            payload,
            1000000L,
            100L,
            System.currentTimeMillis() / 1000 + 3600,
            chainId
        );
        
        SignedTransaction signedTx = new SignedTransaction(
            rawTx,
            account.signTransactionWithAuthenticator(rawTx)
        );
        
        PendingTransaction pendingTx = client.submitTransaction(signedTx);
        client.waitForTransaction(pendingTx.getHash());
    }
    
    /**
     * Deposit to a subaccount.
     */
    public static void depositToSubaccount(
            AptosClient client,
            Ed25519Account account,
            AccountAddress packageAddress,
            AccountAddress subaccountAddr,
            AccountAddress assetAddress,
            long amount,
            int chainId) throws Exception {
        
        ModuleId moduleId = new ModuleId(packageAddress, new Identifier("dex_accounts"));
        
        List<TransactionArgument> functionArgs = new ArrayList<>();
        functionArgs.add(new TransactionArgument.AccountAddress(subaccountAddr));
        functionArgs.add(new TransactionArgument.AccountAddress(assetAddress));
        functionArgs.add(new TransactionArgument.U64(amount));
        
        TransactionPayload payload = new EntryFunctionPayload(
            moduleId,
            new Identifier("deposit_to_subaccount_at"),
            Arrays.asList(),
            functionArgs
        );
        
        long sequenceNumber = client.getNextSequenceNumber(account.getAccountAddress());
        
        RawTransaction rawTx = new RawTransaction(
            account.getAccountAddress(),
            sequenceNumber,
            payload,
            1000000L,
            100L,
            System.currentTimeMillis() / 1000 + 3600,
            chainId
        );
        
        SignedTransaction signedTx = new SignedTransaction(
            rawTx,
            account.signTransactionWithAuthenticator(rawTx)
        );
        
        PendingTransaction pendingTx = client.submitTransaction(signedTx);
        client.waitForTransaction(pendingTx.getHash());
    }
    
    /**
     * Submit an order to Decibel DEX.
     */
    public static String placeOrder(
            AptosClient client,
            Ed25519Account account,
            AccountAddress packageAddress,
            AccountAddress subaccountAddr,
            AccountAddress marketAddress,
            long price,
            long size,
            boolean isBuy,
            int timeInForce,
            boolean isReduceOnly,
            int chainId) throws Exception {
        
        ModuleId moduleId = new ModuleId(packageAddress, new Identifier("dex_accounts"));
        
        List<TransactionArgument> functionArgs = new ArrayList<>();
        functionArgs.add(new TransactionArgument.AccountAddress(subaccountAddr));
        functionArgs.add(new TransactionArgument.AccountAddress(marketAddress));
        functionArgs.add(new TransactionArgument.U64(price));
        functionArgs.add(new TransactionArgument.U64(size));
        functionArgs.add(new TransactionArgument.Bool(isBuy));
        functionArgs.add(new TransactionArgument.U8((byte) timeInForce));
        functionArgs.add(new TransactionArgument.Bool(isReduceOnly));
        // Optional parameters
        functionArgs.add(MoveOption.<TransactionArgument.String>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        functionArgs.add(MoveOption.<TransactionArgument.AccountAddress>empty());
        functionArgs.add(MoveOption.<TransactionArgument.U64>empty());
        
        TransactionPayload payload = new EntryFunctionPayload(
            moduleId,
            new Identifier("place_order_to_subaccount"),
            Arrays.asList(),
            functionArgs
        );
        
        long sequenceNumber = client.getNextSequenceNumber(account.getAccountAddress());
        
        RawTransaction rawTx = new RawTransaction(
            account.getAccountAddress(),
            sequenceNumber,
            payload,
            1000000L,
            100L,
            System.currentTimeMillis() / 1000 + 3600,
            chainId
        );
        
        SignedTransaction signedTx = new SignedTransaction(
            rawTx,
            account.signTransactionWithAuthenticator(rawTx)
        );
        
        PendingTransaction pendingTx = client.submitTransaction(signedTx);
        String txHash = pendingTx.getHash();
        client.waitForTransaction(txHash);
        
        return txHash;
    }
    
    /**
     * Submit bulk orders to Decibel DEX.
     */
    public static String placeBulkOrders(
            AptosClient client,
            Ed25519Account account,
            AccountAddress packageAddress,
            AccountAddress subaccountAddr,
            AccountAddress marketAddress,
            long sequenceNumber,
            List<Long> bidPrices,
            List<Long> bidSizes,
            List<Long> askPrices,
            List<Long> askSizes,
            int chainId) throws Exception {
        
        ModuleId moduleId = new ModuleId(packageAddress, new Identifier("dex_accounts"));
        
        List<TransactionArgument> functionArgs = new ArrayList<>();
        functionArgs.add(new TransactionArgument.AccountAddress(subaccountAddr));
        functionArgs.add(new TransactionArgument.AccountAddress(marketAddress));
        functionArgs.add(new TransactionArgument.U64(sequenceNumber));
        
        // Add U64Vector arguments for bulk order parameters
        functionArgs.add(new TransactionArgument.U64Vector(bidPrices));
        functionArgs.add(new TransactionArgument.U64Vector(bidSizes));
        functionArgs.add(new TransactionArgument.U64Vector(askPrices));
        functionArgs.add(new TransactionArgument.U64Vector(askSizes));
        
        TransactionPayload payload = new EntryFunctionPayload(
            moduleId,
            new Identifier("place_bulk_orders_to_subaccount"),
            Arrays.asList(),
            functionArgs
        );
        
        long accountSequenceNumber = client.getNextSequenceNumber(account.getAccountAddress());
        
        RawTransaction rawTx = new RawTransaction(
            account.getAccountAddress(),
            accountSequenceNumber,
            payload,
            1000000L,
            100L,
            System.currentTimeMillis() / 1000 + 3600,
            chainId
        );
        
        SignedTransaction signedTx = new SignedTransaction(
            rawTx,
            account.signTransactionWithAuthenticator(rawTx)
        );
        
        PendingTransaction pendingTx = client.submitTransaction(signedTx);
        String txHash = pendingTx.getHash();
        client.waitForTransaction(txHash);
        
        return txHash;
    }
    
    /**
     * Cancel bulk orders by submitting empty bid and ask lists.
     * This effectively cancels all orders for the given sequence number.
     */
    public static String cancelBulkOrders(
            AptosClient client,
            Ed25519Account account,
            AccountAddress packageAddress,
            AccountAddress subaccountAddr,
            AccountAddress marketAddress,
            long sequenceNumber,
            int chainId) throws Exception {
        
        // Submit empty vectors to cancel all orders
        List<Long> emptyPrices = new ArrayList<>();
        List<Long> emptySizes = new ArrayList<>();
        
        return placeBulkOrders(
            client, account, packageAddress, subaccountAddr, marketAddress,
            sequenceNumber, emptyPrices, emptySizes, emptyPrices, emptySizes, chainId);
    }
}
