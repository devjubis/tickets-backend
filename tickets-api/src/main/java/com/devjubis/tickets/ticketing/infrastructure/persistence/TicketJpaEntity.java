package com.devjubis.tickets.ticketing.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import com.devjubis.tickets.shared.infrastructure.persistence.BaseJpaEntity;
import com.devjubis.tickets.ticketing.domain.enums.TicketPriority;
import com.devjubis.tickets.ticketing.domain.enums.TicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketJpaEntity extends BaseJpaEntity {

    @Generated(event = EventType.INSERT)
    @Column(name = "ticket_number", nullable = false)
    private Long ticketNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "category_id", nullable = false)
    private Short categoryId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ticket_status")
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "priority", nullable = false, columnDefinition = "ticket_priority")
    private TicketPriority priority;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
