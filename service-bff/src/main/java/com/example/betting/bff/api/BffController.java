package com.example.betting.bff.api;

import com.example.betting.proto.betting.v1.BetSlip;
import com.example.betting.proto.betting.v1.BettingServiceGrpc;
import com.example.betting.proto.betting.v1.ListBetsRequest;
import com.example.betting.proto.betting.v1.PlaceBetRequest;
import com.example.betting.proto.common.v1.Money;
import com.example.betting.proto.event.v1.Event;
import com.example.betting.proto.event.v1.EventServiceGrpc;
import com.example.betting.proto.event.v1.ListEventsRequest;
import com.example.betting.proto.wallet.v1.GetBalanceRequest;
import com.example.betting.proto.wallet.v1.GetBalanceResponse;
import com.example.betting.proto.wallet.v1.WalletServiceGrpc;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 화면용 REST 진입점. 각 요청에서 백엔드 gRPC 를 호출·조합한다.
 * (Browser → BFF(HTTP) → 백엔드(gRPC) 가 하나의 분산 trace 로 이어진다.)
 */
@RestController
@RequestMapping("/api")
public class BffController {

    private final EventServiceGrpc.EventServiceBlockingStub eventStub;
    private final BettingServiceGrpc.BettingServiceBlockingStub bettingStub;
    private final WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    public BffController(EventServiceGrpc.EventServiceBlockingStub eventStub,
                         BettingServiceGrpc.BettingServiceBlockingStub bettingStub,
                         WalletServiceGrpc.WalletServiceBlockingStub walletStub) {
        this.eventStub = eventStub;
        this.bettingStub = bettingStub;
        this.walletStub = walletStub;
    }

    /** 한 화면 = 잔액 + 경기목록 + 내베팅 (백엔드 3콜을 조합). */
    @GetMapping("/dashboard")
    public DashboardDto dashboard(@RequestParam(defaultValue = "u1") String userId) {
        GetBalanceResponse balance = walletStub.getBalance(
                GetBalanceRequest.newBuilder().setUserId(userId).build());
        List<Event> events = eventStub.listEvents(ListEventsRequest.getDefaultInstance()).getEventsList();
        List<BetSlip> bets = bettingStub.listBets(
                ListBetsRequest.newBuilder().setUserId(userId).build()).getBetsList();

        return new DashboardDto(
                userId,
                money(balance.getBalance()),
                events.stream().map(BffController::toDto).toList(),
                bets.stream().map(BffController::toDto).toList());
    }

    @PostMapping("/bets")
    public BetDto placeBet(@RequestBody PlaceBetBody body) {
        BetSlip bet = bettingStub.placeBet(PlaceBetRequest.newBuilder()
                .setUserId(body.userId())
                .setEventId(body.eventId())
                .setMarketId(body.marketId())
                .setSelectionId(body.selectionId())
                .setStake(Money.newBuilder().setCurrency("KRW").setAmountMinor(body.stakeMinor()))
                .build()).getBet();
        return toDto(bet);
    }

    /** 하위 gRPC 에러를 사용자 친화적 JSON + HTTP 상태로 변환. */
    @ExceptionHandler(StatusRuntimeException.class)
    ResponseEntity<ApiError> onGrpcError(StatusRuntimeException ex) {
        HttpStatus status = switch (ex.getStatus().getCode()) {
            case FAILED_PRECONDITION, INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        String message = ex.getStatus().getDescription() != null
                ? ex.getStatus().getDescription() : ex.getStatus().getCode().name();
        return ResponseEntity.status(status).body(new ApiError(message));
    }

    private static EventDto toDto(Event e) {
        return new EventDto(e.getId(), e.getName(), e.getSport(), e.getStatus().name(),
                e.getMarketsList().stream().map(m -> new MarketDto(
                        m.getId(), m.getName(),
                        m.getSelectionsList().stream()
                                .map(s -> new SelectionDto(s.getId(), s.getName(), s.getOdds()))
                                .toList())).toList());
    }

    private static BetDto toDto(BetSlip b) {
        return new BetDto(b.getId(), b.getEventId(), b.getMarketId(), b.getSelectionId(),
                money(b.getStake()), b.getOdds(), b.getStatus().name(), money(b.getPayout()));
    }

    private static MoneyDto money(Money m) {
        return new MoneyDto(m.getCurrency(), m.getAmountMinor());
    }

    record ApiError(String message) {
    }
}
