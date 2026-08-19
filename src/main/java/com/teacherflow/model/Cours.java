package com.teacherflow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Un cours réutilisable (ex. "6e A - Mathématiques") avec sa bibliothèque de fichiers.
 * Les {@link Creneau} ne font que sélectionner un sous-ensemble de ces fichiers,
 * ils n'en possèdent pas de copie.
 */
public class Cours {

    private UUID id;
    private String nom;
    private String couleur;
    private List<Fichier> fichiers = new ArrayList<>();

    public Cours() {
    }

    public Cours(String nom, String couleur) {
        this.id = UUID.randomUUID();
        this.nom = nom;
        this.couleur = couleur;
    }

    public Fichier ajouterFichier(String chemin, String nomAffichage) {
        Fichier fichier = new Fichier(chemin, nomAffichage);
        fichiers.add(fichier);
        return fichier;
    }

    public boolean retirerFichier(UUID fichierId) {
        return fichiers.removeIf(f -> f.getId().equals(fichierId));
    }

    public Optional<Fichier> trouverFichier(UUID fichierId) {
        return fichiers.stream().filter(f -> f.getId().equals(fichierId)).findFirst();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public List<Fichier> getFichiers() {
        return fichiers;
    }

    public void setFichiers(List<Fichier> fichiers) {
        this.fichiers = fichiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cours cours)) return false;
        return Objects.equals(id, cours.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return nom;
    }
}
