package com.nhom01.imagerec.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class ServerConfigTest {

    @Test
    void defaultsWhenNothingGiven() {
        ServerConfig config = ServerConfig.parse(new Properties(), new String[0]);

        assertThat(config).isEqualTo(new ServerConfig(5000, "fake", true, 20, null, null));
    }

    @Test
    void argumentsOverrideProperties() {
        Properties properties = new Properties();
        properties.setProperty("port", "6000");
        properties.setProperty("detector", "djl");

        ServerConfig config = ServerConfig.parse(properties,
                new String[] {"--port=7000", "--gui=false", "--maxClients=3"});

        assertThat(config).isEqualTo(new ServerConfig(7000, "djl", false, 3, null, null));
    }

    @Test
    void modelArgumentIsKept() {
        ServerConfig config = ServerConfig.parse(new Properties(), new String[] {"--model=C:/m/best.onnx"});

        assertThat(config.model()).isEqualTo("C:/m/best.onnx");
    }

    @Test
    void ortThreadsIsOptionalAndParsed() {
        assertThat(ServerConfig.parse(new Properties(), new String[0]).ortThreads()).isNull();
        assertThat(ServerConfig.parse(new Properties(), new String[] {"--ortThreads=2"}).ortThreads()).isEqualTo(2);
    }

    @Test
    void invalidOrtThreadsIsRejected() {
        assertThatThrownBy(() -> ServerConfig.parse(new Properties(), new String[] {"--ortThreads=0"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ortThreads");
    }

    @Test
    void invalidPortIsRejectedWithClearMessage() {
        assertThatThrownBy(() -> ServerConfig.parse(new Properties(), new String[] {"--port=abc"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @Test
    void unknownDetectorIsRejectedWithClearMessage() {
        assertThatThrownBy(() -> ServerConfig.parse(new Properties(), new String[] {"--detector=xyz"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("detector");
    }
}
