package com.devjubis.tickets.iam.domain.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;
import com.devjubis.tickets.shared.domain.UuidV7;
import com.devjubis.tickets.shared.domain.vo.Email;
import com.devjubis.tickets.shared.domain.vo.PasswordHash;

public final class User {

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 120;
    private static final int AVATAR_URL_MAX_LENGTH = 512;

    private static final Set<Role> INTERNAL_ROLES = EnumSet.of(Role.ADMIN, Role.AGENT);

    private final UUID id;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private String name;
    private Email email;
    private PasswordHash passwordHash;
    private String avatarUrl;
    private UUID customerId;
    private final EnumSet<Role> roles;
    private boolean active;

    private User(UUID id, String name, Email email, PasswordHash passwordHash, String avatarUrl,
            UUID customerId, Set<Role> roles, boolean active, Instant createdAt, Instant updatedAt,
            long version) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.avatarUrl = avatarUrl;
        this.customerId = customerId;
        this.roles = EnumSet.copyOf(roles);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static User createAdmin(String name, Email email, PasswordHash passwordHash) {
        return createInternal(name, email, passwordHash, Role.ADMIN);
    }

    public static User createAgent(String name, Email email, PasswordHash passwordHash) {
        return createInternal(name, email, passwordHash, Role.AGENT);
    }

    public static User createRequester(String name, Email email, PasswordHash passwordHash,
            UUID customerId) {
        requireValidName(name);
        requirePresent(email, "O email é obrigatório");
        requirePresent(passwordHash, "A senha é obrigatória");
        requirePresent(customerId, "Um solicitante pertence sempre a um cliente");

        return new User(UuidV7.generate(), name.trim(), email, passwordHash, null, customerId,
                EnumSet.of(Role.REQUESTER), true, null, null, 0L);
    }

    public static User restore(UUID id, String name, Email email, PasswordHash passwordHash, String avatarUrl,
            UUID customerId, Set<Role> roles, boolean active, Instant createdAt, Instant updatedAt,
            long version) {
        requireAtLeastOneRole(roles);

        return new User(id, name, email, passwordHash, avatarUrl, customerId, roles, active, createdAt,
                updatedAt, version);
    }

    private static User createInternal(String name, Email email, PasswordHash passwordHash, Role role) {
        requireValidName(name);
        requirePresent(email, "O email é obrigatório");
        requirePresent(passwordHash, "A senha é obrigatória");

        return new User(UuidV7.generate(), name.trim(), email, passwordHash, null, null, EnumSet.of(role),
                true, null, null, 0L);
    }

    public void grantRole(Role role) {
        requirePresent(role, "O papel é obrigatório");
        requireActive();

        EnumSet<Role> candidateRoles = EnumSet.copyOf(this.roles);
        candidateRoles.add(role);
        requireConsistentMembership(candidateRoles, this.customerId);

        this.roles.add(role);
    }

    public void revokeRole(Role role) {
        requirePresent(role, "O papel é obrigatório");
        requireActive();

        EnumSet<Role> candidateRoles = EnumSet.copyOf(this.roles);
        candidateRoles.remove(role);
        requireAtLeastOneRole(candidateRoles);
        requireConsistentMembership(candidateRoles, this.customerId);

        this.roles.remove(role);
    }

    public void assignToCustomer(UUID newCustomerId) {
        requirePresent(newCustomerId, "O cliente é obrigatório");
        requireActive();
        requireConsistentMembership(this.roles, newCustomerId);

        this.customerId = newCustomerId;
    }

    public void changePasswordHash(PasswordHash newPasswordHash) {
        requirePresent(newPasswordHash, "A senha é obrigatória");
        requireActive();

        this.passwordHash = newPasswordHash;
    }

    public void changeEmail(Email newEmail) {
        requirePresent(newEmail, "O email é obrigatório");
        requireActive();

        this.email = newEmail;
    }

    public void updateProfile(String newName, String newAvatarUrl) {
        requireValidName(newName);
        requireValidAvatarUrl(newAvatarUrl);
        requireActive();

        this.name = newName.trim();
        this.avatarUrl = newAvatarUrl;
    }

    public void deactivate() {
        if (!this.active) {
            throw new BusinessRuleViolation("O utilizador já está inativo");
        }
        this.active = false;
    }

    public void activate() {
        if (this.active) {
            throw new BusinessRuleViolation("O utilizador já está ativo");
        }
        this.active = true;
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public boolean isRequester() {
        return hasRole(Role.REQUESTER);
    }

    public boolean isInternalStaff() {
        return this.roles.stream().anyMatch(INTERNAL_ROLES::contains);
    }

    public boolean belongsTo(UUID candidateCustomerId) {
        return this.customerId != null && this.customerId.equals(candidateCustomerId);
    }

    private void requireActive() {
        if (!this.active) {
            throw new BusinessRuleViolation("Um utilizador inativo não pode ser alterado");
        }
    }

    private static void requireAtLeastOneRole(Set<Role> candidateRoles) {
        if (candidateRoles == null || candidateRoles.isEmpty()) {
            throw new BusinessRuleViolation("Um utilizador tem de ter pelo menos um papel");
        }
    }

    private static void requireConsistentMembership(Set<Role> candidateRoles, UUID candidateCustomerId) {
        if (candidateRoles.contains(Role.REQUESTER)) {
            if (candidateRoles.stream().anyMatch(INTERNAL_ROLES::contains)) {
                throw new BusinessRuleViolation("Um solicitante não pode acumular papéis internos");
            }
            if (candidateCustomerId == null) {
                throw new BusinessRuleViolation("Um solicitante pertence sempre a um cliente");
            }
        } else if (candidateCustomerId != null) {
            throw new BusinessRuleViolation("Apenas um solicitante pertence a um cliente");
        }
    }

    private static void requireValidName(String candidateName) {
        if (candidateName == null || candidateName.isBlank()) {
            throw new BusinessRuleViolation("O nome é obrigatório");
        }
        String trimmedName = candidateName.trim();
        if (trimmedName.length() < NAME_MIN_LENGTH || trimmedName.length() > NAME_MAX_LENGTH) {
            throw new BusinessRuleViolation(
                    "O nome deve ter entre %d e %d caracteres".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
    }

    private static void requireValidAvatarUrl(String candidateAvatarUrl) {
        if (candidateAvatarUrl != null && candidateAvatarUrl.length() > AVATAR_URL_MAX_LENGTH) {
            throw new BusinessRuleViolation(
                    "O endereço do avatar não pode exceder %d caracteres".formatted(AVATAR_URL_MAX_LENGTH));
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

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public boolean isActive() {
        return active;
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
