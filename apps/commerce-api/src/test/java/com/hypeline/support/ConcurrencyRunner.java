package com.hypeline.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 여러 스레드를 한 지점에 모았다가 동시에 푼다.
 *
 * <p>스레드를 만들자마자 실행하면 생성 순서대로 늘어져 실제 경합이 재현되지 않는다.
 * 시작 신호를 기다리게 해두고 한꺼번에 풀어야 같은 행을 동시에 노리는 상황이 만들어진다.
 */
public final class ConcurrencyRunner {

    private ConcurrencyRunner() {
    }

    public record Result(int successCount, int failureCount) {
        public int total() {
            return successCount + failureCount;
        }
    }

    /**
     * @param threads 동시에 실행할 스레드 수
     * @param action  각 스레드가 수행할 작업. true 를 반환하면 성공으로 센다
     */
    public static Result runConcurrently(int threads, IntPredicate action) {
        // 풀 크기는 반드시 작업 수 이상이어야 한다.
        // 풀이 더 작으면 먼저 실행된 작업들이 시작 신호를 기다리며 모든 스레드를 점유하고,
        // 나머지 작업은 실행조차 되지 못해 준비 완료 신호가 영원히 오지 않는다.
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        try {
            for (int i = 0; i < threads; i++) {
                int index = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (action.test(index)) {
                            success.incrementAndGet();
                        } else {
                            failure.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failure.incrementAndGet();
                    } catch (Exception e) {
                        failure.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(30, TimeUnit.SECONDS))
                .as("스레드 %d개가 모두 시작 지점에 도달", threads).isTrue();
            start.countDown();
            assertThat(done.await(120, TimeUnit.SECONDS)).as("작업 완료").isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었다.", e);
        } finally {
            executor.shutdownNow();
        }

        return new Result(success.get(), failure.get());
    }
}
