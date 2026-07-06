package com.example.betting.wallet.service;

/** 지갑 도메인 예외. gRPC 계층에서 적절한 Status 로 매핑된다. */
public final class WalletExceptions {

    private WalletExceptions() {
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String userId) {
            super("account not found: " + userId);
        }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String userId, long requested, long available) {
            super("insufficient funds for " + userId + ": requested=" + requested + ", available=" + available);
        }
    }
}
