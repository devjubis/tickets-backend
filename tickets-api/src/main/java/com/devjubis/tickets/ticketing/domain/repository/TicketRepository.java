package com.devjubis.tickets.ticketing.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.devjubis.tickets.ticketing.domain.model.Ticket;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(UUID id);

    boolean existsById(UUID id);
}
