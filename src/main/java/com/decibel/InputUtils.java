package com.decibel;

import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.core.AccountAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility methods for loading configuration and initializing accounts.
 */
public class InputUtils {
    private static final Logger logger = LoggerFactory.getLogger(InputUtils.class);
    private static final long USDC_MINT_AMOUNT = 100_000_000L; // 100 USDC (6 decimals)
    private static final long USDC_DEPOSIT_AMOUNT = 50_000_000L; // 50 USDC to deposit
    
    /**
     * Load configuration from config.properties.
     */
    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream input = InputUtils.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IOException("Unable to find config.properties");
            }
            props.load(input);
        }
        return props;
    }
    
    /**
     * Initialize an account from config or generate a new one.
     * If a private key is provided in config, it will be used.
     * Otherwise, a new account is generated, funded, and prepared with USDC.
     */
    public static Ed25519Account initializeAccount(AptosClient client, Properties config) throws Exception {
        Ed25519Account account;
        
        // Load or generate account
        String privateKeyHex = config.getProperty("account.private.key");
        if (privateKeyHex != null && !privateKeyHex.trim().isEmpty()) {
            // Use existing private key from config
            account = Ed25519Account.fromPrivateKeyHex(privateKeyHex.trim());
            logger.info("🔑 Using account from config: {}", account.getAccountAddress());
        } else {
            // Generate new account
            account = Ed25519Account.generate();
            logger.info("✨ Generated new account: {}", account.getAccountAddress());
            
            // Fund account with APT from faucet
            logger.info("Requesting APT from faucet...");
            try {
                DecibelUtils.fundAccountFromFaucet(account.getAccountAddress());
                logger.info("✅ Faucet funding requested");
                Thread.sleep(3000);
            } catch (Exception e) {
                logger.warn("Faucet funding failed (account may already be funded): {}", e.getMessage());
            }
            
            // Mint USDC to the account
            logger.info("Minting {} USDC...", USDC_MINT_AMOUNT / 100_000_000.0);
            AccountAddress packageAddress = AccountAddress.fromHex(config.getProperty("deployment.package"));
            int chainId = Integer.parseInt(config.getProperty("chain.id"));
            DecibelTransactions.mintUsdc(client, account, packageAddress, account.getAccountAddress(), USDC_MINT_AMOUNT, chainId);
            logger.info("✅ USDC minted");
            
            // Deposit USDC to primary subaccount
            AccountAddress subaccountAddr = DecibelUtils.getPrimarySubaccountAddr(packageAddress, account.getAccountAddress());
            logger.info("Depositing {} USDC to subaccount: {}", USDC_DEPOSIT_AMOUNT / 100_000_000.0, subaccountAddr);
            AccountAddress usdcAddress = DecibelUtils.createObjectAddress(packageAddress, "USDC");
            DecibelTransactions.depositToSubaccount(client, account, packageAddress, subaccountAddr, usdcAddress, USDC_DEPOSIT_AMOUNT, chainId);
            logger.info("✅ USDC deposited to subaccount");
        }
        
        return account;
    }
}
