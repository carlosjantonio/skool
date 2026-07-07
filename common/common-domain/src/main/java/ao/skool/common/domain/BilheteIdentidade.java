package ao.skool.common.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Angolan Bilhete de Identidade (BI). Format: 9 digits + 2 letters + 3 digits, e.g. 001234567LA042.
 * Validation is loose here — accept the historical format; the SIS module can tighten it.
 */
public record BilheteIdentidade(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{9}[A-Z]{2}\\d{3}$");

    public BilheteIdentidade {
        Objects.requireNonNull(value, "value");
        value = value.trim().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid BI format: " + value);
        }
    }
}
