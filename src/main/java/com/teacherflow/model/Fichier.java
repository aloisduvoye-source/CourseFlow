package com.teacherflow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Un fichier rattaché à un {@link Cours} (support, exercice, corrigé...).
 */
public class Fichier {

    private UUID id;
    private String chemin;
    private String nomAffichage;
    private List<String> tags = new ArrayList<>();
    private String dossier;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDossier() {
        return dossier;
    }

    public void setDossier(String dossier) {
        this.dossier = dossier;
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
