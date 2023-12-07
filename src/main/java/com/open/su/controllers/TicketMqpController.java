package com.open.su.controllers;

import com.open.su.TicketService;
import com.open.su.controllers.models.UpdateStatusErrorMessage;
import com.open.su.controllers.models.UpdateStatusMessage;
import com.open.su.controllers.models.UpdateStatusSuccessMessage;
import com.open.su.exceptions.TicketServiceException;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.Targeted;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TicketMqpController {
    private static final Logger LOGGER = Logger.getLogger(TicketMqpController.class);

    private final TicketService ticketService;

    @Inject
    TicketMqpController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Update ticket status
     *
     * @param message the {@link UpdateStatusMessage} in JSON format
     * @return a {@link Uni} of {@link Targeted} with the {@link UpdateStatusSuccessMessage} or {@link UpdateStatusErrorMessage}
     */
    @Incoming("update-status-in")
    @Outgoing("update-status-out")
    @Outgoing("update-status-error")
    public Uni<Targeted> updateStatus(JsonObject message) {
        UpdateStatusMessage updateStatusMessage = message.mapTo(UpdateStatusMessage.class);
        return ticketService.updateTicketStatus(updateStatusMessage.ticketId(), updateStatusMessage.status())
                .onItem().transformToUni(t -> Uni.createFrom().item(Targeted.of("update-status-out", new UpdateStatusSuccessMessage(t.getItem1(), t.getItem2()))))
                .onFailure().recoverWithUni(t -> {
                    final String errorExchangeName = "update-status-error";
                    if (t instanceof TicketServiceException ticketServiceException) {
                        return Uni.createFrom().item(Targeted.of(errorExchangeName, ticketServiceException.toUpdateStatusErrorMessage(updateStatusMessage.ticketId())));
                    }
                    LOGGER.error("Unexpected error during ticket status update", t);
                    return Uni.createFrom().item(Targeted.of(errorExchangeName, new UpdateStatusErrorMessage(t, UpdateStatusErrorMessage.Type.UNEXPECTED_ERROR, updateStatusMessage.ticketId())));
                });
    }
}
