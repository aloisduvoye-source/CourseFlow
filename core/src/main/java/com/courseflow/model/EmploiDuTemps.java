package com.courseflow.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    /**
     * Version courante du schéma sérialisé. À incrémenter chaque fois qu'un changement de modèle
     * nécessite une migration des données existantes (voir {@code MigrationSchema} dans le module
     * persistence, qui enchaîne les étapes jusqu'à cette version).
     */
    public static final int VERSION_ACTUELLE = 1;

    /**
     * Une instance nouvellement construite (donnée fraîche, pas encore chargée depuis le disque)
     * est par convention à la version courante. 0 désigne spécifiquement les données antérieures
     * à l'introduction de ce champ (aucune clé "version" dans le JSON) ; c'est {@code DataStore}
     * qui détecte ce cas à la lecture du fichier brut et force cette valeur avant migration, plutôt
     * que de s'appuyer sur cette valeur par défaut (voir {@code DataStore#charger()}).
     */
    private int version = VERSION_ACTUELLE;

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
     * Trouve le créneau correspondant à une date et une heure données (ex. l'instant présent),
     * en tenant compte de la semaine (A/B/toutes) à laquelle appartient chaque créneau.
     */
    public Optional<Creneau> trouverCreneauCourant(LocalDate date, LocalTime heure) {
        DayOfWeek jour = date.getDayOfWeek();
        TypeSemaine semaine = parametres.semainePour(date);
        return creneaux.stream()
                .filter(c -> c.getJour() == jour && c.contient(heure) && c.correspondA(semaine))
                .findFirst();
    }

    /**
     * Résout la liste des {@link Fichier} sélectionnés pour un créneau donné, parmi les
     * fichiers visibles du {@link Cours} associé (possédés ou liés depuis le cours par défaut).
     */
    public List<Fichier> fichiersPourCreneau(Creneau creneau) {
        Optional<Cours> coursAssocie = trouverCours(creneau.getCoursId());
        if (coursAssocie.isEmpty()) {
            return List.of();
        }
        List<UUID> idsSelectionnes = creneau.getFichiersSelectionnesIds();
        return fichiersVisibles(coursAssocie.get()).stream()
                .filter(f -> idsSelectionnes.contains(f.getId()))
                .collect(Collectors.toList());
    }

    /**
     * @return les fichiers possédés par ce cours, plus ceux liés depuis le cours par défaut
     * ({@link Cours#getFichiersLies()}) résolus par leur identifiant. Un lien vers un fichier
     * qui n'existe plus (cours par défaut changé ou fichier supprimé entre-temps) est
     * silencieusement ignoré.
     */
    public List<Fichier> fichiersVisibles(Cours cours) {
        List<Fichier> resultat = new ArrayList<>(cours.getFichiers());
        for (UUID fichierId : cours.getFichiersLies()) {
            trouverFichierPartage(fichierId).ifPresent(resultat::add);
        }
        return resultat;
    }

    private Optional<Fichier> trouverFichierPartage(UUID fichierId) {
        return cours.stream()
                .flatMap(c -> c.getFichiers().stream())
                .filter(f -> f.getId().equals(fichierId))
                .findFirst();
    }

    /**
     * @return le cours désigné comme "cours par défaut" dans {@link Parametres}, s'il existe
     * encore.
     */
    public Optional<Cours> trouverCoursDefaut() {
        UUID coursDefautId = parametres.getCoursDefautId();
        if (coursDefautId == null) {
            return Optional.empty();
        }
        return trouverCours(coursDefautId);
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
