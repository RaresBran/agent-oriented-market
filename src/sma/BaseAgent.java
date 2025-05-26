package sma;

import java.util.*;

/**
 * Base class for all agents, handling identity and lifecycle.
 */
public abstract class BaseAgent {
    private int cash;
    protected final String id;
    protected final String type;
    protected final int entersStep;
    private boolean busy = false;
    private int lastInitiationStep = -1;

    protected BaseAgent(String id, String type, int entersStep, int cash) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.entersStep = entersStep;
        this.cash = cash;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getEntersStep() {
        return entersStep;
    }

    public void resetForStep() {
        busy = false;
    }

    public boolean canInitiate(int step) {
        return !busy && step > lastInitiationStep;
    }

    public boolean canBeContacted() {
        return !busy;
    }

    public void markInteracted(int step) {
        busy = true;
        lastInitiationStep = step;
    }

    public void markBusy() {
        busy = true;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    /**
     * Choose a partner from the given candidates.
     */
    public abstract Optional<TraderAgent> choosePartner(List<TraderAgent> candidates);

    /**
     * Interact with the chosen partner (barter, buy, sell).
     */
    public abstract InteractionRecord interactWith(TraderAgent partner, Map<String, Integer> prices);
}
