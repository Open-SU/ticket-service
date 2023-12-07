package com.open.su.exceptions;

import com.open.su.controllers.models.UpdateStatusErrorMessage;
import io.grpc.Status;

import java.util.UUID;

public class TicketServiceException extends RuntimeException {

    /**
     * Predefined exception for database errors.
     */
    public static final TicketServiceException DATABASE_ERROR = new TicketServiceException(Type.DATABASE_ERROR, "Database error");

    /**
     * Predefined exception for not found errors.
     */
    public static final TicketServiceException NOT_FOUND = new TicketServiceException(Type.NOT_FOUND, "Not found");

    /**
     * Predefined exception for conflict errors.
     */
    public static final TicketServiceException CONFLICT = new TicketServiceException(Type.CONFLICT, "Conflict");

    /**
     * Predefined exception for invalid argument errors.
     */
    public static final TicketServiceException INVALID_ARGUMENT = new TicketServiceException(Type.INVALID_ARGUMENT, "Invalid argument");

    final Type type;

    TicketServiceException(Type type, String message) {
        super(message);
        this.type = type;
    }

    TicketServiceException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public TicketServiceException withMessage(String message) {
        return new TicketServiceException(type, message, this);
    }

    public TicketServiceException withCause(Throwable cause) {
        return new TicketServiceException(type, getMessage(), cause);
    }

    /**
     * Converts this exception to a {@link RuntimeException} that is gRPC suitable.
     *
     * @return the gRPC suitable exception
     */
    public RuntimeException toGrpcException() {
        return switch (type) {
            case DATABASE_ERROR ->
                    Status.INTERNAL.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case NOT_FOUND -> Status.NOT_FOUND.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case CONFLICT ->
                    Status.ALREADY_EXISTS.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case INVALID_ARGUMENT ->
                    Status.INVALID_ARGUMENT.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
        };
    }

    public UpdateStatusErrorMessage toUpdateStatusErrorMessage(UUID ticketId) {
        return switch (type) {
            case NOT_FOUND ->
                    new UpdateStatusErrorMessage(this, UpdateStatusErrorMessage.Type.TICKET_NOT_FOUND, ticketId);
            case INVALID_ARGUMENT ->
                    new UpdateStatusErrorMessage(this, UpdateStatusErrorMessage.Type.INVALID_STATUS, ticketId);
            default -> new UpdateStatusErrorMessage(this, UpdateStatusErrorMessage.Type.UNEXPECTED_ERROR, ticketId);
        };
    }

    /**
     * Types of possible {@link TicketServiceException}.
     */
    public enum Type {
        DATABASE_ERROR,
        NOT_FOUND,
        CONFLICT,
        INVALID_ARGUMENT
    }
}
