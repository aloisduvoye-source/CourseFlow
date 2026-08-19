package com.teacherflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Référence vers un dossier réel du disque attachée à un {@link Cours} : un chemin, un mode
 * de recherche (récursif ou non), et la liste des chemins déjà importés en {@link Fichier}
 * dans le cours — pour qu'une actualisation ultérieure ne propose que les fichiers pas encore
 * importés, sans jamais dupliquer.
 */
public class DossierReference {

    private String chemin;
    private boolean recursif;
    private List<String> fichiersImportes = new ArrayList<>();

    public DossierReference() {
    }

    public DossierReference(String chemin, boolean recursif) {
        this.chemin = chemin;
        this.recursif = recursif;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public boolean isRecursif() {
        return recursif;
    }

    public void setRecursif(boolean recursif) {
        this.recursif = recursif;
    }

    public List<String> getFichiersImportes() {
        return fichiersImportes;
    }

    public void setFichiersImportes(List<String> fichiersImportes) {
        this.fichiersImportes = fichiersImportes;
    }
}
