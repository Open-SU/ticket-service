package com.open.su;

import com.open.su.exceptions.TicketServiceException;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@WithTransaction
public class TicketService {

    private static final Logger LOGGER = Logger.getLogger(TicketService.class);

    /**
     * List tickets with pagination, sorting, and filtering
     *
     * @param page page number and size
     * @param sort sort by field and direction
     * @return a {@link Uni} with the list of tickets (with minimal information)
     */
    public Uni<List<Ticket>> listTickets(Page page, Sort sort, Tuple2<String, Parameters>... params) {
        LOGGER.trace("Listing tickets with page " + page + " and sort " + sort);
        final String scope = "list tickets";

        PanacheQuery<Ticket> query = Ticket.<Ticket>findAll(sort).page(page);
        for (Tuple2<String, Parameters> param : params) {
            query = query.filter(param.getItem1(), param.getItem2());
        }

        return query.list()
                .onFailure().transform(t -> {
                    String message = "Failed to list tickets";
                    LOGGER.error(scope + ": " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                });
    }

    /**
     * Get ticket details
     *
     * @param id the id of the ticket
     * @return a {@link Uni} with the ticket details
     */
    public Uni<Ticket> getTicketDetails(UUID id) {
        LOGGER.trace("Getting ticket details for ticket with id " + id);
        final String scope = "get ticket details";

        return findTicketOrFail(id, scope);
    }

    /**
     * Attend an event
     *
     * @param eventId    the id of the event
     * @param attendeeId the id of the attendee
     * @return a {@link Uni} with the id of the ticket
     */
    public Uni<UUID> attendEvent(UUID eventId, UUID attendeeId) {
        LOGGER.trace("Attend event with id " + eventId + " for user with id " + attendeeId);
        final String scope = "attend event";

        return Ticket.<Ticket>find("eventId=?1 AND attendeeId=?2", eventId, attendeeId).firstResult()
                .onFailure().transform(t -> {
                    String message = "Failed to get ticket for event with id " + eventId;
                    LOGGER.error(scope + ": " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNotNull().transformToUni(existingTicket -> {
                    if (!existingTicket.id.equals(eventId)) {
                        String message = "Ticket for event with id " + eventId + " and attendee with id " + attendeeId + " already exists";
                        LOGGER.debug(scope + ": " + message);
                        return Uni.createFrom().failure(TicketServiceException.CONFLICT.withMessage(message));
                    }
                    return Uni.createFrom().nullItem();
                })
                .onItem().transformToUni(ticket -> {
                    Ticket newTicket = new Ticket();
                    newTicket.eventId = eventId;
                    newTicket.attendeeId = attendeeId;
                    return persistTicketOrFail(newTicket, scope);
                })
                .onItem().transform(e -> e == null ? null : e.id);
    }

    /**
     * Select a bus for a ticket
     *
     * @param id    the id of the ticket
     * @param busId the id of the bus
     * @return a {@link Uni} with the id of the ticket
     */
    public Uni<UUID> selectBus(UUID id, UUID busId) {
        LOGGER.trace("Select bus with id " + busId + " for ticket with id " + id);
        final String scope = "select bus";

        return findTicketOrFail(id, scope)
                .onItem().transformToUni(existingTicket -> {
                    existingTicket.busId = busId;
                    return persistTicketOrFail(existingTicket, scope);
                })
                .onItem().transform(e -> e == null ? null : e.id);
    }

    /**
     * Update the status of a ticket
     *
     * @param id     the id of the ticket
     * @param status the new status of the ticket
     * @return a {@link Uni} with the id of the ticket
     */
    public Uni<Tuple2<UUID, Ticket.Status>> updateTicketStatus(UUID id, Ticket.Status status) {
        LOGGER.trace("Update ticket status to " + status + " for ticket with id " + id);
        final String scope = "update ticket status";

        return findTicketOrFail(id, scope)
                .onItem().transformToUni(existingTicket -> {
                    if (status.order != Math.abs(existingTicket.status.order) + 1 || existingTicket.status.order < 0) {
                        String message = "Cannot update ticket status from " + existingTicket.status + " to " + status;
                        LOGGER.debug(scope + ": " + message);
                        return Uni.createFrom().failure(TicketServiceException.INVALID_ARGUMENT.withMessage(message));
                    }
                    existingTicket.status = status;
                    return persistTicketOrFail(existingTicket, scope);
                })
                .onItem().transform(e -> e == null ? null : Tuple2.of(e.id, e.status));
    }

    /**
     * Update a ticket
     *
     * @param ticket the ticket to update
     * @return a {@link Uni} with the id of the updated ticket
     */
    public Uni<UUID> updateTicket(Ticket ticket) {
        LOGGER.trace("Updating ticket " + ticket);
        final String scope = "update ticket";

        return findTicketOrFail(ticket.id, scope)
                .onItem().transformToUni(existingTicket -> persistTicketOrFail(existingTicket.update(ticket), scope))
                .onItem().transform(e -> e == null ? null : e.id);
    }

    /**
     * Delete a ticket
     *
     * @param id the id of the ticket to delete
     * @return a {@link Uni} of Void
     */
    public Uni<Void> deleteTicket(UUID id) {
        LOGGER.trace("Deleting ticket with id " + id);
        final String scope = "delete ticket";

        return findTicketOrFail(id, scope)
                .onItem().transformToUni(existingTicket ->
                        existingTicket.delete()
                                .onFailure().transform(t -> {
                                    String message = "Failed to delete ticket with id " + id;
                                    LOGGER.error(scope + ": " + message, t);
                                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                                })
                                .onItem().invoke(() -> LOGGER.debug(scope + ": " + "Deleted ticket with id " + id)));
    }

    /**
     * Find a ticket by id or fail
     *
     * @param id    the id of the ticket
     * @param scope the context in which the find is performed (for logging purposes)
     * @return a {@link Uni} with the ticket, otherwise a failed {@link Uni}
     */
    Uni<Ticket> findTicketOrFail(UUID id, String scope) {
        return Ticket.<Ticket>findById(id)
                .onFailure().transform(t -> {
                    String message = "Failed to get ticket with id " + id;
                    LOGGER.error(scope + ": " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNull().failWith(() -> {
                    String message = "Ticket with id " + id + " does not exist";
                    LOGGER.debug(scope + ": " + message);
                    return TicketServiceException.NOT_FOUND.withMessage(message);
                });
    }

    /**
     * Persist a ticket or fail
     *
     * @param ticket the ticket to persist
     * @param scope  the context in which the persist is performed (for logging purposes)
     * @return a {@link Uni} with the persisted ticket, otherwise a failed {@link Uni}
     */
    Uni<Ticket> persistTicketOrFail(Ticket ticket, String scope) {
        return ticket.<Ticket>persist()
                .onFailure().transform(t -> {
                    String message = "Failed to persist stock for ticket with id " + ticket.id;
                    LOGGER.error(scope + ": " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNotNull().invoke(existingTicket -> LOGGER.debug(scope + ": " + "Persisted ticket with id " + existingTicket.id));
    }
}
