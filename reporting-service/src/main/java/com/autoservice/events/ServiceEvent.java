package com.autoservice.events;

public record ServiceEvent(String eventId, String action, Long serviceId, String name) {
}
