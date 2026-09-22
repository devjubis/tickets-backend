package com.devjubis.tickets.ticketing.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.devjubis.tickets.ticketing.domain.model.Ticket;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TicketPersistenceMapper {

    TicketJpaEntity toJpaEntity(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticketNumber", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    TicketJpaEntity applyChanges(Ticket ticket, @MappingTarget TicketJpaEntity entity);

    default Ticket toDomain(TicketJpaEntity entity) {
        return Ticket.restore(
                entity.getId(),
                entity.getTicketNumber(),
                entity.getCustomerId(),
                entity.getCategoryId(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getAssignedTo(),
                entity.getOpenedAt(),
                entity.getFirstResponseAt(),
                entity.getResolvedAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }
}
