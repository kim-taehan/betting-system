package com.example.betting.wallet.service;

import com.example.betting.wallet.domain.AccountEntity;
import com.example.betting.wallet.domain.AccountRepository;
import com.example.betting.wallet.domain.LedgerEntry;
import com.example.betting.wallet.domain.LedgerRepository;
import com.example.betting.wallet.domain.LedgerType;
import com.example.betting.wallet.messaging.WalletChangedEvent;
import com.example.betting.wallet.service.WalletExceptions.AccountNotFoundException;
import com.example.betting.wallet.service.WalletExceptions.InsufficientFundsException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WalletService(AccountRepository accountRepository,
                         LedgerRepository ledgerRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.eventPublisher = eventPublisher;
    }

    /** 잔액 조회. */
    @Transactional(readOnly = true)
    public TxnResult getBalance(String userId) {
        AccountEntity account = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return new TxnResult(userId, account.getCurrency(), account.getBalanceMinor(), null);
    }

    /** 차감. 잔액 부족 시 예외. 멱등키 중복 시 원래 결과를 재현. */
    @Transactional
    public TxnResult debit(String userId, long amountMinor, String idempotencyKey, String reference) {
        LedgerEntry replay = replayOrNull(idempotencyKey);
        if (replay != null) {
            return toResult(replay);
        }
        AccountEntity account = lockAccount(userId);
        if (account.getBalanceMinor() < amountMinor) {
            throw new InsufficientFundsException(userId, amountMinor, account.getBalanceMinor());
        }
        long newBalance = account.getBalanceMinor() - amountMinor;
        return apply(account, LedgerType.DEBIT, amountMinor, newBalance, idempotencyKey, reference);
    }

    /** 지급. 멱등키 중복 시 원래 결과를 재현. */
    @Transactional
    public TxnResult credit(String userId, long amountMinor, String idempotencyKey, String reference) {
        LedgerEntry replay = replayOrNull(idempotencyKey);
        if (replay != null) {
            return toResult(replay);
        }
        AccountEntity account = lockAccount(userId);
        long newBalance = account.getBalanceMinor() + amountMinor;
        return apply(account, LedgerType.CREDIT, amountMinor, newBalance, idempotencyKey, reference);
    }

    private AccountEntity lockAccount(String userId) {
        return accountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    private LedgerEntry replayOrNull(String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private TxnResult apply(AccountEntity account, LedgerType type, long amountMinor,
                            long newBalance, String idempotencyKey, String reference) {
        account.setBalanceMinor(newBalance);
        Instant now = Instant.now();
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID().toString(), account.getUserId(), type, amountMinor,
                newBalance, idempotencyKey, reference, now);
        ledgerRepository.save(entry);

        // 커밋 이후에만 Kafka 로 발행 (미커밋 상태를 흘리지 않도록)
        eventPublisher.publishEvent(new WalletChangedEvent(
                account.getUserId(), account.getCurrency(), newBalance, type.name(),
                amountMinor, entry.getId(), now.toEpochMilli()));

        return new TxnResult(account.getUserId(), account.getCurrency(), newBalance, entry.getId());
    }

    private TxnResult toResult(LedgerEntry entry) {
        AccountEntity account = accountRepository.findById(entry.getUserId())
                .orElseThrow(() -> new AccountNotFoundException(entry.getUserId()));
        return new TxnResult(entry.getUserId(), account.getCurrency(),
                entry.getBalanceAfterMinor(), entry.getId());
    }

    /** 서비스 반환값. gRPC 계층에서 proto 로 매핑. */
    public record TxnResult(String userId, String currency, long balanceMinor, String ledgerEntryId) {
    }
}
