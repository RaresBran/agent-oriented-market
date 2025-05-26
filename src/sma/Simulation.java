package sma;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class Simulation {
    private final List<TraderAgent> traderAgents;
    private final Map<String, Integer> prices;

    public Simulation(Scenario scenario) {
        this.prices = Map.copyOf(scenario.prices());
        this.traderAgents = scenario.agentConfigs().stream()
                .flatMap(cfg -> cfg.createAgents(scenario.defaultCash()).stream())
                .collect(Collectors.toList());
    }

    // main loop
    public void runIndefinitely(long delayMs) {
        int step = 0;
        while (true) {
            final int currentStep = step;
            List<TraderAgent> present = traderAgents.stream()
                    .filter(a -> a.getEntersStep() <= currentStep)
                    .peek(TraderAgent::resetForStep)
                    .sorted(Comparator.comparing(TraderAgent::getType).thenComparing(TraderAgent::getId))
                    .collect(Collectors.toList());

            List<InteractionRecord> records = performStep(present, prices, currentStep);
            printStep(currentStep, present, records);

            step++;
            sleep(delayMs);
        }
    }

    private static List<InteractionRecord> performStep(List<TraderAgent> traderAgents, Map<String, Integer> prices, int step) {
        List<TraderAgent> shuffled = new ArrayList<>(traderAgents);
        Collections.shuffle(shuffled);
        List<InteractionRecord> records = new ArrayList<>();

        for (TraderAgent initiator : shuffled) {
            if (!initiator.canInitiate(step)) {
                continue;
            }

            Optional<TraderAgent> partner = initiator.choosePartner(shuffled);
            partner.filter(TraderAgent::canBeContacted).ifPresent(p -> {
                initiator.markInteracted(step);
                p.markBusy();
                records.add(
                        initiator.interactWith(p, prices)
                );
            });
        }
        return records;
    }

    private void printStep(int step, List<TraderAgent> present, List<InteractionRecord> records) {
        records.forEach(r -> System.out.println(step + ": " + r));
        present.forEach(a -> System.out.println(step + " — " + a.summary()));
        System.out.println();
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
