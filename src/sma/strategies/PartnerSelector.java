package sma.strategies;

import sma.TraderAgent;

import java.util.List;
import java.util.Optional;

/**
 * Policy for selecting an interaction partner.
 */
public interface PartnerSelector {
    Optional<TraderAgent> choosePartner(TraderAgent self, List<TraderAgent> candidates);
}

