package com.open.su.controllers.models;

import com.open.su.Ticket;

import java.util.UUID;

public record UpdateStatusSuccessMessage(UUID ticketId, Ticket.Status status) {
}
