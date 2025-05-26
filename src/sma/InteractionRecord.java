package sma;

import java.util.List;

final class InteractionRecord {
    private final String initiatorId;
    private final String partnerId;
    private final List<String> sold;
    private final List<String> bought;
    private final List<String> bartered;

    InteractionRecord(String initiatorId, String partnerId,
                      List<String> sold, List<String> bought, List<String> bartered) {
        this.initiatorId = initiatorId;
        this.partnerId = partnerId;
        this.sold = sold;
        this.bought = bought;
        this.bartered = bartered;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(initiatorId)
                .append(" to ").append(partnerId).append(":");

        boolean hasPrevious = false;

        if (!bartered.isEmpty()) {
            sb.append(" bartered ").append(String.join(", ", bartered));
            hasPrevious = true;
        }
        if (!sold.isEmpty()) {
            if (hasPrevious) sb.append("; ");
            sb.append("sells ").append(String.join(", ", sold));
            hasPrevious = true;
        }
        if (!bought.isEmpty()) {
            if (hasPrevious) sb.append("; ");
            sb.append("buys ").append(String.join(", ", bought));
        }

        if (sold.isEmpty() && bought.isEmpty() && bartered.isEmpty()) {
            sb.append(" no transaction");
        }

        return sb.toString();
    }
}
