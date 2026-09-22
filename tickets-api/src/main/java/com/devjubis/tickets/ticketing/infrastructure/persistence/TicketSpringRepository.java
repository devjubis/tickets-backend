package com.devjubis.tickets.ticketing.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketSpringRepository
        extends JpaRepository<TicketJpaEntity, UUID>, JpaSpecificationExecutor<TicketJpaEntity> {
}
