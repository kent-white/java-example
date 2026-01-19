package com.decibel;

import com.aptoslabs.japtos.core.AccountAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for Decibel DEX operations.
 */
public class DecibelUtils {
    private static final String FAUCET_URL = "https://faucet-dev-netna-us-central1-410192433417.us-central1.run.app";
    
    /**
     * Fund account from the Netna faucet.
     */
    public static void fundAccountFromFaucet(AccountAddress address) throws IOException {
        String urlString = String.format("%s/mint?amount=10000000000&address=%s", 
            FAUCET_URL, address.toHexString().replace("0x", ""));
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(0);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        
        // Write empty body (required for POST)
        try (OutputStream os = conn.getOutputStream()) {
            os.flush();
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            throw new IOException("Faucet request failed: " + responseCode + " - " + response);
        }
        
        conn.disconnect();
    }
    
    /**
     * Calculate the primary subaccount address for a given account using the new derivation method.
     * This uses the GlobalSubaccountManager as the deriver and a BCS-serialized SubaccountSeed.
     * 
     * @param packageAddress The package address where the DEX is deployed
     * @param accountAddress The owner account address
     * @return The primary subaccount address
     */
    public static AccountAddress getPrimarySubaccountAddr(AccountAddress packageAddress, AccountAddress accountAddress) {
        try {
            // Step 1: Create the subaccount manager address
            byte[] managerSeed = "GlobalSubaccountManager".getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            digest.update(packageAddress.toBytes());
            digest.update(managerSeed);
            digest.update((byte) 0xFE); // Object address marker
            byte[] managerHash = digest.digest();
            AccountAddress subaccountManager = AccountAddress.fromBytes(managerHash);
            
            // Step 2: Create the SubaccountSeed structure
            // The seed is: [owner_address_bytes + BCS_encoded_MoveString("primary_subaccount")]
            // BCS encoding of MoveString: length as ULEB128 + string bytes
            String seedString = "primary_subaccount";
            byte[] seedStringBytes = seedString.getBytes(StandardCharsets.UTF_8);
            
            // BCS encode the string length as ULEB128 (for strings < 128 chars, it's just 1 byte)
            byte[] bcsLength = new byte[] { (byte) seedStringBytes.length };
            
            // Construct the full seed: owner_address + BCS(MoveString)
            byte[] fullSeed = new byte[accountAddress.toBytes().length + bcsLength.length + seedStringBytes.length];
            System.arraycopy(accountAddress.toBytes(), 0, fullSeed, 0, accountAddress.toBytes().length);
            System.arraycopy(bcsLength, 0, fullSeed, accountAddress.toBytes().length, bcsLength.length);
            System.arraycopy(seedStringBytes, 0, fullSeed, accountAddress.toBytes().length + bcsLength.length, seedStringBytes.length);
            
            // Step 3: Create the primary subaccount address
            digest = MessageDigest.getInstance("SHA3-256");
            digest.update(subaccountManager.toBytes());
            digest.update(fullSeed);
            digest.update((byte) 0xFE); // Object address marker
            
            byte[] hash = digest.digest();
            return AccountAddress.fromBytes(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA3-256 algorithm not available", e);
        }
    }
    
    /**
     * Create an object address from a publisher address and seed.
     */
    public static AccountAddress createObjectAddress(AccountAddress publisherAddr, String seed) {
        try {
            byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);
            
            // Create object address using SHA3-256 hash
            // Format: sha3-256(address + seed + 0xFE)
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            digest.update(publisherAddr.toBytes());
            digest.update(seedBytes);
            digest.update((byte) 0xFE); // Object address marker
            
            byte[] hash = digest.digest();
            return AccountAddress.fromBytes(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA3-256 algorithm not available", e);
        }
    }
    
    /**
     * Get the next bulk order sequence number for a subaccount on a specific market.
     * Queries the trading API to get the latest bulk order and returns the next sequence number.
     * Returns 0 if no bulk orders exist yet.
     */
    public static long getBulkOrderSequenceNumber(
            String tradingApiUrl,
            AccountAddress subaccountAddr,
            AccountAddress marketAddr) throws IOException {
        try {
            // Query the trading API for the latest bulk order
            String urlString = String.format("%s/api/v1/bulk_orders?user=0x%s&market=0x%s",
                tradingApiUrl,
                subaccountAddr.toHexString().replace("0x", ""),
                marketAddr.toHexString().replace("0x", ""));

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 404) {
                // No bulk orders exist yet, start at 0
                return 0;
            }

            if (responseCode != 200) {
                throw new IOException("Failed to fetch bulk order: " + responseCode);
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());

            // Response is an array, get the first element
            if (root.isArray() && root.size() > 0) {
                long currentSeqNum = root.get(0).path("sequence_number").asLong(-1);
                if (currentSeqNum >= 0) {
                    // Return the next sequence number
                    return currentSeqNum + 1;
                }
            }

            // No bulk order found, start at 0
            return 0;
        } catch (Exception e) {
            // If any error, start at 0
            return 0;
        }
    }

    /**
     * Query the trading API for all available markets and their configurations.
     *
     * @param tradingApiUrl Base URL of the trading API
     * @return List of MarketConfig objects for all available markets
     * @throws IOException If the API request fails
     */
    public static List<MarketConfig> getMarkets(String tradingApiUrl) throws IOException {
        return getMarkets(tradingApiUrl, null);
    }

    public static List<MarketConfig> getMarkets(String tradingApiUrl, String apiKey) throws IOException {
        String urlString = String.format("%s/api/v1/markets", tradingApiUrl);

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            throw new IOException("Failed to fetch markets: " + responseCode + " - " + response);
        }

        // Read response
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        // Parse JSON response array
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.toString());

        List<MarketConfig> markets = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode marketNode : root) {
                markets.add(MarketConfig.fromJson(marketNode));
            }
        }

        return markets;
    }

    /**
     * Query the trading API for a specific market configuration by market address.
     *
     * @param tradingApiUrl Base URL of the trading API
     * @param marketAddr Market address to search for
     * @return MarketConfig for the specified market, or null if not found
     * @throws IOException If the API request fails
     */
    public static MarketConfig getMarketConfig(String tradingApiUrl, AccountAddress marketAddr)
            throws IOException {
        return getMarketConfig(tradingApiUrl, marketAddr, null);
    }

    public static MarketConfig getMarketConfig(String tradingApiUrl, AccountAddress marketAddr, String apiKey)
            throws IOException {
        List<MarketConfig> markets = getMarkets(tradingApiUrl, apiKey);

        for (MarketConfig market : markets) {
            if (market.getMarketAddr().equals(marketAddr)) {
                return market;
            }
        }

        return null;
    }

}
