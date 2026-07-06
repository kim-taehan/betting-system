package com.example.betting.bff.api;

import java.util.List;

/** 화면(JSON)용 DTO 모음. proto 메시지를 브라우저 친화적 형태로 변환한다. */
final class ApiModels {
    private ApiModels() {
    }
}

record MoneyDto(String currency, long amountMinor) {
}

record SelectionDto(String id, String name, double odds) {
}

record MarketDto(String id, String name, List<SelectionDto> selections) {
}

record EventDto(String id, String name, String sport, String status, List<MarketDto> markets) {
}

record BetDto(String id, String eventId, String marketId, String selectionId,
              MoneyDto stake, double odds, String status, MoneyDto payout) {
}

/** 한 화면에 필요한 데이터를 한 번에 조합한 BFF 응답. */
record DashboardDto(String userId, MoneyDto balance, List<EventDto> events, List<BetDto> bets) {
}

/** 베팅 접수 요청 바디. */
record PlaceBetBody(String userId, String eventId, String marketId, String selectionId, long stakeMinor) {
}
