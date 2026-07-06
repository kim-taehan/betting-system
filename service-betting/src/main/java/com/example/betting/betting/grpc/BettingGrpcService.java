package com.example.betting.betting.grpc;

import com.example.betting.betting.domain.BetSlipEntity;
import com.example.betting.betting.service.BetRejectedException;
import com.example.betting.betting.service.BettingService;
import com.example.betting.proto.betting.v1.BetSlip;
import com.example.betting.proto.betting.v1.BetStatus;
import com.example.betting.proto.betting.v1.BettingServiceGrpc;
import com.example.betting.proto.betting.v1.GetBetRequest;
import com.example.betting.proto.betting.v1.GetBetResponse;
import com.example.betting.proto.betting.v1.ListBetsRequest;
import com.example.betting.proto.betting.v1.ListBetsResponse;
import com.example.betting.proto.betting.v1.PlaceBetRequest;
import com.example.betting.proto.betting.v1.PlaceBetResponse;
import com.example.betting.proto.common.v1.Money;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class BettingGrpcService extends BettingServiceGrpc.BettingServiceImplBase {

    private final BettingService bettingService;

    public BettingGrpcService(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @Override
    public void placeBet(PlaceBetRequest request, StreamObserver<PlaceBetResponse> responseObserver) {
        try {
            BetSlipEntity bet = bettingService.placeBet(
                    request.getUserId(),
                    request.getEventId(),
                    request.getMarketId(),
                    request.getSelectionId(),
                    request.getStake().getCurrency(),
                    request.getStake().getAmountMinor());
            responseObserver.onNext(PlaceBetResponse.newBuilder().setBet(toProto(bet)).build());
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    @Override
    public void getBet(GetBetRequest request, StreamObserver<GetBetResponse> responseObserver) {
        BetSlipEntity bet = bettingService.getBet(request.getBetId());
        if (bet == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("bet not found: " + request.getBetId()).asRuntimeException());
            return;
        }
        responseObserver.onNext(GetBetResponse.newBuilder().setBet(toProto(bet)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void listBets(ListBetsRequest request, StreamObserver<ListBetsResponse> responseObserver) {
        ListBetsResponse.Builder builder = ListBetsResponse.newBuilder();
        bettingService.listBets(request.getUserId()).forEach(bet -> builder.addBets(toProto(bet)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private static BetSlip toProto(BetSlipEntity bet) {
        return BetSlip.newBuilder()
                .setId(bet.getId())
                .setUserId(bet.getUserId())
                .setEventId(bet.getEventId())
                .setMarketId(bet.getMarketId())
                .setSelectionId(bet.getSelectionId())
                .setStake(money(bet.getCurrency(), bet.getStakeMinor()))
                .setOdds(bet.getOdds())
                .setStatus(BetStatus.valueOf(bet.getStatus().name()))
                .setPayout(money(bet.getCurrency(), bet.getPayoutMinor()))
                .setPlacedAtEpochMs(bet.getPlacedAt().toEpochMilli())
                .build();
    }

    private static Money money(String currency, long amountMinor) {
        return Money.newBuilder().setCurrency(currency).setAmountMinor(amountMinor).build();
    }

    /** Wallet 등 하위 호출의 gRPC 에러는 그대로 전파, 검증 실패는 INVALID_ARGUMENT. */
    private static Status toStatus(RuntimeException ex) {
        if (ex instanceof StatusRuntimeException sre) {
            return sre.getStatus();
        }
        if (ex instanceof BetRejectedException) {
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
        }
        return Status.INTERNAL.withDescription(ex.getMessage());
    }
}
