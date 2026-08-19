package com.teacherflow.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Racine du modèle : l'ensemble des {@link Cours} de l'utilisateur et la semaine type
 * de {@link Creneau} qui les organise.
 */
public class EmploiDuTemps {

    private List<Cours> cours = new ArrayList<>();
    private List<Creneau> creneaux = new ArrayList<>();
    private Parametres parametres = new Parametres();

    public EmploiDuTemps() {
    }

    public Cours ajouterCours(String nom, String couleur) {
        Cours nouveauCours = new Cours(nom, couleur);
        cours.add(nouveauCours);
        return nouveauCours;
    }

    public boolean supprimerCours(UUID coursId) {
        creneaux.removeIf(c -> c.getCoursId().equals(coursId));
        return cours.removeIf(c -> c.getId().equals(coursId));
    }

    public Optional<Cours> trouverCours(UUID coursId) {
        return cours.stream().filter(c -> c.getId().equals(coursId)).findFirst();
    }

    public Creneau ajouterCreneau(DayOfWeek jour, LocalTime heureDebut, LocalTime heureFin, UUID coursId) {
        Creneau creneau = new Creneau(jour, heureDebut, heureFin, coursId);
        creneaux.add(creneau);
        return creneau;
    }

    public boolean supprimerCreneau(UUID creneauId) {
        return creneaux.removeIf(c -> c.getId().equals(creneauId));
    }

    public Optional<Creneau> trouverCreneau(UUID creneauId) {
        return creneaux.stream().filter(c -> c.getId().equals(creneauId)).findFirst();
    }

    /**
     * Trouve le créneau correspondant à un jour et une heure donnés (ex. l'instant présent).
     */
    public Optional<Creneau> trouverCreneauCourant(DayOfWeek jour, LocalTime heure) {
        return creneaux.stream()
                .filter(c -> c.getJour() == jour && c.contient(heure))
                .findFirst();
    }

    /**
     * Résout la liste des {@link Fichier} sélectionnés pour un créneau donné,
     * parmi la bibliothèque du {@link Cours} associé.
     */
    public List<Fichier> fichiersPourCreneau(Creneau creneau) {
        Optional<Cours> coursAssocie = trouverCours(creneau.getCoursId());
        if (coursAssocie.isEmpty()) {
            return List.of();
        }
        List<UUID> idsSelectionnes = creneau.getFichiersSelectionnesIds();
        return coursAssocie.get().getFichiers().stream()
                .filter(f -> idsSelectionnes.contains(f.getId()))
                .collect(Collectors.toList());
    }

    public List<Cours> getCours() {
        return cours;
    }

    public void setCours(List<Cours> cours) {
        this.cours = cours;
    }

    public List<Creneau> getCreneaux() {
        return creneaux;
    }

    public void setCreneaux(List<Creneau> creneaux) {
        this.creneaux = creneaux;
    }

    public Parametres getParametres() {
        return parametres;
    }

    public void setParametres(Parametres parametres) {
        this.parametres = parametres;
    }
}
