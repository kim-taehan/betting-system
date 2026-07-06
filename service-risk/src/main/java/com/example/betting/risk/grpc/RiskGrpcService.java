package com.example.betting.risk.grpc;

import com.example.betting.proto.common.v1.Money;
import com.example.betting.proto.risk.v1.CheckBetRequest;
import com.example.betting.proto.risk.v1.CheckBetResponse;
import com.example.betting.proto.risk.v1.GetExposureRequest;
import com.example.betting.proto.risk.v1.GetExposureResponse;
import com.example.betting.proto.risk.v1.RiskServiceGrpc;
import com.example.betting.risk.service.RiskService;
import com.example.betting.risk.service.RiskService.CheckResult;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class RiskGrpcService extends RiskServiceGrpc.RiskServiceImplBase {

    private final RiskService riskService;

    public RiskGrpcService(RiskService riskService) {
        this.riskService = riskService;
    }

    @Override
    public void checkBet(CheckBetRequest request, StreamObserver<CheckBetResponse> responseObserver) {
        CheckResult result = riskService.checkBet(request.getUserId(), request.getStake().getAmountMinor());
        responseObserver.onNext(CheckBetResponse.newBuilder()
                .setApproved(result.approved())
                .setReason(result.reason())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getExposure(GetExposureRequest request, StreamObserver<GetExposureResponse> responseObserver) {
        long total = riskService.userExposure(request.getUserId());
        responseObserver.onNext(GetExposureResponse.newBuilder()
                .setUserId(request.getUserId())
                .setTotalStaked(Money.newBuilder().setCurrency("KRW").setAmountMinor(total))
                .build());
        responseObserver.onCompleted();
    }
}
