package com.example.betting.event.grpc;

import com.example.betting.proto.event.v1.OddsUpdate;
import io.grpc.stub.ServerCallStreamObserver;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * StreamOdds 구독자를 관리하고 배당 변동을 밀어준다(server-streaming).
 * 구독자는 event_id 로 필터링할 수 있다(빈 문자열이면 전체 수신).
 */
@Component
public class OddsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(OddsBroadcaster.class);

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    /** 구독자 record. observer 는 스레드 안전하지 않으므로 전송 시 동기화한다. */
    record Subscriber(String eventIdFilter, ServerCallStreamObserver<OddsUpdate> observer) {

        boolean matches(String eventId) {
            return eventIdFilter.isEmpty() || eventIdFilter.equals(eventId);
        }
    }

    Subscriber register(String eventIdFilter, ServerCallStreamObserver<OddsUpdate> observer) {
        Subscriber subscriber = new Subscriber(eventIdFilter, observer);
        subscribers.add(subscriber);
        log.info("StreamOdds 구독 등록 (filter='{}', 현재 {}명)", eventIdFilter, subscribers.size());
        return subscriber;
    }

    void remove(Subscriber subscriber) {
        if (subscribers.remove(subscriber)) {
            log.info("StreamOdds 구독 해제 (현재 {}명)", subscribers.size());
        }
    }

    /** 매칭되는 모든 구독자에게 배당 변동 1건을 전송. */
    public void broadcast(OddsUpdate update) {
        for (Subscriber subscriber : subscribers) {
            if (!subscriber.matches(update.getEventId())) {
                continue;
            }
            ServerCallStreamObserver<OddsUpdate> observer = subscriber.observer();
            try {
                if (observer.isCancelled()) {
                    remove(subscriber);
                    continue;
                }
                synchronized (observer) {
                    observer.onNext(update);
                }
            } catch (RuntimeException ex) {
                log.warn("구독자 전송 실패, 제거함: {}", ex.getMessage());
                remove(subscriber);
            }
        }
    }
}
