package sma.util;

import sma.TraderAgent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class AgentConfig {
    final String type;
    final int count;
    final int entersStep;
    final Map<String, Integer> sells;
    final Map<String, Integer> buys;

    private AgentConfig(String type, int count, int entersStep,
                        Map<String, Integer> sells, Map<String, Integer> buys) {
        this.type = Objects.requireNonNull(type);
        this.count = count;
        this.entersStep = entersStep;
        this.sells = Map.copyOf(sells);
        this.buys = Map.copyOf(buys);
    }

    public static AgentConfig fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        int count = (Integer) map.getOrDefault("count", 1);
        int enters = (Integer) map.getOrDefault("enters", 0);
        Map<String, Integer> sells = castMap(map.get("sells"));
        Map<String, Integer> buys = castMap(map.get("buys"));
        return new AgentConfig(type, count, enters, sells, buys);
    }

    public List<TraderAgent> createAgents(int defaultCash) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new TraderAgent(type + i, type, entersStep, defaultCash, sells, buys))
                .collect(Collectors.toList());
    }

    private static Map<String, Integer> castMap(Object obj) {
        if (obj == null) return Collections.emptyMap();
        return ((Map<?, ?>) obj).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> ((Number) e.getValue()).intValue()
                ));
    }
}
