package sma.strategies;

import sma.InteractionRecord;
import sma.TraderAgent;

import java.util.Map;

/**
 * Policy that defines how two agents interact (barter, buy, sell).
 */
public interface TradeStrategy {
    InteractionRecord interact(TraderAgent initiator, TraderAgent partner, Map<String,Integer> referencePrices);
}

