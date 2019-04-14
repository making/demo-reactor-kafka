package com.example.demoreactorkafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

@Controller
public class AccessLogRouter {

    private final Flux<AccessLog> accessLogs;

    public AccessLogRouter(ObjectMapper objectMapper, KafkaProps props) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        consumerProps.put(GROUP_ID_CONFIG, props.getGroupId());
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        consumerProps.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        consumerProps.put(SASL_MECHANISM, "SCRAM-SHA-256");
        consumerProps.put(SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";", props.getUsername(),
            props.getPassword()));

        ReceiverOptions<byte[], byte[]> receiverOptions =
            ReceiverOptions.<byte[], byte[]>create(consumerProps)
                .subscription(Collections.singleton("request-log"));

        this.accessLogs =
            KafkaReceiver.create(receiverOptions)
                .receive()
                .doOnNext(r -> r.receiverOffset().acknowledge())
                .map(r -> {
                    try {
                        return objectMapper.readValue(r.value(), AccessLog.class);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .log("logs")
                .publish()
                .autoConnect()
                .share();
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
            .GET("/", req -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(this.accessLogs, AccessLog.class))
            .build();
    }
}
