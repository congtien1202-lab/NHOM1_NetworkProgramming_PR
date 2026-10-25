package com.nhom01.imagerec.server.detect;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhom01.imagerec.common.Detection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MonkeyGuardTest {

    private static Detection box(String label, double confidence, double x, double y, double w, double h) {
        return new Detection(label, confidence, x, y, w, h);
    }

    private static final Detection MONKEY = box("monkey", 0.8, 0.2, 0.2, 0.4, 0.4);

    @Test
    void monkeyBelowOwnThresholdIsDropped() {
        AtomicInteger calls = new AtomicInteger();
        List<Detection> result = MonkeyGuard.apply(
                List.of(box("monkey", 0.49, 0.2, 0.2, 0.4, 0.4), box("dog", 0.4, 0, 0, 0.1, 0.1)),
                () -> {
                    calls.incrementAndGet();
                    return List.of();
                });

        assertThat(result).extracting(Detection::label).containsExactly("dog");
        assertThat(calls).hasValue(0);
    }

    @Test
    void monkeyOverlappingGuardPersonIsVetoed() {
        Detection person = box("person", 0.25, 0.2, 0.2, 0.4, 0.38);

        List<Detection> result = MonkeyGuard.apply(List.of(MONKEY, box("dog", 0.4, 0, 0, 0.1, 0.1)),
                () -> List.of(person));

        assertThat(result).extracting(Detection::label).containsExactly("dog");
    }

    @Test
    void monkeyKeptWhenGuardPersonOverlapIsLow() {
        Detection person = box("person", 0.9, 0.5, 0.5, 0.4, 0.4);

        List<Detection> result = MonkeyGuard.apply(List.of(MONKEY), () -> List.of(person));

        assertThat(result).containsExactly(MONKEY);
    }

    @Test
    void monkeyKeptWhenGuardPersonIsBelowGuardConfidence() {
        Detection weakPerson = box("person", 0.19, 0.2, 0.2, 0.4, 0.4);

        assertThat(MonkeyGuard.apply(List.of(MONKEY), () -> List.of(weakPerson))).containsExactly(MONKEY);
    }

    @Test
    void guardModelNotCalledWithoutMonkey() {
        AtomicInteger calls = new AtomicInteger();
        List<Detection> input = List.of(box("person", 0.9, 0, 0, 0.5, 0.5), box("monkey", 0.3, 0, 0, 0.5, 0.5));

        List<Detection> result = MonkeyGuard.apply(input, () -> {
            calls.incrementAndGet();
            return List.of();
        });

        assertThat(result).extracting(Detection::label).containsExactly("person");
        assertThat(calls).hasValue(0);
    }

    @Test
    void guardModelCalledOnceForSeveralMonkeys() {
        AtomicInteger calls = new AtomicInteger();
        Detection second = box("monkey", 0.6, 0.7, 0.7, 0.2, 0.2);

        List<Detection> result = MonkeyGuard.apply(List.of(MONKEY, second), () -> {
            calls.incrementAndGet();
            return List.of();
        });

        assertThat(result).containsExactly(MONKEY, second);
        assertThat(calls).hasValue(1);
    }

    @Test
    void withoutGuardModelOnlyThresholdApplies() {
        assertThat(MonkeyGuard.apply(List.of(MONKEY), null)).containsExactly(MONKEY);
    }
}
