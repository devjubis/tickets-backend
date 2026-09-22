package com.devjubis.tickets.ticketing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;
import com.devjubis.tickets.ticketing.domain.enums.TicketPriority;
import com.devjubis.tickets.ticketing.domain.enums.TicketStatus;
import com.devjubis.tickets.ticketing.domain.exception.InvalidStatusTransition;

class TicketTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();
    private static final Short CATEGORY_ID = 1;
    private static final String VALID_TITLE = "Impressora não liga";
    private static final Instant OPENED_AT = Instant.parse("2026-09-21T10:00:00Z");

    private static Ticket newTicket() {
        return Ticket.open(CUSTOMER_ID, CATEGORY_ID, VALID_TITLE, "Detalhe", TicketPriority.HIGH, AUTHOR_ID,
                OPENED_AT);
    }

    private static Ticket ticketWithStatus(TicketStatus status) {
        return Ticket.restore(UUID.randomUUID(), 1L, CUSTOMER_ID, CATEGORY_ID, status, TicketPriority.MEDIUM,
                VALID_TITLE, "Detalhe", AUTHOR_ID, null, OPENED_AT, null, null, null, OPENED_AT, OPENED_AT,
                0L);
    }

    @Nested
    @DisplayName("abertura")
    class Abertura {

        @Test
        void deveAbrirComEstadoInicialConsistente() {
            Ticket ticket = newTicket();

            assertThat(ticket.getId()).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
            assertThat(ticket.getCreatedBy()).isEqualTo(AUTHOR_ID);
            assertThat(ticket.getOpenedAt()).isEqualTo(OPENED_AT);
            assertThat(ticket.getTicketNumber()).isNull();
            assertThat(ticket.getAssignedTo()).isNull();
            assertThat(ticket.getFirstResponseAt()).isNull();
            assertThat(ticket.isAssigned()).isFalse();
            assertThat(ticket.isTerminal()).isFalse();
        }

        @Test
        void deveGerarIdentidadeUnicaNaVersao7() {
            Ticket primeiro = newTicket();
            Ticket segundo = newTicket();

            assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
            assertThat(primeiro.getId().version()).isEqualTo(7);
            assertThat(primeiro.getId().variant()).isEqualTo(2);
        }

        @Test
        void deveEmbutirOMomentoDaGeracaoNaIdentidade() {
            long antes = System.currentTimeMillis();
            Ticket ticket = newTicket();
            long depois = System.currentTimeMillis();

            long momentoEmbutido = ticket.getId().getMostSignificantBits() >>> 16;

            assertThat(momentoEmbutido).isBetween(antes, depois);
        }

        @Test
        void deveAssumirPrioridadeMediaQuandoNaoInformada() {
            Ticket ticket = Ticket.open(CUSTOMER_ID, CATEGORY_ID, VALID_TITLE, null, null, AUTHOR_ID,
                    OPENED_AT);

            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        }

        @Test
        void deveRemoverEspacosDoTitulo() {
            Ticket ticket = Ticket.open(CUSTOMER_ID, CATEGORY_ID, "   " + VALID_TITLE + "   ", null, null,
                    AUTHOR_ID, OPENED_AT);

            assertThat(ticket.getTitle()).isEqualTo(VALID_TITLE);
        }

        @Test
        void deveRecusarClienteAusente() {
            assertThatThrownBy(() -> Ticket.open(null, CATEGORY_ID, VALID_TITLE, null, null, AUTHOR_ID,
                    OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("cliente");
        }

        @Test
        void deveRecusarCategoriaAusente() {
            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, null, VALID_TITLE, null, null, AUTHOR_ID,
                    OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("categoria");
        }

        @Test
        void deveRecusarAutorAusente() {
            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, VALID_TITLE, null, null, null,
                    OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("autor");
        }

        @Test
        void deveRecusarMomentoDeAberturaAusente() {
            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, VALID_TITLE, null, null, AUTHOR_ID,
                    null))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   ", "abcd" })
        void deveRecusarTituloInvalido(String tituloInvalido) {
            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, tituloInvalido, null, null,
                    AUTHOR_ID, OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("título");
        }

        @Test
        void deveRecusarTituloNulo() {
            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, null, null, null, AUTHOR_ID,
                    OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRecusarTituloAcimaDoLimite() {
            String tituloLongo = "x".repeat(161);

            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, tituloLongo, null, null, AUTHOR_ID,
                    OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRecusarDescricaoAcimaDoLimite() {
            String descricaoLonga = "x".repeat(4001);

            assertThatThrownBy(() -> Ticket.open(CUSTOMER_ID, CATEGORY_ID, VALID_TITLE, descricaoLonga, null,
                    AUTHOR_ID, OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("descrição");
        }
    }

    @Nested
    @DisplayName("transições de estado")
    class Transicoes {

        @Test
        void deveTransitarDeAbertoParaEmProgresso() {
            Ticket ticket = newTicket();
            Instant momento = OPENED_AT.plus(1, ChronoUnit.HOURS);

            ticket.changeStatus(TicketStatus.IN_PROGRESS, momento);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }

        @Test
        void deveRegistarPrimeiraRespostaAoSairDeAberto() {
            Ticket ticket = newTicket();
            Instant momento = OPENED_AT.plus(30, ChronoUnit.MINUTES);

            ticket.changeStatus(TicketStatus.IN_PROGRESS, momento);

            assertThat(ticket.getFirstResponseAt()).isEqualTo(momento);
        }

        @Test
        void naoDeveSobrescreverPrimeiraResposta() {
            Ticket ticket = newTicket();
            Instant primeira = OPENED_AT.plus(30, ChronoUnit.MINUTES);
            ticket.changeStatus(TicketStatus.IN_PROGRESS, primeira);

            ticket.changeStatus(TicketStatus.WAITING_CUSTOMER, primeira.plus(2, ChronoUnit.HOURS));

            assertThat(ticket.getFirstResponseAt()).isEqualTo(primeira);
        }

        @Test
        void deveMarcarResolvidoAoResolver() {
            Ticket ticket = ticketWithStatus(TicketStatus.IN_PROGRESS);
            Instant momento = OPENED_AT.plus(3, ChronoUnit.HOURS);

            ticket.changeStatus(TicketStatus.RESOLVED, momento);

            assertThat(ticket.getResolvedAt()).isEqualTo(momento);
            assertThat(ticket.getClosedAt()).isNull();
        }

        @Test
        void deveMarcarFechadoAoFechar() {
            Ticket ticket = ticketWithStatus(TicketStatus.RESOLVED);
            Instant momento = OPENED_AT.plus(5, ChronoUnit.HOURS);

            ticket.changeStatus(TicketStatus.CLOSED, momento);

            assertThat(ticket.getClosedAt()).isEqualTo(momento);
            assertThat(ticket.isTerminal()).isTrue();
        }

        @Test
        void deveMarcarFechadoAoCancelar() {
            Ticket ticket = ticketWithStatus(TicketStatus.OPEN);
            Instant momento = OPENED_AT.plus(1, ChronoUnit.HOURS);

            ticket.changeStatus(TicketStatus.CANCELLED, momento);

            assertThat(ticket.getClosedAt()).isEqualTo(momento);
            assertThat(ticket.isTerminal()).isTrue();
        }

        @Test
        void deveLimparResolvidoAoReabrir() {
            Ticket ticket = ticketWithStatus(TicketStatus.IN_PROGRESS);
            ticket.changeStatus(TicketStatus.RESOLVED, OPENED_AT.plus(3, ChronoUnit.HOURS));
            assertThat(ticket.getResolvedAt()).isNotNull();

            ticket.changeStatus(TicketStatus.IN_PROGRESS, OPENED_AT.plus(4, ChronoUnit.HOURS));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(ticket.getResolvedAt()).isNull();
        }

        @Test
        void deveRecusarTransicaoDeFechadoParaEmProgresso() {
            Ticket ticket = ticketWithStatus(TicketStatus.CLOSED);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.IN_PROGRESS, OPENED_AT))
                    .isInstanceOf(InvalidStatusTransition.class)
                    .hasMessageContaining("CLOSED")
                    .hasMessageContaining("IN_PROGRESS");
        }

        @ParameterizedTest
        @EnumSource(TicketStatus.class)
        void naoDeveSairDeEstadoTerminal(TicketStatus destino) {
            Ticket fechado = ticketWithStatus(TicketStatus.CLOSED);
            Ticket cancelado = ticketWithStatus(TicketStatus.CANCELLED);

            assertThat(fechado.canTransitionTo(destino)).isFalse();
            assertThat(cancelado.canTransitionTo(destino)).isFalse();
        }

        @Test
        void deveRecusarSaltoDeAbertoParaResolvido() {
            Ticket ticket = newTicket();

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.RESOLVED, OPENED_AT))
                    .isInstanceOf(InvalidStatusTransition.class);
        }

        @Test
        void deveExporEstadosDeOrigemEDestinoNaExcecao() {
            Ticket ticket = ticketWithStatus(TicketStatus.CLOSED);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.OPEN, OPENED_AT))
                    .isInstanceOfSatisfying(InvalidStatusTransition.class, excecao -> {
                        assertThat(excecao.getFromStatus()).isEqualTo(TicketStatus.CLOSED);
                        assertThat(excecao.getToStatus()).isEqualTo(TicketStatus.OPEN);
                    });
        }

        @Test
        void deveRecusarDestinoAusente() {
            Ticket ticket = newTicket();

            assertThatThrownBy(() -> ticket.changeStatus(null, OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRecusarMomentoAusente() {
            Ticket ticket = newTicket();

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.IN_PROGRESS, null))
                    .isInstanceOf(BusinessRuleViolation.class);
        }
    }

    @Nested
    @DisplayName("atribuição")
    class Atribuicao {

        @Test
        void deveAtribuirResponsavel() {
            Ticket ticket = newTicket();
            Instant momento = OPENED_AT.plus(15, ChronoUnit.MINUTES);

            ticket.assignTo(AGENT_ID, momento);

            assertThat(ticket.getAssignedTo()).isEqualTo(AGENT_ID);
            assertThat(ticket.isAssigned()).isTrue();
            assertThat(ticket.getFirstResponseAt()).isEqualTo(momento);
        }

        @Test
        void deveRemoverResponsavel() {
            Ticket ticket = newTicket();
            ticket.assignTo(AGENT_ID, OPENED_AT);

            ticket.unassign();

            assertThat(ticket.getAssignedTo()).isNull();
            assertThat(ticket.isAssigned()).isFalse();
        }

        @Test
        void deveRecusarAtribuicaoEmTicketFechado() {
            Ticket ticket = ticketWithStatus(TicketStatus.CLOSED);

            assertThatThrownBy(() -> ticket.assignTo(AGENT_ID, OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("CLOSED");
        }

        @Test
        void deveRecusarRemocaoEmTicketCancelado() {
            Ticket ticket = ticketWithStatus(TicketStatus.CANCELLED);

            assertThatThrownBy(ticket::unassign).isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRecusarResponsavelAusente() {
            Ticket ticket = newTicket();

            assertThatThrownBy(() -> ticket.assignTo(null, OPENED_AT))
                    .isInstanceOf(BusinessRuleViolation.class);
        }
    }

    @Nested
    @DisplayName("edição")
    class Edicao {

        @Test
        void deveAtualizarDetalhes() {
            Ticket ticket = newTicket();

            ticket.updateDetails("Novo título do chamado", "Nova descrição", TicketPriority.CRITICAL,
                    (short) 2);

            assertThat(ticket.getTitle()).isEqualTo("Novo título do chamado");
            assertThat(ticket.getDescription()).isEqualTo("Nova descrição");
            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.CRITICAL);
            assertThat(ticket.getCategoryId()).isEqualTo((short) 2);
        }

        @Test
        void devePreservarPrioridadeQuandoNaoInformada() {
            Ticket ticket = newTicket();

            ticket.updateDetails("Novo título do chamado", null, null, CATEGORY_ID);

            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        }

        @Test
        void deveRecusarEdicaoEmTicketResolvidoEFechado() {
            Ticket ticket = ticketWithStatus(TicketStatus.CLOSED);

            assertThatThrownBy(() -> ticket.updateDetails("Novo título do chamado", null, null, CATEGORY_ID))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void devePermitirEdicaoEmTicketResolvido() {
            Ticket ticket = ticketWithStatus(TicketStatus.RESOLVED);

            assertThatCode(() -> ticket.updateDetails("Novo título do chamado", null, null, CATEGORY_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        void deveRecusarTituloInvalidoNaEdicao() {
            Ticket ticket = newTicket();

            assertThatThrownBy(() -> ticket.updateDetails("abc", null, null, CATEGORY_ID))
                    .isInstanceOf(BusinessRuleViolation.class);
        }
    }

    @Nested
    @DisplayName("pertença a cliente")
    class Pertenca {

        @Test
        void deveReconhecerOProprioCliente() {
            Ticket ticket = newTicket();

            assertThat(ticket.belongsTo(CUSTOMER_ID)).isTrue();
        }

        @Test
        void deveRecusarClienteDiferente() {
            Ticket ticket = newTicket();

            assertThat(ticket.belongsTo(UUID.randomUUID())).isFalse();
        }
    }
}
