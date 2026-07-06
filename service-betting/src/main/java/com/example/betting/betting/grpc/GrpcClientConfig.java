package com.example.betting.betting.grpc;

import com.example.betting.proto.event.v1.EventServiceGrpc;
import com.example.betting.proto.risk.v1.RiskServiceGrpc;
import com.example.betting.proto.wallet.v1.WalletServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Betting 은 오케스트레이터로서 Event/Wallet 을 gRPC 로 동기 호출한다.
 * application.yml 의 spring.grpc.client.channels.{event,wallet} 채널로 blocking 스텁을 만든다.
 */
@Configuration
public class GrpcClientConfig {

    @Bean
    EventServiceGrpc.EventServiceBlockingStub eventStub(GrpcChannelFactory channels) {
        return EventServiceGrpc.newBlockingStub(channels.createChannel("event"));
    }

    @Bean
    WalletServiceGrpc.WalletServiceBlockingStub walletStub(GrpcChannelFactory channels) {
        return WalletServiceGrpc.newBlockingStub(channels.createChannel("wallet"));
    }

    @Bean
    RiskServiceGrpc.RiskServiceBlockingStub riskStub(GrpcChannelFactory channels) {
        return RiskServiceGrpc.newBlockingStub(channels.createChannel("risk"));
    }
}
