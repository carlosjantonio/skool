package ao.skool.sis.internal.domain;

/**
 * Relationship of a guardian to the student. Names in pt-AO but stored as SCREAMING_SNAKE
 * so the DB is language-neutral.
 */
public enum GuardianRelationship {
    PAI,          // father
    MAE,          // mother
    AVO,          // grandparent
    IRMAO,        // sibling
    TIO,          // uncle/aunt
    TUTOR,        // legal guardian
    OUTRO         // other
}
