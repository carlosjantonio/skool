package ao.skool.documents.api;

/**
 * Categorizes stored files so the UI can filter and the retention policy can apply
 * different rules per category. New categories can be added; renaming one is a
 * migration.
 */
public enum DocumentType {
    BI_SCAN,               // Bilhete de Identidade scan
    VACCINATION_CARD,      // cartão de vacinação
    PREVIOUS_TRANSCRIPT,   // certificado da escola anterior
    STUDENT_PHOTO,
    ASSIGNMENT_SUBMISSION,
    SUBJECT_MATERIAL,
    OTHER
}
