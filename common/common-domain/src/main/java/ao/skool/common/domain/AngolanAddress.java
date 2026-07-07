package ao.skool.common.domain;

/**
 * Angolan administrative address: Província → Município → Comuna/Bairro.
 * Line1 covers rua/avenida + número; complement is optional.
 */
public record AngolanAddress(
        String provincia,
        String municipio,
        String comunaOuBairro,
        String line1,
        String complement
) {}
