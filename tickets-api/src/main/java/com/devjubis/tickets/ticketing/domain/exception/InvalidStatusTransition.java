package com.devjubis.tickets.ticketing.domain.exception;

import com.devjubis.tickets.shared.domain.DomainException;
import com.devjubis.tickets.ticketing.domain.enums.TicketStatus;

public class InvalidStatusTransition extends DomainException {

    private final transient TicketStatus fromStatus;
    private final transient TicketStatus toStatus;

    public InvalidStatusTransition(TicketStatus fromStatus, TicketStatus toStatus) {
        super("Não é possível passar de %s para %s.".formatted(fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }
}
