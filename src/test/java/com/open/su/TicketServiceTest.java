package com.open.su;

import com.open.su.exceptions.TicketServiceException;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@QuarkusTest
class TicketServiceTest {
    @Inject
    TicketService ticketService;

    @RunOnVertxContext
    @Test
    void testListTickets(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket1 = new Ticket();
            ticket1.busId = UUID.randomUUID();
            ticket1.eventId = UUID.randomUUID();
            ticket1.attendeeId = UUID.randomUUID();
            ticket1.status = Ticket.Status.CREATED;
            Ticket ticket2 = new Ticket();
            ticket2.busId = UUID.randomUUID();
            ticket2.eventId = UUID.randomUUID();
            ticket2.attendeeId = UUID.randomUUID();
            ticket2.status = Ticket.Status.PAID;
            Ticket ticket3 = new Ticket();
            ticket3.busId = UUID.randomUUID();
            ticket3.eventId = UUID.randomUUID();
            ticket3.attendeeId = UUID.randomUUID();
            ticket3.status = Ticket.Status.REFUNDED;

            asserter.putData("tickets", List.of(ticket1, ticket2, ticket3));

            return ticket1.persist().chain(ticket2::persist).chain(ticket3::persist);
        });

        asserter.assertThat(() -> {
            Page page = Page.of(0, 10);
            Sort sort = Sort.by("status", Sort.Direction.Ascending);

            return ticketService.listTickets(page, sort);
        }, response -> {
            List<Ticket> tickets = (List<Ticket>) asserter.getData("tickets");
            Assertions.assertEquals(tickets.get(0), response.get(0));
            Assertions.assertEquals(tickets.get(1), response.get(1));
            Assertions.assertEquals(tickets.get(2), response.get(2));
        });

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testGetTicketDetails(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket = new Ticket();
            ticket.busId = UUID.randomUUID();
            ticket.eventId = UUID.randomUUID();
            ticket.attendeeId = UUID.randomUUID();
            ticket.status = Ticket.Status.CREATED;

            asserter.putData("ticket", ticket);

            return ticket.persist();
        });

        asserter.assertThat(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket");

            return ticketService.getTicketDetails(ticket.id);
        }, response -> {
            Ticket ticket = (Ticket) asserter.getData("ticket");
            Assertions.assertEquals(ticket, response);
        });

        asserter.assertFailedWith(() -> ticketService.getTicketDetails(UUID.randomUUID())
                , e -> Assertions.assertSame(TicketServiceException.Type.NOT_FOUND, ((TicketServiceException) e).getType()));

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testAttendEvent(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket1 = new Ticket();
            ticket1.busId = UUID.randomUUID();
            ticket1.eventId = UUID.randomUUID();
            ticket1.attendeeId = UUID.randomUUID();
            ticket1.status = Ticket.Status.CREATED;
            Ticket ticket2 = new Ticket();
            ticket2.id = UUID.randomUUID();
            ticket2.busId = UUID.randomUUID();
            ticket2.eventId = UUID.randomUUID();
            ticket2.attendeeId = UUID.randomUUID();
            ticket2.status = Ticket.Status.PAID;

            asserter.putData("ticket1", ticket1);
            asserter.putData("ticket2", ticket2);

            return ticket1.persist();
        });

        asserter.execute(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket2");

            return ticketService.attendEvent(ticket.eventId, ticket.attendeeId);
        });

        asserter.execute(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket2");

            return Ticket.find("eventId=?1 AND attendeeId=?2", ticket.eventId, ticket.attendeeId).firstResult();
        });

        asserter.assertFailedWith(() -> {
            Ticket ticket = new Ticket();
            ticket.id = UUID.randomUUID();
            ticket.busId = UUID.randomUUID();
            ticket.eventId = ((Ticket) asserter.getData("ticket1")).eventId;
            ticket.attendeeId = ((Ticket) asserter.getData("ticket1")).attendeeId;
            ticket.status = Ticket.Status.CREATED;

            return ticketService.attendEvent(ticket.eventId, ticket.attendeeId);
        }, e -> Assertions.assertSame(TicketServiceException.Type.CONFLICT, ((TicketServiceException) e).getType()));

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testUpdateTicket(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket1 = new Ticket();
            ticket1.eventId = UUID.randomUUID();
            ticket1.busId = UUID.randomUUID();
            ticket1.attendeeId = UUID.randomUUID();
            ticket1.status = Ticket.Status.CREATED;
            Ticket ticket2 = new Ticket();
            ticket2.id = UUID.randomUUID();
            ticket2.eventId = UUID.randomUUID();
            ticket2.busId = UUID.randomUUID();
            ticket2.attendeeId = UUID.randomUUID();
            ticket2.status = Ticket.Status.PAID;

            asserter.putData("ticket1", ticket1);
            asserter.putData("ticket2", ticket2);

            return ticket1.persist();
        });

        asserter.assertThat(() -> {
            Ticket ticket1 = (Ticket) asserter.getData("ticket1");
            Ticket ticket2 = (Ticket) asserter.getData("ticket2");
            ticket1.busId = ticket2.busId;

            return ticketService.updateTicket(ticket1);
        }, response -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");
            Assertions.assertEquals(ticket.id, response);
        });

        asserter.assertThat(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");

            return Ticket.findById(ticket.id);
        }, response -> {
            Ticket ticket = (Ticket) asserter.getData("ticket2");
            Assertions.assertEquals(ticket.busId, ((Ticket) response).busId);
        });

        asserter.assertFailedWith(() -> {
            Ticket ticket = new Ticket();
            ticket.id = UUID.randomUUID();
            ticket.eventId = UUID.randomUUID();
            ticket.busId = UUID.randomUUID();
            ticket.attendeeId = UUID.randomUUID();
            ticket.status = Ticket.Status.CREATED;

            return ticketService.updateTicket(ticket);
        }, e -> Assertions.assertSame(TicketServiceException.Type.NOT_FOUND, ((TicketServiceException) e).getType()));

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testUpdateTicketStatus(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket1 = new Ticket();
            ticket1.eventId = UUID.randomUUID();
            ticket1.busId = UUID.randomUUID();
            ticket1.attendeeId = UUID.randomUUID();
            ticket1.status = Ticket.Status.CREATED;
            Ticket ticket2 = new Ticket();
            ticket2.id = UUID.randomUUID();
            ticket2.eventId = UUID.randomUUID();
            ticket2.busId = UUID.randomUUID();
            ticket2.attendeeId = UUID.randomUUID();
            ticket2.status = Ticket.Status.PAID;
            Ticket ticket3 = new Ticket();
            ticket3.eventId = UUID.randomUUID();
            ticket3.busId = UUID.randomUUID();
            ticket3.attendeeId = UUID.randomUUID();
            ticket3.status = Ticket.Status.CANCELLED;
            Ticket ticket4 = new Ticket();
            ticket4.eventId = UUID.randomUUID();
            ticket4.busId = UUID.randomUUID();
            ticket4.attendeeId = UUID.randomUUID();
            ticket4.status = Ticket.Status.SCANNED;

            asserter.putData("ticket1", ticket1);
            asserter.putData("ticket2", ticket2);
            asserter.putData("ticket3", ticket3);
            asserter.putData("ticket4", ticket4);

            return ticket1.persist().chain(ticket3::persist).chain(ticket4::persist);
        });

        // Test that CREATED -> SCANNED is not allowed
        asserter.assertFailedWith(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");

            return ticketService.updateTicketStatus(ticket.id, Ticket.Status.SCANNED);
        }, e -> Assertions.assertSame(TicketServiceException.Type.INVALID_ARGUMENT, ((TicketServiceException) e).getType()));

        asserter.assertThat(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");

            return ticketService.updateTicketStatus(ticket.id, Ticket.Status.PAID);
        }, response -> {
            Ticket ticket1 = (Ticket) asserter.getData("ticket1");
            Ticket ticket2 = (Ticket) asserter.getData("ticket2");
            Assertions.assertEquals(ticket1.id, response.getItem1());
            Assertions.assertEquals(ticket2.status, response.getItem2());
        });

        asserter.assertThat(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");

            return Ticket.findById(ticket.id);
        }, response -> {
            Ticket ticket = (Ticket) asserter.getData("ticket2");
            Assertions.assertEquals(ticket.status, ((Ticket) response).status);
        });

        // Test that PAID -> CANCELLED is not allowed
        asserter.assertFailedWith(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket1");

            return ticketService.updateTicketStatus(ticket.id, Ticket.Status.CANCELLED);
        }, e -> Assertions.assertSame(TicketServiceException.Type.INVALID_ARGUMENT, ((TicketServiceException) e).getType()));

        // Test that CANCELLED -> SCANNED is not allowed
        asserter.assertFailedWith(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket3");

            return ticketService.updateTicketStatus(ticket.id, Ticket.Status.SCANNED);
        }, e -> Assertions.assertSame(TicketServiceException.Type.INVALID_ARGUMENT, ((TicketServiceException) e).getType()));

        // Test that SCANNED -> PAID is not allowed
        asserter.assertFailedWith(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket4");

            return ticketService.updateTicketStatus(ticket.id, Ticket.Status.PAID);
        }, e -> Assertions.assertSame(TicketServiceException.Type.INVALID_ARGUMENT, ((TicketServiceException) e).getType()));


        asserter.assertFailedWith(() -> ticketService.updateTicketStatus(UUID.randomUUID(), Ticket.Status.PAID)
                , e -> Assertions.assertSame(TicketServiceException.Type.NOT_FOUND, ((TicketServiceException) e).getType()));

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testDeleteTicket(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Ticket ticket = new Ticket();
            ticket.busId = UUID.randomUUID();
            ticket.eventId = UUID.randomUUID();
            ticket.attendeeId = UUID.randomUUID();
            ticket.status = Ticket.Status.CREATED;

            asserter.putData("ticket", ticket);

            return ticket.persist();
        });

        asserter.assertFailedWith(() -> ticketService.deleteTicket(UUID.randomUUID())
                , e -> Assertions.assertSame(TicketServiceException.Type.NOT_FOUND, ((TicketServiceException) e).getType()));

        // Assert that calling delete does not fail (delete returns void if not failing)
        asserter.assertThat(() -> {
            Ticket ticket = (Ticket) asserter.getData("ticket");

            return ticketService.deleteTicket(ticket.id);
        }, Assertions::assertNull);

        asserter.assertFailedWith(() -> ticketService.deleteTicket(((Ticket) asserter.getData("ticket")).id)
                , e -> Assertions.assertSame(TicketServiceException.Type.NOT_FOUND, ((TicketServiceException) e).getType()));

        asserter.execute(() -> Ticket.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }
}
