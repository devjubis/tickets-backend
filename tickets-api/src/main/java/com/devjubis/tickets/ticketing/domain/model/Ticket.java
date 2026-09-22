package com.devjubis.tickets.ticketing.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;
import com.devjubis.tickets.shared.domain.UuidV7;
import com.devjubis.tickets.ticketing.domain.enums.TicketPriority;
import com.devjubis.tickets.ticketing.domain.enums.TicketStatus;
import com.devjubis.tickets.ticketing.domain.exception.InvalidStatusTransition;

public final class Ticket {

    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN,
            Set.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_CUSTOMER, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS,
            Set.of(TicketStatus.WAITING_CUSTOMER, TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.WAITING_CUSTOMER,
            Set.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED,
            Set.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS),
            TicketStatus.CLOSED,
            Set.of(),
            TicketStatus.CANCELLED,
            Set.of());

    private final UUID id;
    private final Long ticketNumber;
    private final UUID customerId;
    private final UUID createdBy;
    private final Instant openedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private Short categoryId;
    private TicketStatus status;
    private TicketPriority priority;
    private String title;
    private String description;
    private UUID assignedTo;
    private Instant firstResponseAt;
    private Instant resolvedAt;
    private Instant closedAt;

    private Ticket(UUID id, Long ticketNumber, UUID customerId, Short categoryId, TicketStatus status,
            TicketPriority priority, String title, String description, UUID createdBy, UUID assignedTo,
            Instant openedAt, Instant firstResponseAt, Instant resolvedAt, Instant closedAt,
            Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.customerId = customerId;
        this.categoryId = categoryId;
        this.status = status;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.openedAt = openedAt;
        this.firstResponseAt = firstResponseAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Ticket open(UUID customerId, Short categoryId, String title, String description,
            TicketPriority priority, UUID createdBy, Instant openedAt) {
        requirePresent(customerId, "O cliente é obrigatório");
        requirePresent(categoryId, "A categoria é obrigatória");
        requirePresent(createdBy, "O autor é obrigatório");
        requirePresent(openedAt, "O momento de abertura é obrigatório");
        requireValidTitle(title);
        requireValidDescription(description);

        return new Ticket(UuidV7.generate(), null, customerId, categoryId, TicketStatus.OPEN,
                priority == null ? TicketPriority.MEDIUM : priority, title.trim(), description, createdBy,
                null, openedAt, null, null, null, null, null, 0L);
    }

    public static Ticket restore(UUID id, Long ticketNumber, UUID customerId, Short categoryId,
            TicketStatus status, TicketPriority priority, String title, String description, UUID createdBy,
            UUID assignedTo, Instant openedAt, Instant firstResponseAt, Instant resolvedAt, Instant closedAt,
            Instant createdAt, Instant updatedAt, long version) {
        return new Ticket(id, ticketNumber, customerId, categoryId, status, priority, title, description,
                createdBy, assignedTo, openedAt, firstResponseAt, resolvedAt, closedAt, createdAt, updatedAt,
                version);
    }

    public void changeStatus(TicketStatus targetStatus, Instant changedAt) {
        requirePresent(targetStatus, "O estado de destino é obrigatório");
        requirePresent(changedAt, "O momento da transição é obrigatório");
        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransition(this.status, targetStatus);
        }

        registerFirstResponse(changedAt);
        this.status = targetStatus;
        applyStatusTimestamps(targetStatus, changedAt);
    }

    public void assignTo(UUID agentId, Instant assignedAt) {
        requirePresent(agentId, "O responsável é obrigatório");
        requirePresent(assignedAt, "O momento da atribuição é obrigatório");
        requireNotTerminal();

        this.assignedTo = agentId;
        registerFirstResponse(assignedAt);
    }

    public void unassign() {
        requireNotTerminal();
        this.assignedTo = null;
    }

    public void updateDetails(String newTitle, String newDescription, TicketPriority newPriority,
            Short newCategoryId) {
        requireNotTerminal();
        requireValidTitle(newTitle);
        requireValidDescription(newDescription);
        requirePresent(newCategoryId, "A categoria é obrigatória");

        this.title = newTitle.trim();
        this.description = newDescription;
        this.priority = newPriority == null ? this.priority : newPriority;
        this.categoryId = newCategoryId;
    }

    public boolean canTransitionTo(TicketStatus targetStatus) {
        return ALLOWED_TRANSITIONS.get(this.status).contains(targetStatus);
    }

    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.get(this.status).isEmpty();
    }

    public boolean isAssigned() {
        return this.assignedTo != null;
    }

    public boolean belongsTo(UUID candidateCustomerId) {
        return this.customerId.equals(candidateCustomerId);
    }

    private void registerFirstResponse(Instant respondedAt) {
        if (this.firstResponseAt == null && this.status == TicketStatus.OPEN) {
            this.firstResponseAt = respondedAt;
        }
    }

    private void applyStatusTimestamps(TicketStatus targetStatus, Instant changedAt) {
        switch (targetStatus) {
            case RESOLVED -> this.resolvedAt = changedAt;
            case CLOSED, CANCELLED -> this.closedAt = changedAt;
            case IN_PROGRESS -> this.resolvedAt = null;
            default -> {
            }
        }
    }

    private void requireNotTerminal() {
        if (isTerminal()) {
            throw new BusinessRuleViolation("Um chamado %s não pode ser alterado.".formatted(this.status));
        }
    }

    private static void requireValidTitle(String candidateTitle) {
        if (candidateTitle == null || candidateTitle.isBlank()) {
            throw new BusinessRuleViolation("O título é obrigatório");
        }
        String trimmedTitle = candidateTitle.trim();
        if (trimmedTitle.length() < TITLE_MIN_LENGTH || trimmedTitle.length() > TITLE_MAX_LENGTH) {
            throw new BusinessRuleViolation("O título deve ter entre %d e %d caracteres"
                    .formatted(TITLE_MIN_LENGTH, TITLE_MAX_LENGTH));
        }
    }

    private static void requireValidDescription(String candidateDescription) {
        if (candidateDescription != null && candidateDescription.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessRuleViolation(
                    "A descrição não pode exceder %d caracteres".formatted(DESCRIPTION_MAX_LENGTH));
        }
    }

    private static void requirePresent(Object value, String message) {
        if (value == null) {
            throw new BusinessRuleViolation(message);
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getTicketNumber() {
        return ticketNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Short getCategoryId() {
        return categoryId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getFirstResponseAt() {
        return firstResponseAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
