package sma;

import java.util.List;

final class InteractionRecord {
    private final String initiatorId;
    private final String partnerId;
    private final List<String> sold;
    private final List<String> bought;

    InteractionRecord(String initiatorId, String partnerId,
                      List<String> sold, List<String> bought) {
        this.initiatorId = initiatorId;
        this.partnerId = partnerId;
        this.sold = sold;
        this.bought = bought;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(initiatorId)
                .append(" to ").append(partnerId).append(":");
        if (!sold.isEmpty()) {
            sb.append(" sells ").append(String.join(", ", sold));
        }
        if (!bought.isEmpty()) {
            if (!sold.isEmpty()) {
                sb.append(" and");
            }
            sb.append(" buys ").append(String.join(", ", bought));
        }
        if (sold.isEmpty() && bought.isEmpty()) {
            sb.append(" no transaction");
        }
        return sb.toString();
    }
}
