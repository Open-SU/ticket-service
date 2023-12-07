package com.open.su.controllers.models;

import java.util.UUID;

public record UpdateStatusErrorMessage(Throwable throwable, Type type, UUID ticketId) {
    public enum Type {
        TICKET_NOT_FOUND,
        INVALID_STATUS,
        UNEXPECTED_ERROR
    }
}
