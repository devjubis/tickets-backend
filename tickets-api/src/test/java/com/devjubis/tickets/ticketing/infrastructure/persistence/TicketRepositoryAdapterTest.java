package com.devjubis.tickets.ticketing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devjubis.tickets.shared.domain.UuidV7;
import com.devjubis.tickets.shared.infrastructure.config.JpaAuditingConfig;
import com.devjubis.tickets.support.PostgresContainerTest;
import com.devjubis.tickets.ticketing.domain.enums.TicketPriority;
import com.devjubis.tickets.ticketing.domain.enums.TicketStatus;
import com.devjubis.tickets.ticketing.domain.model.Ticket;
import com.devjubis.tickets.ticketing.domain.repository.TicketRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TicketRepositoryAdapter.class, TicketPersistenceMapperImpl.class, JpaAuditingConfig.class })
class TicketRepositoryAdapterTest extends PostgresContainerTest {

    private static final Short SUPORTE = 1;
    private static final Instant OPENED_AT = Instant.parse("2026-09-21T10:00:00Z");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID customerId;
    private UUID authorId;

    @BeforeEach
    void seedReferences() {
        customerId = UuidV7.generate();
        authorId = UuidV7.generate();
        insertCustomer(customerId);
        insertUser(authorId);
    }

    private Ticket newTicket() {
        return Ticket.open(customerId, SUPORTE, "Impressora não liga", "Detalhe do problema",
                TicketPriority.HIGH, authorId, OPENED_AT);
    }

    private void insertCustomer(UUID id) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO customers (id, trade_name, document_number, active,"
                        + " created_at, updated_at, version) VALUES (?1, ?2, ?3, true, now(), now(), 0)")
                .setParameter(1, id)
                .setParameter(2, "ACME Ltda")
                .setParameter(3, String.valueOf(System.nanoTime() % 100000000000000L))
                .executeUpdate();
    }

    private void insertUser(UUID id) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO users (id, name, email, password_hash, active,"
                        + " created_at, updated_at, version) VALUES (?1, ?2, ?3, ?4, true, now(), now(), 0)")
                .setParameter(1, id)
                .setParameter(2, "Maria Agente")
                .setParameter(3, "agente." + id + "@exemplo.com")
                .setParameter(4, "$2a$12$hashfake")
                .executeUpdate();
    }

    @Test
    void devePersistirEReidratarOAgregado() {
        Ticket persisted = ticketRepository.save(newTicket());
        entityManager.flush();
        entityManager.clear();

        Ticket reloaded = ticketRepository.findById(persisted.getId()).orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(persisted.getId());
        assertThat(reloaded.getCustomerId()).isEqualTo(customerId);
        assertThat(reloaded.getCreatedBy()).isEqualTo(authorId);
        assertThat(reloaded.getCategoryId()).isEqualTo(SUPORTE);
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(reloaded.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(reloaded.getTitle()).isEqualTo("Impressora não liga");
        assertThat(reloaded.getDescription()).isEqualTo("Detalhe do problema");
        assertThat(reloaded.getOpenedAt()).isEqualTo(OPENED_AT);
    }

    @Test
    void deveGerarTicketNumberPelaSequenciaDaBase() {
        Ticket primeiro = ticketRepository.save(newTicket());
        Ticket segundo = ticketRepository.save(newTicket());

        assertThat(primeiro.getTicketNumber()).isNotNull().isPositive();
        assertThat(segundo.getTicketNumber()).isEqualTo(primeiro.getTicketNumber() + 1);
    }

    @Test
    void devePreencherAAuditoriaNaInsercao() {
        Ticket persisted = ticketRepository.save(newTicket());

        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getVersion()).isZero();
    }

    @Test
    void devePreservarCriacaoEIncrementarVersaoNaAtualizacao() {
        Ticket persisted = ticketRepository.save(newTicket());
        entityManager.flush();
        entityManager.clear();

        Ticket carregado = ticketRepository.findById(persisted.getId()).orElseThrow();
        carregado.changeStatus(TicketStatus.IN_PROGRESS, OPENED_AT.plus(1, ChronoUnit.HOURS));
        Ticket atualizado = ticketRepository.save(carregado);

        assertThat(atualizado.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(atualizado.getVersion()).isEqualTo(persisted.getVersion() + 1);
        assertThat(atualizado.getCreatedAt())
                .isCloseTo(persisted.getCreatedAt(), within(1, ChronoUnit.MICROS));
        assertThat(atualizado.getTicketNumber()).isEqualTo(persisted.getTicketNumber());
    }

    @Test
    void naoDeveAlterarOAutorOriginalNaAtualizacao() {
        Ticket persisted = ticketRepository.save(newTicket());
        entityManager.flush();
        entityManager.clear();

        Ticket carregado = ticketRepository.findById(persisted.getId()).orElseThrow();
        carregado.assignTo(authorId, OPENED_AT.plus(1, ChronoUnit.HOURS));
        Ticket atualizado = ticketRepository.save(carregado);

        assertThat(atualizado.getCreatedBy()).isEqualTo(authorId);
        assertThat(atualizado.getCustomerId()).isEqualTo(customerId);
        assertThat(atualizado.getOpenedAt()).isEqualTo(OPENED_AT);
    }

    @Test
    void deveRecusarEscritaSobreVersaoDesatualizada() {
        Ticket persisted = ticketRepository.save(newTicket());
        entityManager.flush();
        entityManager.clear();

        Ticket copiaDesatualizada = ticketRepository.findById(persisted.getId()).orElseThrow();

        Ticket copiaAtual = ticketRepository.findById(persisted.getId()).orElseThrow();
        copiaAtual.changeStatus(TicketStatus.IN_PROGRESS, OPENED_AT.plus(1, ChronoUnit.HOURS));
        ticketRepository.save(copiaAtual);
        entityManager.flush();
        entityManager.clear();

        copiaDesatualizada.changeStatus(TicketStatus.CANCELLED, OPENED_AT.plus(2, ChronoUnit.HOURS));

        assertThatThrownBy(() -> ticketRepository.save(copiaDesatualizada))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void deveResponderExistsById() {
        Ticket persisted = ticketRepository.save(newTicket());

        assertThat(ticketRepository.existsById(persisted.getId())).isTrue();
        assertThat(ticketRepository.existsById(UuidV7.generate())).isFalse();
    }

    @Test
    void deveDevolverVazioParaIdInexistente() {
        assertThat(ticketRepository.findById(UuidV7.generate())).isEmpty();
    }
}
