package com.open.su.controllers.models;

import com.open.su.Ticket;

import java.util.UUID;

/**
 * Message received from the message queue.
 *
 * @see com.open.su.controllers.TicketMqpController
 */
public record UpdateStatusMessage(UUID ticketId, Ticket.Status status) {
}
