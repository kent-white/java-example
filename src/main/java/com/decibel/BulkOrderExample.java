package com.decibel;

import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Example Java application demonstrating interactive bulk order management on Decibel DEX.
 * Press '1' to move all orders up 1%, '2' to move all orders down 1%, or 'x' to exit.
 * Orders maintain the same spread distance while moving up or down in price.
 */
public class BulkOrderExample {
    private static final Logger logger = LoggerFactory.getLogger(BulkOrderExample.class);
    
    private final AptosClient client;
    private final Properties config;
    private final Ed25519Account account;
    private final AccountAddress packageAddress;
    private final AccountAddress marketAddress;
    private final int chainId;
    
    public BulkOrderExample() throws Exception {
        // Load configuration
        this.config = InputUtils.loadConfig();
        
        // Initialize Aptos client
        String fullnodeUrl = config.getProperty("aptos.fullnode.url");
        this.client = new AptosClient(fullnodeUrl);
        
        // Initialize account (load from config or generate new)
        this.account = InputUtils.initializeAccount(client, config);
        
        // Load config values
        this.packageAddress = AccountAddress.fromHex(config.getProperty("deployment.package"));
        this.marketAddress = AccountAddress.fromHex("0xe6de4f6ec47f1bc2ab73920e9f202953e60482e1c1a90e7eef3ee45c8aafee36");
        this.chainId = Integer.parseInt(config.getProperty("chain.id"));
    }
    
    public String submitBulkOrders(long sequenceNumber, List<Long> bidPrices, List<Long> bidSizes, 
                                    List<Long> askPrices, List<Long> askSizes) throws Exception {
        String txHash = DecibelTransactions.placeBulkOrders(
            client, account, packageAddress, marketAddress,
            sequenceNumber, bidPrices, bidSizes, askPrices, askSizes, chainId);
        
        return txHash;
    }
    
    public static void main(String[] args) {
        try {
            BulkOrderExample example = new BulkOrderExample();
            
            AccountAddress subaccountAddr = DecibelUtils.getPrimarySubaccountAddr(example.account.getAccountAddress());
            logger.info("Market: APT-PERP");
            logger.info("Subaccount: {}", subaccountAddr);
            
            // Get the trading API URL from config
            String tradingApiUrl = example.config.getProperty("trading.api.url", "https://api.netna.aptoslabs.com/decibel");

            // Fetch market configuration
            MarketConfig marketConfig = DecibelUtils.getMarketConfig(tradingApiUrl, example.marketAddress);
            if (marketConfig == null) {
                throw new RuntimeException("Market configuration not found for address: " + example.marketAddress);
            }
            logger.info("Market config loaded: {}", marketConfig);

            // Get the current bulk order sequence number from the trading API
            long sequenceNumber = DecibelUtils.getBulkOrderSequenceNumber(
                tradingApiUrl,
                subaccountAddr,
                example.marketAddress
            );
            logger.info("Starting with sequence number: {}", sequenceNumber);
            
            // Starting mid price and spread offsets (in basis points of mid price)
            double midPrice = 260000000;  // $2.60
            double bidOffset1 = 0.01;  // 1% below mid (bid 1)
            double bidOffset2 = 0.02;  // 2% below mid (bid 2)
            double askOffset1 = 0.01;  // 1% above mid (ask 1)
            double askOffset2 = 0.02;  // 2% above mid (ask 2)
            long orderSize = 100000L;
            
            System.out.println("\n🤖 Interactive Bulk Order Bot");
            System.out.println("==============================");
            System.out.println("Mid Price: $" + String.format("%.2f", midPrice / 100_000_000.0));
            System.out.println("Spread: ±1% and ±2%\n");
            System.out.println("Press '1' + ENTER to move all orders UP 1% (↑)");
            System.out.println("Press '2' + ENTER to move all orders DOWN 1% (↓)");
            System.out.println("Press 'f' + ENTER to fund account (faucet + mint + deposit)");
            System.out.println("Press 'x' + ENTER to cancel orders and exit\n");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
            // Interactive loop
            while (true) {
                System.out.print("Enter command (1/2/f/x): ");
                String input = reader.readLine();
                
                if (input == null) {
                    continue;
                }
                
                input = input.trim().toLowerCase();
                
                if (input.equals("x")) {
                    System.out.println("\n🛑 Cancelling orders and stopping bot...");
                    try {
                        String txHash = DecibelTransactions.cancelBulkOrders(
                            example.client, example.account, example.packageAddress, 
                            example.marketAddress, sequenceNumber, example.chainId);
                        System.out.println("✅ Orders cancelled | Tx: " + txHash.substring(0, 10) + "...");
                    } catch (Exception e) {
                        logger.error("Failed to cancel orders", e);
                        System.err.println("❌ Failed to cancel orders: " + e.getMessage());
                    }
                    System.exit(0);
                } else if (input.equals("1")) {
                    // Move mid price up by 1%
                    midPrice *= 1.01;
                    System.out.println("↑ Moving UP to mid $" + String.format("%.2f", midPrice / 100_000_000.0));
                } else if (input.equals("2")) {
                    // Move mid price down by 1%
                    midPrice *= 0.99;
                    System.out.println("↓ Moving DOWN to mid $" + String.format("%.2f", midPrice / 100_000_000.0));
                } else if (input.equals("f")) {
                    // Fund account with faucet, mint USDC, and deposit
                    System.out.println("\n💰 Funding account...");
                    try {
                        // Request APT from faucet
                        logger.info("Requesting APT from faucet...");
                        DecibelUtils.fundAccountFromFaucet(example.account.getAccountAddress());
                        System.out.println("✅ Faucet funding requested");
                        Thread.sleep(3000);
                        
                        // Mint USDC
                        long USDC_MINT_AMOUNT = 100_000_000L; // 100 USDC
                        logger.info("Minting {} USDC...", USDC_MINT_AMOUNT / 100_000_000.0);
                        DecibelTransactions.mintUsdc(example.client, example.account, example.packageAddress, 
                            example.account.getAccountAddress(), USDC_MINT_AMOUNT, example.chainId);
                        System.out.println("✅ USDC minted");
                        
                        // Deposit USDC to subaccount
                        long USDC_DEPOSIT_AMOUNT = 50_000_000L; // 50 USDC
                        AccountAddress subaccount = DecibelUtils.getPrimarySubaccountAddr(example.account.getAccountAddress());
                        AccountAddress usdcAddress = DecibelUtils.createObjectAddress(example.packageAddress, "USDC");
                        logger.info("Depositing {} USDC to subaccount: {}", USDC_DEPOSIT_AMOUNT / 100_000_000.0, subaccount);
                        DecibelTransactions.depositToSubaccount(example.client, example.account, example.packageAddress, 
                            subaccount, usdcAddress, USDC_DEPOSIT_AMOUNT, example.chainId);
                        System.out.println("✅ USDC deposited to subaccount\n");
                    } catch (Exception e) {
                        logger.error("Failed to fund account", e);
                        System.err.println("❌ Failed to fund account: " + e.getMessage() + "\n");
                    }
                    continue;
                } else {
                    System.out.println("❌ Invalid command. Use 1, 2, f, or x.");
                    continue;
                }
                
                // Calculate bid and ask prices from mid price with fixed offsets
                // Round prices to valid tick increments and sizes to valid lot increments
                long bidPrice1 = marketConfig.priceToTickInteger((long)(midPrice * (1 - bidOffset1)), false);  // Round down for bids
                long bidPrice2 = marketConfig.priceToTickInteger((long)(midPrice * (1 - bidOffset2)), false);
                long askPrice1 = marketConfig.priceToTickInteger((long)(midPrice * (1 + askOffset1)), true);   // Round up for asks
                long askPrice2 = marketConfig.priceToTickInteger((long)(midPrice * (1 + askOffset2)), true);

                long roundedSize = marketConfig.sizeToLotInteger(orderSize);

                List<Long> bidPrices = Arrays.asList(bidPrice1, bidPrice2);
                List<Long> bidSizes = Arrays.asList(roundedSize, roundedSize);
                List<Long> askPrices = Arrays.asList(askPrice1, askPrice2);
                List<Long> askSizes = Arrays.asList(roundedSize, roundedSize);
                
                logger.info("Seq {}: Bids [{}, {}] Asks [{}, {}]",
                    sequenceNumber,
                    bidPrices.get(0), bidPrices.get(1),
                    askPrices.get(0), askPrices.get(1));
                
                try {
                    String txHash = example.submitBulkOrders(sequenceNumber, bidPrices, bidSizes, askPrices, askSizes);
                    System.out.println("✅ Orders updated For subaccount: " + subaccountAddr + " | Tx: " + txHash.substring(0, 10) + "...\n");
                    System.out.println("View on explorer:");
                    System.out.println("https://explorer.aptoslabs.com/txn/" + txHash + "?network=decibel");

                    sequenceNumber++;
                } catch (Exception e) {
                    logger.error("Failed to submit bulk orders", e);
                    System.err.println("❌ Failed: " + e.getMessage() + "\n");
                }
            }
            
        } catch (Exception e) {
            logger.error("Bot failed", e);
            System.err.println("\n❌ Bot failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
