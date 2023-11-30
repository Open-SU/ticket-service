package com.open.su;

import com.open.su.exceptions.TicketServiceException;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@WithTransaction
public class TicketService {

    private static final Logger LOGGER = Logger.getLogger(TicketService.class);

    public Uni<List<Ticket>> listTickets(Page page, Sort sort, @Nullable UUID eventId, @Nullable UUID attendeeId, @Nullable UUID busId) {
        LOGGER.trace("Listing tickets with page " + page + " and sort " + sort);
        PanacheQuery<Ticket> query = Ticket.<Ticket>findAll(sort).page(page);

        if (eventId != null) {
            query = query.filter("eventId", Parameters.with("event_id", eventId));
        }
        if (attendeeId != null) {
            query = query.filter("attendeeId", Parameters.with("attendee_id", attendeeId));
        }
        if (busId != null) {
            query = query.filter("busId", Parameters.with("bus_id", busId));
        }

        return query.list()
                .onFailure().transform(t -> {
                    String message = "Failed to list tickets";
                    LOGGER.error("[" + Method.LIST + "] " + message, t);
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
        return findTicketOrFail(id, Method.DETAILS);
    }

    public Uni<UUID> attendEvent(UUID evendId) {
        //TODO: implement
        return Uni.createFrom().item(UUID.randomUUID());
    }

    public Uni<UUID> shotgunBus(UUID busId) {
        //TODO: implement
        return Uni.createFrom().item(UUID.randomUUID());
    }

    /**
     * Update a ticket
     *
     * @param ticket the ticket to update
     * @return a {@link Uni} with the id of the updated ticket
     */
    public Uni<UUID> updateTicket(Ticket ticket) {
        LOGGER.trace("Updating ticket with id " + ticket.id);
        return findTicketOrFail(ticket.id, Method.UPDATE)
                .onItem().transformToUni(existingTicket -> persistTicketOrFail(ticket, Method.UPDATE)
                        .onItem().transform(updatedTicket -> updatedTicket.id));
    }

    /**
     * Delete a ticket
     *
     * @param id the id of the ticket to delete
     * @return a {@link Uni} of Void
     */
    public Uni<Void> deleteTicket(UUID id) {
        LOGGER.trace("Deleting ticket with id " + id);
        return findTicketOrFail(id, Method.DELETE)
                .onItem().transformToUni(existingTicket ->
                        existingTicket.delete()
                                .onFailure().transform(t -> {
                                    String message = "Failed to delete ticket with id " + id;
                                    LOGGER.error("[" + Method.DELETE + "] " + message, t);
                                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                                })
                                .onItem().invoke(() -> LOGGER.debug("[" + Method.DELETE + "] " + "Deleted ticket with id " + id)));
    }

    public Uni<UUID> inviteGuest(UUID eventId, UUID attendeeId) {
        //TODO: implement
        return Uni.createFrom().item(UUID.randomUUID());
    }

    /**
     * Find a ticket by id or fail
     *
     * @param id     the id of the ticket
     * @param method the context in which the find is performed (for logging purposes)
     * @return a {@link Uni} with the ticket, otherwise a failed {@link Uni}
     */
    Uni<Ticket> findTicketOrFail(UUID id, Method method) {
        return Ticket.<Ticket>findById(id)
                .onFailure().transform(t -> {
                    String message = "Failed to get ticket with id " + id;
                    LOGGER.error("[" + method + "] " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNull().failWith(() -> {
                    String message = "Ticket with id " + id + " does not exist";
                    LOGGER.debug("[" + method + "] " + message);
                    return TicketServiceException.NOT_FOUND.withMessage(message);
                });
    }

    /**
     * Persist a ticket or fail
     *
     * @param ticket the ticket to persist
     * @param method the context in which the persist is performed (for logging purposes)
     * @return a {@link Uni} with the persisted ticket, otherwise a failed {@link Uni}
     */
    Uni<Ticket> persistTicketOrFail(Ticket ticket, Method method) {
        return ticket.<Ticket>persist()
                .onFailure().transform(t -> {
                    String message = "Failed to persist ticket for attendee " + ticket.attendeeId;
                    LOGGER.error("[" + method + "] " + message, t);
                    return TicketServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNotNull().invoke(existingTicket -> LOGGER.debug("[" + method + "] Persisted ticket with id " + existingTicket.id));
    }

    enum Method {
        LIST,
        DETAILS,
        CREATE,
        UPDATE,
        DELETE,
    }
}
