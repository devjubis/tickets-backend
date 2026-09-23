package com.devjubis.tickets.shared.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;

class EmailTest {

    @Test
    void deveAceitarEnderecoValido() {
        Email email = new Email("maria.silva@exemplo.com");

        assertThat(email.value()).isEqualTo("maria.silva@exemplo.com");
    }

    @Test
    void deveNormalizarParaMinusculas() {
        Email email = new Email("Maria.Silva@Exemplo.COM");

        assertThat(email.value()).isEqualTo("maria.silva@exemplo.com");
    }

    @Test
    void deveRemoverEspacosEnvolventes() {
        Email email = new Email("   maria@exemplo.com   ");

        assertThat(email.value()).isEqualTo("maria@exemplo.com");
    }

    @Test
    void deveConsiderarIguaisEnderecosQueSoDiferemEmMaiusculas() {
        assertThat(new Email("Maria@Exemplo.com")).isEqualTo(new Email("maria@exemplo.com"));
    }

    @Test
    void deveExporOValorNoToString() {
        assertThat(new Email("maria@exemplo.com")).hasToString("maria@exemplo.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void deveRecusarEnderecoAusente(String enderecoAusente) {
        assertThatThrownBy(() -> new Email(enderecoAusente))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("obrigatório");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sem-arroba.com",
            "@exemplo.com",
            "maria@",
            "maria@exemplo",
            "maria@exemplo.c",
            "maria silva@exemplo.com",
            "maria@exemplo .com",
            "maria@@exemplo.com" })
    void deveRecusarEnderecoInvalido(String enderecoInvalido) {
        assertThatThrownBy(() -> new Email(enderecoInvalido))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void deveRecusarEnderecoAcimaDoLimiteDaColuna() {
        String enderecoLongo = "a".repeat(170) + "@exemplo.com";

        assertThatThrownBy(() -> new Email(enderecoLongo))
                .isInstanceOf(BusinessRuleViolation.class)
                .hasMessageContaining("180");
    }
}
