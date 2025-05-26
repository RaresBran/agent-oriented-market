package sma;

import sma.strategies.*;
import sma.util.AgentMemory;

import java.util.*;
import java.util.stream.Collectors;

public final class TraderAgent extends BaseAgent {
    private final Map<String, Integer> sellsRemaining;
    private final Map<String, Integer> buysRemaining;
    private final Map<String, Integer> inventory;
    private final Map<String, Integer> failedSellAttempts;
    private final Map<String, Integer> failedBuyAttempts;

    private final Map<String, AgentMemory> knownAgentsInfo;

    private final PartnerSelector selector;
    private final PricingPolicy pricing;
    private final TradeStrategy trader;

    public TraderAgent(String id, String type, int entersStep, int defaultCash,
                       Map<String, Integer> sells, Map<String, Integer> buys) {
        super(id, type, entersStep, defaultCash);
        this.sellsRemaining = new HashMap<>(sells);
        this.buysRemaining = new HashMap<>(buys);
        this.selector = new KnowledgePartnerSelector();
        this.pricing = new DynamicPricingPolicy();
        this.trader = new BarterThenSaleStrategy();
        this.failedSellAttempts = new HashMap<>();
        this.failedBuyAttempts = new HashMap<>();
        this.knownAgentsInfo = new HashMap<>();
        this.inventory = new HashMap<>();

        for (String product : sells.keySet()) {
            failedSellAttempts.put(product, 0);
        }
        for (String product : buys.keySet()) {
            failedBuyAttempts.put(product, 0);
        }
    }

    public Map<String, Integer> getSellsRemaining() {
        return sellsRemaining;
    }

    public Map<String, Integer> getBuysRemaining() {
        return buysRemaining;
    }

    public Map<String, Integer> getInventory() {
        return inventory;
    }

    public Map<String, Integer> getFailedSellAttempts() {
        return failedSellAttempts;
    }

    public Map<String, Integer> getFailedBuyAttempts() {
        return failedBuyAttempts;
    }

    public Map<String, AgentMemory> getKnownAgentsInfo() {
        return knownAgentsInfo;
    }

    public int getDynamicSellPrice(String product, int referencePrice) {
        return pricing.getSellPrice(this, product, referencePrice);
    }

    public int getDynamicBuyPrice(String product, int referencePrice) {
        return pricing.getBuyPrice(this, product, referencePrice);
    }

    public Optional<TraderAgent> choosePartner(List<TraderAgent> candidates) {
        return selector.choosePartner(this, candidates);
    }

    public InteractionRecord interactWith(TraderAgent partner, Map<String, Integer> referencePrices) {
        return trader.interact(this, partner, referencePrices);
    }

    public String summary() {
        StringBuilder sb = new StringBuilder(id)
                .append(" has ").append(getCash()).append(" cash");
        appendListOfItemsToString(sb, "buys", buysRemaining);
        appendListOfItemsToString(sb, "sells", sellsRemaining);
        appendListOfItemsToString(sb, "has", inventory);
        return sb.toString();
    }

    private void appendListOfItemsToString(StringBuilder sb, String label, Map<String, Integer> items) {
        List<String> parts = items.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getValue() + " " + e.getKey())
                .collect(Collectors.toList());
        if (!parts.isEmpty()) {
            sb.append("; ").append(label).append(" ")
                    .append(String.join(", ", parts));
        }
    }
}
