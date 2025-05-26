package sma;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Agent {

    abstract Optional<TraderAgent> choosePartner(List<TraderAgent> candidates);
    abstract InteractionRecord interactWith(TraderAgent partner, Map<String, Integer> prices);
}
