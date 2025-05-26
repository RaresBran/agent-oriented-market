package sma.strategies;

import sma.TraderAgent;
import sma.util.AgentMemory;

import java.util.*;
import java.util.stream.Collectors;

public class KnowledgePartnerSelector implements PartnerSelector {

    @Override
    public Optional<TraderAgent> choosePartner(TraderAgent self, List<TraderAgent> candidates) {
        List<TraderAgent> possiblePartners = candidates.stream()
                .filter(a -> !a.getId().equals(self.getId()) && a.canBeContacted())
                .toList();

        // 40% chance to choose unknow agent
        List<TraderAgent> unknown = possiblePartners.stream()
                .filter(a -> !self.getKnownAgentsInfo().containsKey(a.getId()))
                .collect(Collectors.toList());

        if (!unknown.isEmpty() && Math.random() < 0.4) {
            TraderAgent chosen = pickRandom(unknown);
            self.getKnownAgentsInfo().put(chosen.getId(), new AgentMemory(new HashMap<>(), new HashMap<>()));
            return Optional.of(chosen);
        }

        // 60% chance to choose a prioritized agent
        List<TraderAgent> prioritized = new ArrayList<>();

        for (TraderAgent candidate : possiblePartners) {
            AgentMemory memory = self.getKnownAgentsInfo().get(candidate.getId());
            if (memory == null) continue;

            boolean sellsUseful = memory.getProductsToSell().keySet().stream()
                    .anyMatch(p -> self.getBuysRemaining().getOrDefault(p, 0) > 0);
            boolean buysUseful = memory.getProductsToBuy().keySet().stream()
                    .anyMatch(p -> self.getSellsRemaining().getOrDefault(p, 0) > 0);

            if (sellsUseful || buysUseful) {
                prioritized.add(candidate);
            }
        }

        if (!prioritized.isEmpty()) {
            return Optional.of(pickRandom(prioritized));
        }

        // If no other option, choose a random known agent
        List<TraderAgent> fallback = possiblePartners.stream()
                .filter(a -> self.getKnownAgentsInfo().containsKey(a.getId()))
                .collect(Collectors.toList());

        if (!fallback.isEmpty()) {
            return Optional.of(pickRandom(fallback));
        }

        return Optional.empty();
    }

    private TraderAgent pickRandom(List<TraderAgent> list) {
        return list.get(new Random().nextInt(list.size()));
    }
}
