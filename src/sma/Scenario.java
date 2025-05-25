package sma;

import java.util.List;
import java.util.Map;

record Scenario(int defaultCash, Map<String, Integer> prices, List<AgentConfig> agentConfigs) {
}

