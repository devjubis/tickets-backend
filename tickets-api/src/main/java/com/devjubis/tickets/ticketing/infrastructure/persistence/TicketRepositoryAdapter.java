package com.devjubis.tickets.ticketing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.devjubis.tickets.ticketing.domain.model.Ticket;
import com.devjubis.tickets.ticketing.domain.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class TicketRepositoryAdapter implements TicketRepository {

    private final TicketSpringRepository ticketSpringRepository;
    private final TicketPersistenceMapper ticketPersistenceMapper;

    @Override
    public Ticket save(Ticket ticket) {
        TicketJpaEntity entityToPersist = ticketSpringRepository.findById(ticket.getId())
                .map(existingEntity -> applyChangesRejectingStaleVersion(ticket, existingEntity))
                .orElseGet(() -> ticketPersistenceMapper.toJpaEntity(ticket));

        return ticketPersistenceMapper.toDomain(ticketSpringRepository.saveAndFlush(entityToPersist));
    }

    private TicketJpaEntity applyChangesRejectingStaleVersion(Ticket ticket, TicketJpaEntity existingEntity) {
        if (ticket.getVersion() != existingEntity.getVersion()) {
            throw new ObjectOptimisticLockingFailureException(TicketJpaEntity.class, ticket.getId());
        }
        return ticketPersistenceMapper.applyChanges(ticket, existingEntity);
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        return ticketSpringRepository.findById(id).map(ticketPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return ticketSpringRepository.existsById(id);
    }
}
