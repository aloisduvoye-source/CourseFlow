package com.courseflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Référence vers un dossier réel du disque attachée à un {@link Cours} : un chemin (toujours
 * un seul niveau, sans sous-dossiers), et la liste des chemins déjà importés en {@link Fichier}
 * dans le cours — pour qu'une actualisation ultérieure ne propose que les fichiers pas encore
 * importés, sans jamais dupliquer. Référencer récursivement crée une {@link DossierReference}
 * distincte par sous-dossier de l'arborescence, plutôt qu'une seule référence "profonde".
 */
public class DossierReference {

    private String chemin;
    private List<String> fichiersImportes = new ArrayList<>();

    public DossierReference() {
    }

    public DossierReference(String chemin) {
        this.chemin = chemin;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public List<String> getFichiersImportes() {
        return fichiersImportes;
    }

    public void setFichiersImportes(List<String> fichiersImportes) {
        this.fichiersImportes = fichiersImportes;
    }
}
