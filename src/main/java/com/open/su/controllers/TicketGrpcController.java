package com.open.su.controllers;

import com.open.su.*;
import com.open.su.exceptions.TicketServiceException;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@GrpcService
public class TicketGrpcController implements TicketGrpc {
    private static final Logger LOGGER = Logger.getLogger(TicketGrpcController.class);

    private final TicketService ticketService;

    @Inject
    public TicketGrpcController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Get a paginated list of tickets with minimal information.
     *
     * @param request the gRPC request
     * @return the list tickets response
     */
    @Override
    public Multi<ListTicketsResponse> listTickets(ListTicketsRequest request) {
        Page page = Page.of(request.hasPage() ? request.getPage() : 0, request.hasSize() ? request.getSize() : 10);
        Sort sort = Sort.by(request.hasSort() ? request.getSort() : "status", request.hasOrder() ? Sort.Direction.valueOf(request.getOrder()) : Sort.Direction.Ascending);

        return ticketService.listTickets(page, sort)
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while listing tickets";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transformToMulti(tickets -> Multi.createFrom().iterable(tickets))
                .map(Ticket::toListTicketsResponse);
    }

    /**
     * Get a ticket by its ID.
     *
     * @param request the gRPC request
     * @return the get ticket details response
     */
    @Override
    public Uni<GetTicketDetailsResponse> getTicketDetails(GetTicketDetailsRequest request) {
        return ticketService.getTicketDetails(UUID.fromString(request.getId()))
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while getting ticket details";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(Ticket::toGetTicketDetailsResponse);
    }

    /**
     * Update an existing ticket.
     *
     * @param request the gRPC request
     * @return the update ticket response
     */
    @Override
    public Uni<UpdateTicketResponse> updateTicket(UpdateTicketRequest request) {
        return ticketService.updateTicket(new Ticket(request))
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while updating ticket";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> UpdateTicketResponse.newBuilder().setId(id.toString()).build());
    }

    /**
     * Attend an event.
     *
     * @param request the gRPC request
     * @return the attend event response
     */
    public Uni<AttendEventResponse> attendEvent(AttendEventRequest request) {
        return ticketService.attendEvent(UUID.fromString(request.getEventId()), UUID.fromString(request.getAttendeeId()))
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while attending event";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> AttendEventResponse.newBuilder().setId(id.toString()).build());
    }

    /**
     * Select a bus.
     *
     * @param request the gRPC request
     * @return the select bus response
     */
    public Uni<SelectBusResponse> selectBus(SelectBusRequest request) {
        return ticketService.selectBus(UUID.fromString(request.getId()), UUID.fromString(request.getBusId()))
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while selecting bus";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> SelectBusResponse.newBuilder().setId(id.toString()).build());
    }

    /**
     * Delete an existing ticket.
     *
     * @param request the gRPC request
     * @return the delete ticket response
     */
    @Override
    public Uni<DeleteTicketResponse> deleteTicket(DeleteTicketRequest request) {
        return ticketService.deleteTicket(UUID.fromString(request.getId()))
                .onFailure().transform(t -> {
                    if (t instanceof TicketServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while deleting ticket";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> DeleteTicketResponse.newBuilder().build());
    }
}
