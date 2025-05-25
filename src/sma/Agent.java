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
        int failures = failedSellAttempts.getOrDefault(product, 0);
        return Math.max(1, referencePrice - failures);
    }

    public int getDynamicBuyPrice(String product, int referencePrice) {
        int failures = failedBuyAttempts.getOrDefault(product, 0);
        return Math.min(cash, referencePrice + failures);
    }


    Optional<Agent> choosePartner(List<Agent> candidates) {
        List<Agent> possiblePartners = candidates.stream()
                .filter(a -> !a.id.equals(id) && !a.busy)
                .toList();

        // 30% chance to choose unknow agent
        List<Agent> unknown = possiblePartners.stream()
                .filter(a -> !knownAgentsInfo.containsKey(a.id))
                .collect(Collectors.toList());

        if (!unknown.isEmpty() && Math.random() < 0.3) {
            Agent chosen = pickRandom(unknown);
            knownAgentsInfo.put(chosen.id, new AgentMemory(new HashMap<>(), new HashMap<>()));
            return Optional.of(chosen);
        }

        // 70% chance to choose a prioritized agent
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

    public List<String> performTrade(Agent partner, Map<String, Integer> referencePrices) {
        knownAgentsInfo.put(partner.getId(), new AgentMemory(
                new HashMap<>(partner.getSellsRemaining()),
                new HashMap<>(partner.getBuysRemaining())
        ));
        return performSale(this, partner, referencePrices);
    }

    private List<String> performSale(Agent seller, Agent buyer, Map<String, Integer> referencePrices) {
        List<String> sold = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : seller.getSellsRemaining().entrySet()) {
            String product = entry.getKey();
            int quantity = entry.getValue();
            int want = buyer.getBuysRemaining().getOrDefault(product, 0);
            int price = seller.getDynamicSellPrice(product, referencePrices.getOrDefault(product, 0));
            price = Math.min(price, buyer.getDynamicBuyPrice(product, referencePrices.getOrDefault(product, 0)));
            int maxAffordable = price > 0 ? buyer.getCash() / price : 0;
            int tradeQty = Math.min(Math.min(quantity, want), maxAffordable);
            if (tradeQty <= 0) {
                // Failed attempt
                seller.failedSellAttempts.merge(product, 1, Integer::sum);
                buyer.failedBuyAttempts.merge(product, 1, Integer::sum);
                continue;
            }

            // Reset failure counters
            seller.failedSellAttempts.put(product, 0);
            buyer.failedBuyAttempts.put(product, 0);

            // Execute trade
            entry.setValue(quantity - tradeQty);
            buyer.getBuysRemaining().put(product, want - tradeQty);
            buyer.getInventory().merge(product, tradeQty, Integer::sum);
            seller.setCash(seller.getCash() + tradeQty * price);
            buyer.setCash(buyer.getCash() - tradeQty * price);
            sold.add(tradeQty + " " + product);
        }
        return sold;
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
