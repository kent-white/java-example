package com.decibel;

import com.aptoslabs.japtos.core.AccountAddress;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Market configuration containing tick size, lot size, and price/size math helpers.
 * Provides methods to round prices and sizes to valid tick and lot increments.
 */
public class MarketConfig {
    private final AccountAddress marketAddr;
    private final String marketName;
    private final int sizeDecimals;
    private final int maxLeverage;
    private final long tickSize;
    private final long minSize;
    private final long lotSize;
    private final long maxOpenInterest;
    private final int priceDecimals;

    /**
     * Create a new MarketConfig.
     *
     * @param marketAddr Market address
     * @param marketName Market name (e.g., "APT/USD")
     * @param sizeDecimals Number of decimal places for size representation
     * @param maxLeverage Maximum allowed leverage
     * @param tickSize Minimum price increment in integer form
     * @param minSize Minimum order size
     * @param lotSize Minimum size increment in integer form
     * @param maxOpenInterest Maximum open interest
     * @param priceDecimals Number of decimal places for price representation
     */
    public MarketConfig(AccountAddress marketAddr, String marketName, int sizeDecimals,
                        int maxLeverage, long tickSize, long minSize, long lotSize,
                        long maxOpenInterest, int priceDecimals) {
        this.marketAddr = marketAddr;
        this.marketName = marketName;
        this.sizeDecimals = sizeDecimals;
        this.maxLeverage = maxLeverage;
        this.tickSize = tickSize;
        this.minSize = minSize;
        this.lotSize = lotSize;
        this.maxOpenInterest = maxOpenInterest;
        this.priceDecimals = priceDecimals;
    }

    /**
     * Parse a MarketConfig from a JSON node.
     *
     * @param json JSON node containing market configuration
     * @return MarketConfig instance
     */
    public static MarketConfig fromJson(JsonNode json) {
        return new MarketConfig(
            AccountAddress.fromHex(json.path("market_addr").asText()),
            json.path("market_name").asText(),
            json.path("sz_decimals").asInt(),
            json.path("max_leverage").asInt(),
            json.path("tick_size").asLong(),
            json.path("min_size").asLong(),
            json.path("lot_size").asLong(),
            json.path("max_open_interest").asLong(),
            json.path("px_decimals").asInt()
        );
    }

    /**
     * Round a price to the nearest valid tick increment.
     *
     * @param priceInt Price as an integer (e.g., 260000000 for $2.60 with 8 decimals)
     * @param ceil If true, round up; if false, round down
     * @return Price rounded to nearest tick, as an integer
     */
    public long priceToTickInteger(long priceInt, boolean ceil) {
        if (ceil) {
            // Round up to next tick
            return tickSize * ((Math.max(priceInt, 1) - 1) / tickSize + 1);
        } else {
            // Round down to previous tick
            return tickSize * (priceInt / tickSize);
        }
    }

    /**
     * Round a size to the nearest valid lot increment (always rounds up).
     *
     * @param sizeInt Size as an integer (with size decimals already applied)
     * @return Size rounded up to nearest lot, as an integer
     */
    public long sizeToLotInteger(long sizeInt) {
        // Always round up for sizes to ensure minimum lot size
        return lotSize * ((Math.max(sizeInt, 1) - 1) / lotSize + 1);
    }

    // Getters

    public AccountAddress getMarketAddr() {
        return marketAddr;
    }

    public String getMarketName() {
        return marketName;
    }

    public int getSizeDecimals() {
        return sizeDecimals;
    }

    public int getMaxLeverage() {
        return maxLeverage;
    }

    public long getTickSize() {
        return tickSize;
    }

    public long getMinSize() {
        return minSize;
    }

    public long getLotSize() {
        return lotSize;
    }

    public long getMaxOpenInterest() {
        return maxOpenInterest;
    }

    public int getPriceDecimals() {
        return priceDecimals;
    }

    @Override
    public String toString() {
        return String.format("MarketConfig{marketAddr=%s, marketName='%s', sizeDecimals=%d, " +
            "maxLeverage=%d, tickSize=%d, minSize=%d, lotSize=%d, maxOpenInterest=%d, priceDecimals=%d}",
            marketAddr, marketName, sizeDecimals, maxLeverage, tickSize, minSize,
            lotSize, maxOpenInterest, priceDecimals);
    }
}
