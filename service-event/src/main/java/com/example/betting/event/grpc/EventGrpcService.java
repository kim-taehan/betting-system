package com.example.betting.event.grpc;

import com.example.betting.event.domain.EventStatus;
import com.example.betting.event.service.EventQueryService;
import com.example.betting.event.service.EventSettleService;
import com.example.betting.proto.event.v1.Event;
import com.example.betting.proto.event.v1.EventServiceGrpc;
import com.example.betting.proto.event.v1.GetOddsRequest;
import com.example.betting.proto.event.v1.GetOddsResponse;
import com.example.betting.proto.event.v1.ListEventsRequest;
import com.example.betting.proto.event.v1.ListEventsResponse;
import com.example.betting.proto.event.v1.OddsUpdate;
import com.example.betting.proto.event.v1.SettleEventRequest;
import com.example.betting.proto.event.v1.SettleEventResponse;
import com.example.betting.proto.event.v1.StreamOddsRequest;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class EventGrpcService extends EventServiceGrpc.EventServiceImplBase {

    private final EventQueryService queryService;
    private final OddsBroadcaster broadcaster;
    private final EventSettleService settleService;

    public EventGrpcService(EventQueryService queryService, OddsBroadcaster broadcaster,
                            EventSettleService settleService) {
        this.queryService = queryService;
        this.broadcaster = broadcaster;
        this.settleService = settleService;
    }

    @Override
    public void listEvents(ListEventsRequest request, StreamObserver<ListEventsResponse> responseObserver) {
        EventStatus filter = toDomainStatus(request);
        ListEventsResponse response = ListEventsResponse.newBuilder()
                .addAllEvents(queryService.listEvents(filter))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getOdds(GetOddsRequest request, StreamObserver<GetOddsResponse> responseObserver) {
        queryService.getEvent(request.getEventId()).ifPresentOrElse(
                event -> {
                    responseObserver.onNext(GetOddsResponse.newBuilder().setEvent(event).build());
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(Status.NOT_FOUND
                        .withDescription("event not found: " + request.getEventId())
                        .asRuntimeException()));
    }

    @Override
    public void settleEvent(SettleEventRequest request, StreamObserver<SettleEventResponse> responseObserver) {
        try {
            settleService.settle(request.getEventId(), request.getMarketId(), request.getWinningSelectionId());
            Event event = queryService.getEvent(request.getEventId())
                    .orElseThrow(() -> new IllegalArgumentException("event not found: " + request.getEventId()));
            responseObserver.onNext(SettleEventResponse.newBuilder().setEvent(event).build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void streamOdds(StreamOddsRequest request, StreamObserver<OddsUpdate> responseObserver) {
        // server-streaming: 스트림을 열어두고 배당 변동 시마다 밀어준다.
        ServerCallStreamObserver<OddsUpdate> serverObserver =
                (ServerCallStreamObserver<OddsUpdate>) responseObserver;
        OddsBroadcaster.Subscriber subscriber = broadcaster.register(request.getEventId(), serverObserver);
        serverObserver.setOnCancelHandler(() -> broadcaster.remove(subscriber));
    }

    /** UNSPECIFIED(=0) 이면 전체(null), 아니면 해당 상태로 필터. */
    private static EventStatus toDomainStatus(ListEventsRequest request) {
        var protoStatus = request.getStatus();
        return switch (protoStatus) {
            case SCHEDULED -> EventStatus.SCHEDULED;
            case LIVE -> EventStatus.LIVE;
            case SETTLED -> EventStatus.SETTLED;
            default -> null;
        };
    }
}
