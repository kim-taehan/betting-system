package com.example.betting.bff.grpc;

import com.example.betting.proto.betting.v1.BettingServiceGrpc;
import com.example.betting.proto.event.v1.EventServiceGrpc;
import com.example.betting.proto.wallet.v1.WalletServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/** BFF 는 화면 요청을 위해 Event/Betting/Wallet 을 gRPC 로 호출·조합한다. */
@Configuration
public class GrpcClientConfig {

    @Bean
    EventServiceGrpc.EventServiceBlockingStub eventStub(GrpcChannelFactory channels) {
        return EventServiceGrpc.newBlockingStub(channels.createChannel("event"));
    }

    @Bean
    BettingServiceGrpc.BettingServiceBlockingStub bettingStub(GrpcChannelFactory channels) {
        return BettingServiceGrpc.newBlockingStub(channels.createChannel("betting"));
    }

    @Bean
    WalletServiceGrpc.WalletServiceBlockingStub walletStub(GrpcChannelFactory channels) {
        return WalletServiceGrpc.newBlockingStub(channels.createChannel("wallet"));
    }
}
