package com.courseflow.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Un créneau de l'emploi du temps type (un jour + une plage horaire) associé à un {@link Cours},
 * avec la sélection des fichiers de ce cours à utiliser pour cette séance précise.
 */
public class Creneau {

    private UUID id;
    private DayOfWeek jour;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private UUID coursId;
    private List<UUID> fichiersSelectionnesIds = new ArrayList<>();
    private String salle;
    private String description;
    private TypeSemaine typeSemaine = TypeSemaine.TOUTES;

    public Creneau() {
    }

    public Creneau(DayOfWeek jour, LocalTime heureDebut, LocalTime heureFin, UUID coursId) {
        this.id = UUID.randomUUID();
        this.jour = jour;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.coursId = coursId;
    }

    /**
     * @return true si l'heure donnée tombe dans ce créneau (borne de fin exclue).
     */
    public boolean contient(LocalTime heure) {
        return !heure.isBefore(heureDebut) && heure.isBefore(heureFin);
    }

    /**
     * @return true si ce créneau s'applique à la semaine donnée (soit parce qu'il est réglé
     * sur {@link TypeSemaine#TOUTES}, soit parce qu'il correspond exactement).
     */
    public boolean correspondA(TypeSemaine semaineActuelle) {
        return typeSemaine == TypeSemaine.TOUTES || typeSemaine == semaineActuelle;
    }

    public void selectionnerFichier(UUID fichierId) {
        if (!fichiersSelectionnesIds.contains(fichierId)) {
            fichiersSelectionnesIds.add(fichierId);
        }
    }

    public void deselectionnerFichier(UUID fichierId) {
        fichiersSelectionnesIds.remove(fichierId);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DayOfWeek getJour() {
        return jour;
    }

    public void setJour(DayOfWeek jour) {
        this.jour = jour;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public UUID getCoursId() {
        return coursId;
    }

    public void setCoursId(UUID coursId) {
        this.coursId = coursId;
    }

    public List<UUID> getFichiersSelectionnesIds() {
        return fichiersSelectionnesIds;
    }

    public void setFichiersSelectionnesIds(List<UUID> fichiersSelectionnesIds) {
        this.fichiersSelectionnesIds = fichiersSelectionnesIds;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String salle) {
        this.salle = salle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypeSemaine getTypeSemaine() {
        return typeSemaine;
    }

    public void setTypeSemaine(TypeSemaine typeSemaine) {
        this.typeSemaine = typeSemaine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Creneau creneau)) return false;
        return Objects.equals(id, creneau.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return jour + " " + heureDebut + "-" + heureFin;
    }
}
