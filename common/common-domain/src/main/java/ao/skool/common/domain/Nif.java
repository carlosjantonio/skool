package ao.skool.common.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Angolan NIF — Número de Identificação Fiscal. Individuals: 10 digits starting with 0.
 * Companies: 10 digits starting with 5. Keep the check loose; tighten per usage.
 */
public record Nif(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{10}$");

    public Nif {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid NIF format: " + value);
        }
    }
}
