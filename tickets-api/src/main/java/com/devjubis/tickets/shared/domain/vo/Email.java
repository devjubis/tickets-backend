package com.devjubis.tickets.shared.domain.vo;

import java.util.Locale;
import java.util.regex.Pattern;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;

public record Email(String value) {

    private static final int MAX_LENGTH = 180;

    private static final Pattern PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolation("O email é obrigatório");
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleViolation("O email não pode exceder %d caracteres".formatted(MAX_LENGTH));
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new BusinessRuleViolation("O email é inválido");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
