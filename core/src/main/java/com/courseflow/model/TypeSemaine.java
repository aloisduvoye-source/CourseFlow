package com.courseflow.model;

/**
 * Semaine à laquelle un {@link Creneau} s'applique, pour les emplois du temps en semaines
 * alternées : {@code A} ou {@code B} uniquement, ou {@code TOUTES} pour un créneau récurrent
 * chaque semaine (valeur par défaut, comportement historique de l'application).
 */
public enum TypeSemaine {
    A, B, TOUTES
}
