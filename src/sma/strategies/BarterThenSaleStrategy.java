package sma.strategies;

import sma.InteractionRecord;
import sma.TraderAgent;
import sma.util.AgentMemory;
import sma.util.TradeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarterThenSaleStrategy implements TradeStrategy {
    private static final int BARTER_MARGIN = 5;

    @Override
    public InteractionRecord interact(TraderAgent initiator, TraderAgent partner, Map<String, Integer> referencePrices) {
        initiator.getKnownAgentsInfo().put(partner.getId(), new AgentMemory(
                new HashMap<>(partner.getSellsRemaining()),
                new HashMap<>(partner.getBuysRemaining())
        ));

        List<String> bartered = attemptBarter(initiator, partner, referencePrices);

        List<String> sold = performSaleBetweenAgents(initiator, partner, referencePrices, true);
        List<String> bought = performSaleBetweenAgents(partner, initiator, referencePrices, false);

        return new InteractionRecord(initiator.getId(), partner.getId(), sold, bought, bartered);
    }

    private List<String> performSaleBetweenAgents(TraderAgent seller, TraderAgent buyer, Map<String, Integer> referencePrices, boolean isSellerInitiator) {
        List<String> sold = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : seller.getSellsRemaining().entrySet()) {
            String product = entry.getKey();
            int quantity = entry.getValue();

            int referencePrice = referencePrices.getOrDefault(product, 0);
            int sellPrice = seller. getDynamicSellPrice(product, referencePrice);
            int buyPrice = buyer.getDynamicBuyPrice(product, referencePrice);
            int agreedPrice = isSellerInitiator ? sellPrice : buyPrice;

            if (agreedPrice <= 0) continue;

            TradeType tradeType = determineTradeType(buyer, product, agreedPrice, referencePrice);
            if (tradeType == TradeType.NONE)
                continue;

            int tradeQty = computeTradeQuantity(buyer, product, quantity, agreedPrice, tradeType);
            if (tradeQty <= 0) {
                recordFailedAttempt(seller, buyer, product);
                continue;
            }

            updateStatesAfterTrade(seller, buyer, product, tradeQty, agreedPrice, tradeType);
            sold.add(tradeQty + " " + product + (tradeType == TradeType.SPECULATIVE ? " (speculated)" : ""));
        }

        return sold;
    }

    private List<String> attemptBarter(TraderAgent initiator, TraderAgent other, Map<String, Integer> referencePrices) {
        List<String> exchanged = new ArrayList<>();

        for (Map.Entry<String, Integer> mySell : initiator.getSellsRemaining().entrySet()) {
            String myProduct = mySell.getKey();
            int myAvailable = mySell.getValue();
            int otherWants = other.getBuysRemaining().getOrDefault(myProduct, 0);

            if (myAvailable <= 0 || otherWants <= 0) continue;

            for (Map.Entry<String, Integer> theirSell : other.getSellsRemaining().entrySet()) {
                String theirProduct = theirSell.getKey();
                int theirAvailable = theirSell.getValue();
                int iWant = initiator.getBuysRemaining().getOrDefault(theirProduct, 0);

                if (theirAvailable <= 0 || iWant <= 0) continue;

                // Reference prices
                int myUnitPrice = referencePrices.getOrDefault(myProduct, 0);
                int theirUnitPrice = referencePrices.getOrDefault(theirProduct, 0);

                if (myUnitPrice == 0 || theirUnitPrice == 0) continue;

                // Maximum units both agents can give/receive
                int maxMyUnits = Math.min(myAvailable, otherWants);
                int maxTheirUnits = Math.min(theirAvailable, iWant);

                // Try all combinations within quantity bounds
                for (int myQty = 1; myQty <= maxMyUnits; myQty++) {
                    int myTotalValue = myQty * myUnitPrice;

                    for (int theirQty = 1; theirQty <= maxTheirUnits; theirQty++) {
                        int theirTotalValue = theirQty * theirUnitPrice;

                        if (Math.abs(myTotalValue - theirTotalValue) <= BARTER_MARGIN) {
                            // Execute barter
                            initiator.getSellsRemaining().merge(myProduct, -myQty, Integer::sum);
                            other.getBuysRemaining().merge(myProduct, -myQty, Integer::sum);
                            other.getInventory().merge(myProduct, myQty, Integer::sum);

                            other.getSellsRemaining().merge(theirProduct, -theirQty, Integer::sum);
                            initiator.getBuysRemaining().merge(theirProduct, -theirQty, Integer::sum);
                            initiator.getInventory().merge(theirProduct, theirQty, Integer::sum);

                            exchanged.add(myQty + " " + myProduct +
                                    " for " + theirQty + " " + theirProduct);
                            return exchanged;
                        }
                    }
                }
            }
        }
        return exchanged;
    }

    private TradeType determineTradeType(TraderAgent buyer, String product, int price, int reference) {
        // Normal trade if the buyer wants this product
        if (buyer.getBuysRemaining().getOrDefault(product, 0) > 0) {
            return TradeType.NORMAL;
        }

        // Speculative trade if the buyer only wants to resell this product
        // and the price of the product is less than 80% of the reference price
        if (price <= reference * 0.8
                && !buyer.getSellsRemaining().containsKey(product)) {
            return TradeType.SPECULATIVE;
        }

        // No trade can be made
        return TradeType.NONE;
    }

    private int computeTradeQuantity(TraderAgent buyer, String product, int availableQty, int price, TradeType type) {
        // Number of items the buyer can afford
        int maxAffordable = buyer.getCash() / price;

        int desiredQty = switch (type) {
            case NORMAL -> buyer.getBuysRemaining().get(product);
            case SPECULATIVE -> maxAffordable;
            default -> 0;
        };

        return Math.min(Math.min(availableQty, desiredQty), maxAffordable);
    }

    private void recordFailedAttempt(TraderAgent seller, TraderAgent buyer, String product) {
        seller.getFailedSellAttempts().merge(product, 1, Integer::sum);
        buyer.getFailedBuyAttempts().merge(product, 1, Integer::sum);
    }

    private void updateStatesAfterTrade(TraderAgent seller, TraderAgent buyer, String product, int quantity, int price, TradeType type) {

        // Update seller state
        seller.getFailedSellAttempts().put(product, 0);
        seller.getSellsRemaining().merge(product, -quantity, Integer::sum);
        seller.setCash(seller.getCash() + quantity * price);

        // Update buyer state
        buyer.getFailedBuyAttempts().put(product, 0);
        buyer.setCash(buyer.getCash() - quantity * price);
        buyer.getInventory().merge(product, quantity, Integer::sum);

        // Update buyer state based on the trade type that was performed
        switch (type) {
            case NORMAL -> buyer.getBuysRemaining().merge(product, -quantity, Integer::sum);
            case SPECULATIVE -> buyer.getSellsRemaining().merge(product, quantity, Integer::sum);
        }

    }
}
