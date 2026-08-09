package com.project.backend.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 時系列の統合テストで、Spring管理の現在日時を明示的に進めるためのClock。
 */
public final class AdjustableTestClock extends Clock {

    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zone;

    public AdjustableTestClock(Instant initialInstant, ZoneId zone) {
        this.currentInstant = new AtomicReference<>(
                Objects.requireNonNull(initialInstant)
        );
        this.zone = Objects.requireNonNull(zone);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId requestedZone) {
        return new AdjustableTestClock(instant(), requestedZone);
    }

    @Override
    public Instant instant() {
        return currentInstant.get();
    }

    public void setInstant(Instant instant) {
        currentInstant.set(Objects.requireNonNull(instant));
    }

    public void setDate(LocalDate date) {
        setInstant(date.atStartOfDay(zone).toInstant());
    }

    public void advance(Duration duration) {
        currentInstant.updateAndGet(value -> value.plus(duration));
    }
}
