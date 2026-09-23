package com.devjubis.tickets.shared.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;

class PasswordHashTest {

    private static final String SALT_AND_HASH = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQ";
    private static final String VALID_BCRYPT = "$2a$12$" + SALT_AND_HASH;

    @Test
    void deveAceitarHashBcryptValido() {
        PasswordHash hash = new PasswordHash(VALID_BCRYPT);

        assertThat(hash.value()).isEqualTo(VALID_BCRYPT);
        assertThat(VALID_BCRYPT).hasSize(60);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2a", "2b", "2y" })
    void deveAceitarAsVariantesDoPrefixo(String variante) {
        String hash = "$" + variante + "$12$" + SALT_AND_HASH;

        assertThatCode(() -> new PasswordHash(hash)).doesNotThrowAnyException();
    }

    @Test
    void naoDeveExporOHashNoToString() {
        PasswordHash hash = new PasswordHash(VALID_BCRYPT);

        assertThat(hash.toString()).doesNotContain(SALT_AND_HASH);
        assertThat(hash).hasToString("PasswordHash[protegido]");
    }

    @Test
    void deveManterIgualdadePorValor() {
        assertThat(new PasswordHash(VALID_BCRYPT)).isEqualTo(new PasswordHash(VALID_BCRYPT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void deveRecusarHashAusente(String hashAusente) {
        assertThatThrownBy(() -> new PasswordHash(hashAusente))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    void deveRecusarSenhaEmTextoLimpo() {
        assertThatThrownBy(() -> new PasswordHash("senhaSuperSecreta123"))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("BCrypt");
    }

    @Test
    void deveRecusarPrefixoDesconhecido() {
        assertThatThrownBy(() -> new PasswordHash("$2c$12$" + SALT_AND_HASH))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("BCrypt");
    }

    @Test
    void deveRecusarCustoComUmSoDigito() {
        assertThatThrownBy(() -> new PasswordHash("$2a$1$" + SALT_AND_HASH))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("BCrypt");
    }

    @Test
    void deveRecusarCifrasComComprimentoErrado() {
        String curto = "$2a$12$" + SALT_AND_HASH.substring(0, 52);
        String longo = "$2a$12$" + SALT_AND_HASH + "R";

        assertThatThrownBy(() -> new PasswordHash(curto)).isInstanceOf(BusinessRuleViolation.class);
        assertThatThrownBy(() -> new PasswordHash(longo)).isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void deveRecusarCaractereForaDoAlfabetoBcrypt() {
        String comMais = "$2a$12$" + SALT_AND_HASH.substring(0, 52) + "+";

        assertThatThrownBy(() -> new PasswordHash(comMais)).isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void deveRecusarPrefixoSemCifrao() {
        assertThatThrownBy(() -> new PasswordHash("2a$12$" + SALT_AND_HASH))
                .isInstanceOf(BusinessRuleViolation.class);
    }
}
