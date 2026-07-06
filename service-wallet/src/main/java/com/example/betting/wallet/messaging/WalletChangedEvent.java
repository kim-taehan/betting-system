package com.example.betting.wallet.messaging;

/** Kafka 토픽 {@code WalletChanged} 페이로드. 잔액 변동 1건. */
public record WalletChangedEvent(
        String userId,
        String currency,
        long balanceMinor,
        String type,          // DEBIT / CREDIT
        long amountMinor,
        String ledgerEntryId,
        long timestampEpochMs
) {
    public static final String TOPIC = "WalletChanged";
}
