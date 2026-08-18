package com.teacherflow.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Un fichier rattaché à un {@link Cours} (support, exercice, corrigé...).
 */
public class Fichier {

    private UUID id;
    private String chemin;
    private String nomAffichage;

    public Fichier() {
    }

    public Fichier(String chemin, String nomAffichage) {
        this.id = UUID.randomUUID();
        this.chemin = chemin;
        this.nomAffichage = nomAffichage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public String getNomAffichage() {
        return nomAffichage;
    }

    public void setNomAffichage(String nomAffichage) {
        this.nomAffichage = nomAffichage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fichier fichier)) return false;
        return Objects.equals(id, fichier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return nomAffichage != null ? nomAffichage : chemin;
    }
}
