package com.emmanuelfinance.shared.kafka;

public interface KafkaFailureHandler {

    void onFailure(String topic, Object event);
}
