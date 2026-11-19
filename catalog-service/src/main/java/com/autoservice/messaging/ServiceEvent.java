package com.autoservice.messaging;

public record ServiceEvent(String eventId, String action, Long serviceId, String name) {
}
