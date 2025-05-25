package sma;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ScenarioLoader {

    @SuppressWarnings("unchecked")
    static Scenario load(Path yamlPath) throws IOException {
        try (InputStream in = new FileInputStream(yamlPath.toFile())) {
            Map<String, Object> root = new Yaml().load(in);
            int defaultCash = (Integer) root.get("cash");
            Map<String, Integer> prices = castMap(root.get("prices"));
            List<AgentConfig> configs = ((List<Map<String, Object>>) root.get("agents")).stream()
                    .map(AgentConfig::fromMap)
                    .collect(Collectors.toList());
            return new Scenario(defaultCash, prices, configs);
        }
    }

    private static Map<String, Integer> castMap(Object obj) {
        return ((Map<?, ?>) obj).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> ((Number) e.getValue()).intValue()
                ));
    }
}
