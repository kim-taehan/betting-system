package com.example.betting.ops.grpc;

import com.example.betting.ops.domain.AlertEntity;
import com.example.betting.ops.service.OpsQueryService;
import com.example.betting.proto.ops.v1.Alert;
import com.example.betting.proto.ops.v1.GetSystemStatusRequest;
import com.example.betting.proto.ops.v1.GetSystemStatusResponse;
import com.example.betting.proto.ops.v1.ListAlertsRequest;
import com.example.betting.proto.ops.v1.ListAlertsResponse;
import com.example.betting.proto.ops.v1.OpsServiceGrpc;
import com.example.betting.proto.ops.v1.ServiceStatus;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpsGrpcService extends OpsServiceGrpc.OpsServiceImplBase {

    private final OpsQueryService query;
    private final BackendHealthChecker healthChecker;

    public OpsGrpcService(OpsQueryService query, BackendHealthChecker healthChecker) {
        this.query = query;
        this.healthChecker = healthChecker;
    }

    @Override
    public void getSystemStatus(GetSystemStatusRequest request,
                                StreamObserver<GetSystemStatusResponse> responseObserver) {
        Map<String, Long> c = query.counters();
        GetSystemStatusResponse.Builder builder = GetSystemStatusResponse.newBuilder()
                .setBetsPlaced(c.getOrDefault("bets_placed", 0L))
                .setBetsSettled(c.getOrDefault("bets_settled", 0L))
                .setBetsWon(c.getOrDefault("bets_won", 0L))
                .setBetsLost(c.getOrDefault("bets_lost", 0L))
                .setTotalStakedMinor(c.getOrDefault("total_staked_minor", 0L))
                .setTotalPayoutMinor(c.getOrDefault("total_payout_minor", 0L))
                .setRiskAlerts(c.getOrDefault("risk_alerts", 0L));

        healthChecker.checkAll().forEach(h -> builder.addServices(
                ServiceStatus.newBuilder().setName(h.name()).setStatus(h.status()).build()));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listAlerts(ListAlertsRequest request, StreamObserver<ListAlertsResponse> responseObserver) {
        ListAlertsResponse.Builder builder = ListAlertsResponse.newBuilder();
        for (AlertEntity a : query.recentAlerts(request.getLimit())) {
            builder.addAlerts(Alert.newBuilder()
                    .setType(a.getType())
                    .setSubjectId(a.getSubjectId() == null ? "" : a.getSubjectId())
                    .setMessage(a.getMessage() == null ? "" : a.getMessage())
                    .setTimestampEpochMs(a.getCreatedAt().toEpochMilli())
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
