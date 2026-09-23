package com.devjubis.tickets.iam.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;
import com.devjubis.tickets.shared.domain.vo.Email;
import com.devjubis.tickets.shared.domain.vo.PasswordHash;

class UserTest {

    private static final Email EMAIL = new Email("maria.silva@exemplo.com");
    private static final PasswordHash HASH =
            new PasswordHash("$2a$12$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQ");
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String NAME = "Maria Silva";

    private static User admin() {
        return User.createAdmin(NAME, EMAIL, HASH);
    }

    private static User agent() {
        return User.createAgent(NAME, EMAIL, HASH);
    }

    private static User requester() {
        return User.createRequester(NAME, EMAIL, HASH, CUSTOMER_ID);
    }

    @Nested
    @DisplayName("criação")
    class Criacao {

        @Test
        void deveCriarAdministradorSemCliente() {
            User user = admin();

            assertThat(user.getId()).isNotNull();
            assertThat(user.getId().version()).isEqualTo(7);
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getPasswordHash()).isEqualTo(HASH);
            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
            assertThat(user.getCustomerId()).isNull();
            assertThat(user.getAvatarUrl()).isNull();
            assertThat(user.isActive()).isTrue();
            assertThat(user.getVersion()).isZero();
        }

        @Test
        void deveCriarAgenteSemCliente() {
            User user = agent();

            assertThat(user.getRoles()).containsExactly(Role.AGENT);
            assertThat(user.getCustomerId()).isNull();
            assertThat(user.isInternalStaff()).isTrue();
            assertThat(user.isRequester()).isFalse();
        }

        @Test
        void deveCriarSolicitanteLigadoAoCliente() {
            User user = requester();

            assertThat(user.getRoles()).containsExactly(Role.REQUESTER);
            assertThat(user.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(user.isRequester()).isTrue();
            assertThat(user.isInternalStaff()).isFalse();
        }

        @Test
        void deveRecusarSolicitanteSemCliente() {
            assertThatThrownBy(() -> User.createRequester(NAME, EMAIL, HASH, null))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("cliente");
        }

        @Test
        void deveRemoverEspacosDoNome() {
            User user = User.createAdmin("   Maria Silva   ", EMAIL, HASH);

            assertThat(user.getName()).isEqualTo(NAME);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "M" })
        void deveRecusarNomeInvalido(String nomeInvalido) {
            assertThatThrownBy(() -> User.createAdmin(nomeInvalido, EMAIL, HASH))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("nome");
        }

        @Test
        void deveRecusarNomeAcimaDoLimite() {
            String nomeLongo = "x".repeat(121);

            assertThatThrownBy(() -> User.createAgent(nomeLongo, EMAIL, HASH))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRecusarEmailAusente() {
            assertThatThrownBy(() -> User.createAdmin(NAME, null, HASH))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("email");
        }

        @Test
        void deveRecusarSenhaAusente() {
            assertThatThrownBy(() -> User.createAdmin(NAME, EMAIL, null))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("senha");
        }

        @Test
        void deveGerarIdentidadesDistintas() {
            assertThat(admin().getId()).isNotEqualTo(admin().getId());
        }
    }

    @Nested
    @DisplayName("invariante de papéis e cliente")
    class InvarianteDePapeis {

        @Test
        void devePermitirAcumularAdminEAgente() {
            User user = admin();

            user.grantRole(Role.AGENT);

            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.AGENT);
            assertThat(user.isInternalStaff()).isTrue();
        }

        @Test
        void deveRecusarSolicitanteComPapelInterno() {
            User user = admin();

            assertThatThrownBy(() -> user.grantRole(Role.REQUESTER))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("acumular papéis internos");
        }

        @Test
        void deveRecusarPapelInternoNumSolicitante() {
            User user = requester();

            assertThatThrownBy(() -> user.grantRole(Role.AGENT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("acumular papéis internos");
        }

        @Test
        void naoDeveAlterarOsPapeisQuandoAInvarianteFalha() {
            User user = admin();

            assertThatThrownBy(() -> user.grantRole(Role.REQUESTER))
                    .isInstanceOf(BusinessRuleViolation.class);

            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        }

        @Test
        void deveRecusarClienteNumUtilizadorInterno() {
            User user = agent();

            assertThatThrownBy(() -> user.assignToCustomer(CUSTOMER_ID))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("Apenas um solicitante");
        }

        @Test
        void devePermitirTrocarOClienteDeUmSolicitante() {
            User user = requester();
            UUID outroCliente = UUID.randomUUID();

            user.assignToCustomer(outroCliente);

            assertThat(user.getCustomerId()).isEqualTo(outroCliente);
            assertThat(user.belongsTo(outroCliente)).isTrue();
            assertThat(user.belongsTo(CUSTOMER_ID)).isFalse();
        }

        @Test
        void deveRecusarClienteAusente() {
            User user = requester();

            assertThatThrownBy(() -> user.assignToCustomer(null))
                    .isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveRevogarPapelQuandoSobraOutro() {
            User user = admin();
            user.grantRole(Role.AGENT);

            user.revokeRole(Role.ADMIN);

            assertThat(user.getRoles()).containsExactly(Role.AGENT);
        }

        @Test
        void deveRecusarRevogarOUnicoPapel() {
            User user = agent();

            assertThatThrownBy(() -> user.revokeRole(Role.AGENT))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("pelo menos um papel");
        }

        @Test
        void deveRecusarRevogarOPapelDeSolicitante() {
            User user = requester();

            assertThatThrownBy(() -> user.revokeRole(Role.REQUESTER))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("pelo menos um papel");
        }

        @Test
        void deveRecusarPapelAusente() {
            User user = admin();

            assertThatThrownBy(() -> user.grantRole(null)).isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.revokeRole(null)).isInstanceOf(BusinessRuleViolation.class);
        }

        @Test
        void deveTolerarConcederUmPapelJaDetido() {
            User user = admin();

            assertThatCode(() -> user.grantRole(Role.ADMIN)).doesNotThrowAnyException();
            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("encapsulamento dos papéis")
    class EncapsulamentoDosPapeis {

        @Test
        void deveDevolverColecaoImutavel() {
            Set<Role> roles = admin().getRoles();

            assertThatThrownBy(() -> roles.add(Role.AGENT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void naoDeveDeixarACopiaAcompanharOAgregado() {
            User user = admin();
            Set<Role> instantaneo = user.getRoles();

            user.grantRole(Role.AGENT);

            assertThat(instantaneo).containsExactly(Role.ADMIN);
            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.AGENT);
        }

        @Test
        void naoDeveDeixarOConjuntoDeOrigemAfetarOAgregado() {
            EnumSet<Role> origem = EnumSet.of(Role.ADMIN);
            User user = User.restore(UUID.randomUUID(), NAME, EMAIL, HASH, null, null, origem, true, null,
                    null, 0L);

            origem.add(Role.AGENT);

            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("ciclo de vida")
    class CicloDeVida {

        @Test
        void deveDesativarEReativar() {
            User user = admin();

            user.deactivate();
            assertThat(user.isActive()).isFalse();

            user.activate();
            assertThat(user.isActive()).isTrue();
        }

        @Test
        void deveRecusarDesativarDuasVezes() {
            User user = admin();
            user.deactivate();

            assertThatThrownBy(user::deactivate)
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("já está inativo");
        }

        @Test
        void deveRecusarAtivarUtilizadorJaAtivo() {
            assertThatThrownBy(admin()::activate)
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("já está ativo");
        }

        @Test
        void deveBloquearTodaAMutacaoEnquantoInativo() {
            User user = requester();
            user.deactivate();

            assertThatThrownBy(() -> user.changeEmail(new Email("outro@exemplo.com")))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("inativo");
            assertThatThrownBy(() -> user.changePasswordHash(HASH))
                    .isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.updateProfile("Outro Nome", null))
                    .isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.grantRole(Role.REQUESTER))
                    .isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.revokeRole(Role.REQUESTER))
                    .isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.assignToCustomer(UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleViolation.class);
        }
    }

    @Nested
    @DisplayName("alteração de dados")
    class AlteracaoDeDados {

        @Test
        void deveAtualizarPerfil() {
            User user = admin();

            user.updateProfile("  Maria S. Silva  ", "https://cdn.exemplo.com/a.png");

            assertThat(user.getName()).isEqualTo("Maria S. Silva");
            assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.exemplo.com/a.png");
        }

        @Test
        void devePermitirPerfilSemAvatar() {
            User user = admin();

            assertThatCode(() -> user.updateProfile("Maria Silva", null)).doesNotThrowAnyException();
            assertThat(user.getAvatarUrl()).isNull();
        }

        @Test
        void deveRecusarAvatarAcimaDoLimite() {
            User user = admin();
            String urlLonga = "x".repeat(513);

            assertThatThrownBy(() -> user.updateProfile(NAME, urlLonga))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("avatar");
        }

        @Test
        void deveTrocarOEmail() {
            User user = admin();
            Email novoEmail = new Email("Nova.Maria@Exemplo.com");

            user.changeEmail(novoEmail);

            assertThat(user.getEmail().value()).isEqualTo("nova.maria@exemplo.com");
        }

        @Test
        void deveTrocarOHashDaSenha() {
            User user = admin();
            PasswordHash novoHash =
                    new PasswordHash("$2b$12$ZYXWVUTSRQPONMLKJIHGFEdcba9876543210zyxwvutsrqponmlkj");

            user.changePasswordHash(novoHash);

            assertThat(user.getPasswordHash()).isEqualTo(novoHash);
        }

        @Test
        void deveRecusarEmailOuSenhaAusentes() {
            User user = admin();

            assertThatThrownBy(() -> user.changeEmail(null)).isInstanceOf(BusinessRuleViolation.class);
            assertThatThrownBy(() -> user.changePasswordHash(null)).isInstanceOf(BusinessRuleViolation.class);
        }
    }

    @Nested
    @DisplayName("reidratação")
    class Reidratacao {

        @Test
        void deveReidratarSemRevalidarOsDados() {
            UUID id = UUID.randomUUID();
            Instant momento = Instant.parse("2026-09-23T10:00:00Z");

            User user = User.restore(id, "X", EMAIL, HASH, null, CUSTOMER_ID, EnumSet.of(Role.REQUESTER),
                    false, momento, momento, 7L);

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getName()).isEqualTo("X");
            assertThat(user.isActive()).isFalse();
            assertThat(user.getCreatedAt()).isEqualTo(momento);
            assertThat(user.getVersion()).isEqualTo(7L);
        }

        @Test
        void deveRecusarReidratarSemPapeis() {
            assertThatThrownBy(() -> User.restore(UUID.randomUUID(), NAME, EMAIL, HASH, null, null, Set.of(),
                    true, null, null, 0L))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("pelo menos um papel");
        }

        @Test
        void deveRecusarReidratarComPapeisNulos() {
            assertThatThrownBy(() -> User.restore(UUID.randomUUID(), NAME, EMAIL, HASH, null, null, null,
                    true, null, null, 0L))
                    .isInstanceOf(BusinessRuleViolation.class)
                    .hasMessageContaining("pelo menos um papel");
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        void deveResponderHasRole() {
            User user = agent();

            assertThat(user.hasRole(Role.AGENT)).isTrue();
            assertThat(user.hasRole(Role.ADMIN)).isFalse();
        }

        @Test
        void naoDeveConsiderarUtilizadorInternoComoPertencenteAUmCliente() {
            assertThat(admin().belongsTo(CUSTOMER_ID)).isFalse();
            assertThat(admin().belongsTo(null)).isFalse();
        }

        @Test
        void deveReconhecerOClienteDoSolicitante() {
            assertThat(requester().belongsTo(CUSTOMER_ID)).isTrue();
        }
    }
}
