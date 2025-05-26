package sma;

import sma.util.AgentConfig;

import java.util.List;
import java.util.Map;

public record Scenario(int defaultCash, Map<String, Integer> prices, List<AgentConfig> agentConfigs) {
}

