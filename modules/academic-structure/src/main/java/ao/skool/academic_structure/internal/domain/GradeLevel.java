package ao.skool.academic_structure.internal.domain;

/**
 * Angolan grade levels. Naming follows official "Nª classe" convention.
 * Ciclo groupings:
 *   - Ensino Primário: 1ª–6ª classe
 *   - I Ciclo do Ensino Secundário: 7ª–9ª classe
 *   - II Ciclo do Ensino Secundário: 10ª–13ª classe
 */
public enum GradeLevel {
    CLASSE_1(1, Ciclo.PRIMARIO),
    CLASSE_2(2, Ciclo.PRIMARIO),
    CLASSE_3(3, Ciclo.PRIMARIO),
    CLASSE_4(4, Ciclo.PRIMARIO),
    CLASSE_5(5, Ciclo.PRIMARIO),
    CLASSE_6(6, Ciclo.PRIMARIO),
    CLASSE_7(7, Ciclo.I_CICLO),
    CLASSE_8(8, Ciclo.I_CICLO),
    CLASSE_9(9, Ciclo.I_CICLO),
    CLASSE_10(10, Ciclo.II_CICLO),
    CLASSE_11(11, Ciclo.II_CICLO),
    CLASSE_12(12, Ciclo.II_CICLO),
    CLASSE_13(13, Ciclo.II_CICLO);

    private final int number;
    private final Ciclo ciclo;

    GradeLevel(int number, Ciclo ciclo) {
        this.number = number;
        this.ciclo = ciclo;
    }

    public int number() { return number; }
    public Ciclo ciclo() { return ciclo; }

    public enum Ciclo { PRIMARIO, I_CICLO, II_CICLO }
}
