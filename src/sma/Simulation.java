package sma;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class Simulation {
    private final List<Agent> agents;
    private final Map<String, Integer> prices;

    public Simulation(Scenario scenario) {
        this.prices = Map.copyOf(scenario.prices());
        this.agents = scenario.agentConfigs().stream()
                .flatMap(cfg -> cfg.createAgents(scenario.defaultCash()).stream())
                .collect(Collectors.toList());
    }

    // main loop
    public void runIndefinitely(long delayMs) {
        int step = 0;
        while (true) {
            final int currentStep = step;
            List<Agent> present = agents.stream()
                    .filter(a -> a.getEntersStep() <= currentStep)
                    .peek(Agent::resetForStep)
                    .sorted(Comparator.comparing(Agent::getType).thenComparing(Agent::getId))
                    .collect(Collectors.toList());

            List<InteractionRecord> records = performStep(present, prices, currentStep);
            printStep(currentStep, present, records);

            step++;
            sleep(delayMs);
        }
    }

    private static List<InteractionRecord> performStep(List<Agent> agents, Map<String, Integer> prices, int step) {
        List<Agent> shuffled = new ArrayList<>(agents);
        Collections.shuffle(shuffled);
        List<InteractionRecord> records = new ArrayList<>();

        for (Agent initiator : shuffled) {
            if (!initiator.canInitiate(step)) {
                continue;
            }

            Optional<Agent> partner = initiator.choosePartner(shuffled);
            partner.filter(Agent::canBeContacted).ifPresent(p -> {
                initiator.markInteracted(step);
                p.markBusy();
                List<String> sold = initiator.performTrade(p, prices);
                List<String> bought = p.performTrade(initiator, prices);
                records.add(
                        new InteractionRecord(initiator.getId(), p.getId(), sold, bought)
                );
            });
        }
        return records;
    }

    private void printStep(int step, List<Agent> present, List<InteractionRecord> records) {
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
