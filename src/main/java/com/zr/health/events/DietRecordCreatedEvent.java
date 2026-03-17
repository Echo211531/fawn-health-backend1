package com.zr.health.events;

public class DietRecordCreatedEvent {
    private final Long userId;

    public DietRecordCreatedEvent(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}