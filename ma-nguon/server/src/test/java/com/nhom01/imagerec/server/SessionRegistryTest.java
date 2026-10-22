package com.nhom01.imagerec.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    @Test
    void registerAssignsIncreasingIds() {
        SessionRegistry registry = new SessionRegistry();

        SessionInfo first = registry.register("a", "/1.1.1.1");
        SessionInfo second = registry.register("b", "/2.2.2.2");

        assertThat(second.id()).isGreaterThan(first.id());
        assertThat(registry.snapshot()).extracting(SessionInfo::clientName).containsExactly("a", "b");
    }

    @Test
    void recordImageIncrementsCounterAndRemoveDeletes() {
        SessionRegistry registry = new SessionRegistry();
        int id = registry.register("a", "/1.1.1.1").id();

        registry.recordImage(id);
        registry.recordImage(id);

        assertThat(registry.snapshot().get(0).imagesProcessed()).isEqualTo(2);
        registry.remove(id);
        assertThat(registry.snapshot()).isEmpty();
    }

    @Test
    void listenersAreNotifiedOnEveryChange() {
        SessionRegistry registry = new SessionRegistry();
        List<Integer> sizes = new ArrayList<>();
        registry.addListener(sessions -> sizes.add(sessions.size()));

        int id = registry.register("a", "/1.1.1.1").id();
        registry.recordImage(id);
        registry.remove(id);

        assertThat(sizes).containsExactly(1, 1, 0);
    }
}
