package com.open.su;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.UUID;

@Entity
public class Ticket extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "event_id", nullable = false)
    UUID eventId;
    @Column(name = "bus_id")
    UUID busId = null;
    @Column(name = "attendee_id", nullable = false)
    UUID attendeeId = UUID.randomUUID();
    @Column(nullable = false)
    Boolean vip = false;
    @Column(nullable = false)
    TicketStatus status = TicketStatus.CREATED;
    @Column(name = "created_at")
    @CreationTimestamp
    Date createdAt;
    @Column(name = "updated_at")
    @UpdateTimestamp
    Date updatedAt;

    public Ticket() {
    }

    public enum TicketStatus {
        CREATED,
        CANCELLED,
        PAID,
        BUS_ASSIGNED,
        REFUNDED,
        EXCHANGED,
        SCANNED
    }
}
