package sma.strategies;

import sma.TraderAgent;

/**
 * Policy for pricing buys and sells.
 */
public interface PricingPolicy {
    int getSellPrice(TraderAgent agent, String product, int referencePrice);
    int getBuyPrice(TraderAgent agent, String product, int referencePrice);
}
