package sma.util;

import java.util.Map;
import java.util.Objects;

public class AgentMemory {
    private final Map<String, Integer> sells;
    private final Map<String, Integer> buys;

    public AgentMemory(Map<String, Integer> sells, Map<String, Integer> buys) {
        this.sells = Map.copyOf(Objects.requireNonNull(sells));
        this.buys  = Map.copyOf(Objects.requireNonNull(buys));
    }

    public Map<String, Integer> getProductsToSell() {
        return sells;
    }

    public Map<String, Integer> getProductsToBuy() {
        return buys;
    }
}
