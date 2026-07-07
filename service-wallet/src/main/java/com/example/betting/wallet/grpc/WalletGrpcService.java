package com.example.betting.wallet.grpc;

import com.example.betting.proto.common.v1.Money;
import com.example.betting.proto.wallet.v1.CreditRequest;
import com.example.betting.proto.wallet.v1.DebitRequest;
import com.example.betting.proto.wallet.v1.GetBalanceRequest;
import com.example.betting.proto.wallet.v1.GetBalanceResponse;
import com.example.betting.proto.wallet.v1.TxnResponse;
import com.example.betting.proto.wallet.v1.WalletServiceGrpc;
import com.example.betting.wallet.service.WalletExceptions.AccountNotFoundException;
import com.example.betting.wallet.service.WalletExceptions.InsufficientFundsException;
import com.example.betting.wallet.service.WalletService;
import com.example.betting.wallet.service.WalletService.TxnResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class WalletGrpcService extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;
    private final MeterRegistry meter;

    public WalletGrpcService(WalletService walletService, MeterRegistry meter) {
        this.walletService = walletService;
        this.meter = meter;
    }

    @Override
    public void debit(DebitRequest request, StreamObserver<TxnResponse> responseObserver) {
        handle(responseObserver, () -> {
            try {
                TxnResult result = walletService.debit(
                        request.getUserId(),
                        request.getAmount().getAmountMinor(),
                        request.getIdempotencyKey(),
                        request.getReference());
                meter.counter("wallet.debit", "result", "ok").increment();
                return txnResponse(result);
            } catch (InsufficientFundsException ex) {
                // Wallet 실패율 메트릭 (Grafana)
                meter.counter("wallet.debit", "result", "insufficient").increment();
                throw ex;
            }
        });
    }

    @Override
    public void credit(CreditRequest request, StreamObserver<TxnResponse> responseObserver) {
        handle(responseObserver, () -> {
            TxnResult result = walletService.credit(
                    request.getUserId(),
                    request.getAmount().getAmountMinor(),
                    request.getIdempotencyKey(),
                    request.getReference());
            return txnResponse(result);
        });
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        try {
            TxnResult result = walletService.getBalance(request.getUserId());
            responseObserver.onNext(GetBalanceResponse.newBuilder()
                    .setUserId(result.userId())
                    .setBalance(money(result))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    private void handle(StreamObserver<TxnResponse> observer, java.util.function.Supplier<TxnResponse> action) {
        try {
            observer.onNext(action.get());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            observer.onError(toStatus(ex).asRuntimeException());
        }
    }

    private static TxnResponse txnResponse(TxnResult result) {
        return TxnResponse.newBuilder()
                .setUserId(result.userId())
                .setBalance(money(result))
                .setLedgerEntryId(result.ledgerEntryId())
                .build();
    }

    private static Money money(TxnResult result) {
        return Money.newBuilder()
                .setCurrency(result.currency())
                .setAmountMinor(result.balanceMinor())
                .build();
    }

    private static Status toStatus(RuntimeException ex) {
        if (ex instanceof AccountNotFoundException) {
            return Status.NOT_FOUND.withDescription(ex.getMessage());
        }
        if (ex instanceof InsufficientFundsException) {
            return Status.FAILED_PRECONDITION.withDescription(ex.getMessage());
        }
        return Status.INTERNAL.withDescription(ex.getMessage());
    }
}
