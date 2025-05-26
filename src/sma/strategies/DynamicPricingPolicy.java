package sma.strategies;

import sma.TraderAgent;

/**
 * Implements price adjustments based on past failures.
 */
public class DynamicPricingPolicy implements PricingPolicy {

    @Override
    public int getSellPrice(TraderAgent agent, String product, int referencePrice) {
        int failures = agent.getFailedSellAttempts().getOrDefault(product, 0);
        return Math.max(1, referencePrice - failures / 2);
    }

    @Override
    public int getBuyPrice(TraderAgent agent, String product, int referencePrice) {
        int failures = agent.getFailedBuyAttempts().getOrDefault(product, 0);
        return referencePrice + failures / 2;
    }
}
