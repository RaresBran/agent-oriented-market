package sma;

import java.util.*;
import java.util.stream.Collectors;

final class Agent {
    private final String id;
    private final String type;
    private final int entersStep;
    private int cash;
    private final Map<String, Integer> sellsRemaining;
    private final Map<String, Integer> buysRemaining;
    private final Map<String, Integer> failedSellAttempts;
    private final Map<String, Integer> failedBuyAttempts;
    private final Map<String, Integer> inventory;
    private final Map<String, AgentMemory> knownAgentsInfo;
    private int lastInitiationStep = -1;
    private boolean busy;

    Agent(String id, String type, int entersStep, int defaultCash,
          Map<String, Integer> sells, Map<String, Integer> buys) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.entersStep = entersStep;
        this.cash = defaultCash;
        this.sellsRemaining = new HashMap<>(sells);
        this.buysRemaining = new HashMap<>(buys);
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

    static class AgentMemory {
        Map<String, Integer> sells;
        Map<String, Integer> buys;

        AgentMemory(Map<String, Integer> sells, Map<String, Integer> buys) {
            this.sells = new HashMap<>(sells);
            this.buys = new HashMap<>(buys);
        }
    }

    String getId() {
        return id;
    }

    String getType() {
        return type;
    }

    int getEntersStep() {
        return entersStep;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
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

    void resetForStep() {
        busy = false;
    }

    boolean canInitiate(int step) {
        return !busy && step > lastInitiationStep;
    }

    boolean canBeContacted() {
        return !busy;
    }

    void markInteracted(int step) {
        busy = true;
        lastInitiationStep = step;
    }

    void markBusy() {
        busy = true;
    }

    public int getDynamicSellPrice(String product, int referencePrice) {
        int failures = failedSellAttempts.computeIfAbsent(product, k -> 0);
        return Math.max(1, referencePrice - failures);
    }

    public int getDynamicBuyPrice(String product, int referencePrice) {
        int failures = failedBuyAttempts.computeIfAbsent(product, k -> 0);
        return referencePrice + failures;
    }


    Optional<Agent> choosePartner(List<Agent> candidates) {
        List<Agent> possiblePartners = candidates.stream()
                .filter(a -> !a.id.equals(id) && !a.busy)
                .toList();

        // 40% chance to choose unknow agent
        List<Agent> unknown = possiblePartners.stream()
                .filter(a -> !knownAgentsInfo.containsKey(a.id))
                .collect(Collectors.toList());

        if (!unknown.isEmpty() && Math.random() < 0.4) {
            Agent chosen = pickRandom(unknown);
            knownAgentsInfo.put(chosen.id, new AgentMemory(new HashMap<>(), new HashMap<>()));
            return Optional.of(chosen);
        }

        // 60% chance to choose a prioritized agent
        List<Agent> prioritized = new ArrayList<>();

        for (Agent candidate : possiblePartners) {
            AgentMemory memory = knownAgentsInfo.get(candidate.id);
            if (memory == null) continue;

            boolean sellsUseful = memory.sells.keySet().stream()
                    .anyMatch(p -> buysRemaining.getOrDefault(p, 0) > 0);
            boolean buysUseful = memory.buys.keySet().stream()
                    .anyMatch(p -> sellsRemaining.getOrDefault(p, 0) > 0);

            if (sellsUseful || buysUseful) {
                prioritized.add(candidate);
            }
        }

        if (!prioritized.isEmpty()) {
            return Optional.of(pickRandom(prioritized));
        }

        // If no other option, choose a random known agent
        List<Agent> fallback = possiblePartners.stream()
                .filter(a -> knownAgentsInfo.containsKey(a.id))
                .collect(Collectors.toList());

        if (!fallback.isEmpty()) {
            return Optional.of(pickRandom(fallback));
        }

        return Optional.empty();
    }

    public InteractionRecord interactWith(Agent partner, Map<String, Integer> referencePrices) {
        // Save info about the partner
        knownAgentsInfo.put(partner.getId(), new AgentMemory(
                new HashMap<>(partner.getSellsRemaining()),
                new HashMap<>(partner.getBuysRemaining())
        ));

        // Initiator sells to partner
        List<String> sold = performSale(this, partner, referencePrices);

        // Initiator buys from partner
        List<String> bought = performSale(partner, this, referencePrices);

        return new InteractionRecord(this.getId(), partner.getId(), sold, bought);
    }

    private List<String> performSale(Agent seller, Agent buyer, Map<String, Integer> referencePrices) {
        List<String> sold = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : seller.getSellsRemaining().entrySet()) {
            String product = entry.getKey();
            int quantity = entry.getValue();

            int referencePrice = referencePrices.getOrDefault(product, 0);
            int sellPrice = seller.getDynamicSellPrice(product, referencePrice);
            int buyPrice = buyer.getDynamicBuyPrice(product, referencePrice);
            int agreedPrice = seller == this ? sellPrice : buyPrice;

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

    private TradeType determineTradeType(Agent buyer, String product, int price, int reference) {
        // Normal trade if buyer wants this product
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

    private int computeTradeQuantity(Agent buyer, String product, int availableQty, int price, TradeType type) {
        int maxAffordable = buyer.getCash() / price;
        int desiredQty = switch (type) {
            case NORMAL -> buyer.getBuysRemaining().get(product);
            case SPECULATIVE -> maxAffordable;
            default -> 0;
        };
        return Math.min(Math.min(availableQty, desiredQty), maxAffordable);
    }

    private void recordFailedAttempt(Agent seller, Agent buyer, String product) {
        seller.getFailedSellAttempts().merge(product, 1, Integer::sum);
        buyer.getFailedBuyAttempts().merge(product, 1, Integer::sum);
    }

    private void updateStatesAfterTrade(Agent seller, Agent buyer, String product, int quantity, int price, TradeType type) {

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

    private Agent pickRandom(List<Agent> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    String summary() {
        StringBuilder sb = new StringBuilder(id)
                .append(" has ").append(cash).append(" cash");
        appendListOfItemsToString(sb, "buys", buysRemaining);
        appendListOfItemsToString(sb, "sells", sellsRemaining);
        Map<String, Integer> kept = inventory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() - sellsRemaining.getOrDefault(e.getKey(), 0)
                ));
        appendListOfItemsToString(sb, "has", kept);
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
