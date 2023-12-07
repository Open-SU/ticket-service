package com.open.su;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Entity
public class Ticket extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, name = "event_id")
    UUID eventId;
    @Column(nullable = false, name = "attendee_id")
    UUID attendeeId;
    @Column(name = "bus_id")
    UUID busId = null;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Status status = Status.CREATED;
    @Column(name = "created_at")
    @CreationTimestamp
    Date createdAt;
    @Column(name = "updated_at")
    @UpdateTimestamp
    Date updatedAt;

    /**
     * Create a new ticket from a {@link UpdateTicketRequest}
     *
     * @param request the grpc request
     */
    public Ticket(UpdateTicketRequest request) {
        this.id = UUID.fromString(request.getId());
        this.busId = request.hasBusId() ? UUID.fromString(request.getBusId()) : null;
    }

    public Ticket() {

    }

    public Ticket update(Ticket ticket) {
        this.eventId = Optional.ofNullable(ticket.eventId).orElse(this.eventId);
        this.attendeeId = Optional.ofNullable(ticket.attendeeId).orElse(this.attendeeId);
        this.busId = Optional.ofNullable(ticket.busId).orElse(this.busId);
        this.status = Optional.ofNullable(ticket.status).orElse(this.status);
        return this;
    }

    /**
     * Convert the ticket to a {@link ListTicketsResponse}
     *
     * @return the grpc response
     */
    public ListTicketsResponse toListTicketsResponse() {
        ListTicketsResponse.Builder builder = ListTicketsResponse.newBuilder()
                .setId(this.id.toString())
                .setEventId(this.eventId.toString())
                .setAttendeeId(this.attendeeId.toString())
                .setStatus(com.open.su.Status.valueOf(this.status.toString()));

        if (this.busId != null) {
            builder.setBusId(this.busId.toString());
        }

        return builder.build();
    }

    /**
     * Convert the ticket to a {@link GetTicketDetailsResponse}
     *
     * @return the grpc response
     */
    public GetTicketDetailsResponse toGetTicketDetailsResponse() {
        return GetTicketDetailsResponse.newBuilder()
                .setId(this.id.toString())
                .setEventId(this.eventId.toString())
                .setAttendeeId(this.attendeeId.toString())
                .setBusId(this.busId == null ? null : this.busId.toString())
                .setStatus(com.open.su.Status.valueOf(this.status.toString()))
                .setCreatedAt(this.createdAt.toInstant().toString())
                .setUpdatedAt(this.updatedAt.toInstant().toString()).build();
    }

    public enum Status {
        CREATED(0),
        CANCELLED(-1),
        PAID(1),
        REFUNDED(-2),
        EXCHANGED(-2),
        SCANNED(-2);

        public final int order;

        Status(int order) {
            this.order = order;
        }
    }
}
