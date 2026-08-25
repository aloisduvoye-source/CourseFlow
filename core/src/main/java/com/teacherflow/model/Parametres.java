package com.teacherflow.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Réglages de l'emploi du temps : quels jours afficher, la plage horaire globale de la
 * grille, l'incrément minimal (en minutes) pour le déplacement/redimensionnement des
 * créneaux, et les blocs horaires prédéfinis (ex. 9h-10h puis 10h20-11h20) dans lesquels
 * un nouveau créneau peut être créé. Ce modèle de blocs est identique pour tous les jours
 * affichés. Contient aussi le vocabulaire de tags disponible pour les fichiers (et leur
 * couleur), et l'ID du cours désigné comme "cours par défaut".
 */
public class Parametres {

    private static final List<DayOfWeek> JOURS_PAR_DEFAUT = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final int PAS_MINUTES_PAR_DEFAUT = 10;
    private static final LocalTime HEURE_DEBUT_GRILLE_PAR_DEFAUT = LocalTime.of(7, 0);
    private static final LocalTime HEURE_FIN_GRILLE_PAR_DEFAUT = LocalTime.of(20, 0);
    private static final List<String> TAGS_PAR_DEFAUT = List.of("dm", "td", "correction", "cm");
    private static final String[] PALETTE_TAGS = {
            "#5DADE2", "#48C9B0", "#F4D03F", "#EB984E", "#EC7063",
            "#AF7AC5", "#5499C7", "#52BE80", "#F39C12", "#CD6155"
    };

    private List<DayOfWeek> joursAffiches = new ArrayList<>(JOURS_PAR_DEFAUT);
    private int pasMinutes = PAS_MINUTES_PAR_DEFAUT;
    private LocalTime heureDebutGrille = HEURE_DEBUT_GRILLE_PAR_DEFAUT;
    private LocalTime heureFinGrille = HEURE_FIN_GRILLE_PAR_DEFAUT;
    private List<PlageHoraire> blocs = new ArrayList<>(blocsParDefaut());
    private List<String> tagsDisponibles = new ArrayList<>(TAGS_PAR_DEFAUT);
    private Map<String, String> couleursTags = new LinkedHashMap<>();
    private UUID coursDefautId;
    private boolean themeSombre = false;
    private boolean afficherGuidesBlocs = true;
    private LocalDate ancrageSemaineA;

    private static List<PlageHoraire> blocsParDefaut() {
        List<PlageHoraire> blocs = new ArrayList<>();
        LocalTime heure = LocalTime.of(8, 0);
        LocalTime fin = LocalTime.of(18, 0);
        while (heure.isBefore(fin)) {
            blocs.add(new PlageHoraire(heure, heure.plusHours(1)));
            heure = heure.plusHours(1);
        }
        return blocs;
    }

    public List<DayOfWeek> getJoursAffiches() {
        return joursAffiches;
    }

    public void setJoursAffiches(List<DayOfWeek> joursAffiches) {
        this.joursAffiches = joursAffiches;
    }

    public int getPasMinutes() {
        return pasMinutes;
    }

    public void setPasMinutes(int pasMinutes) {
        this.pasMinutes = pasMinutes;
    }

    public LocalTime getHeureDebutGrille() {
        return heureDebutGrille;
    }

    public void setHeureDebutGrille(LocalTime heureDebutGrille) {
        this.heureDebutGrille = heureDebutGrille;
    }

    public LocalTime getHeureFinGrille() {
        return heureFinGrille;
    }

    public void setHeureFinGrille(LocalTime heureFinGrille) {
        this.heureFinGrille = heureFinGrille;
    }

    public List<PlageHoraire> getBlocs() {
        return blocs;
    }

    public void setBlocs(List<PlageHoraire> blocs) {
        this.blocs = blocs;
    }

    public List<String> getTagsDisponibles() {
        return tagsDisponibles;
    }

    public void setTagsDisponibles(List<String> tagsDisponibles) {
        this.tagsDisponibles = tagsDisponibles;
    }

    public Map<String, String> getCouleursTags() {
        return couleursTags;
    }

    public void setCouleursTags(Map<String, String> couleursTags) {
        this.couleursTags = couleursTags;
    }

    /**
     * @return la couleur (hexadécimale) du tag donné : celle personnalisée dans
     * {@link #couleursTags} si elle existe, sinon une couleur par défaut déterministe (même tag
     * ⇒ toujours la même couleur) tirée d'une palette fixe, pour que les tags non personnalisés
     * restent visuellement distincts sans configuration.
     */
    public String couleurTag(String tag) {
        String personnalisee = couleursTags.get(tag);
        if (personnalisee != null) {
            return personnalisee;
        }
        int index = Math.abs(tag.toLowerCase(Locale.ROOT).hashCode()) % PALETTE_TAGS.length;
        return PALETTE_TAGS[index];
    }

    public UUID getCoursDefautId() {
        return coursDefautId;
    }

    public void setCoursDefautId(UUID coursDefautId) {
        this.coursDefautId = coursDefautId;
    }

    public boolean isThemeSombre() {
        return themeSombre;
    }

    public void setThemeSombre(boolean themeSombre) {
        this.themeSombre = themeSombre;
    }

    public boolean isAfficherGuidesBlocs() {
        return afficherGuidesBlocs;
    }

    public void setAfficherGuidesBlocs(boolean afficherGuidesBlocs) {
        this.afficherGuidesBlocs = afficherGuidesBlocs;
    }

    public LocalDate getAncrageSemaineA() {
        return ancrageSemaineA;
    }

    public void setAncrageSemaineA(LocalDate ancrageSemaineA) {
        this.ancrageSemaineA = ancrageSemaineA;
    }

    /**
     * @return la semaine (A ou B) à laquelle appartient la date donnée, par parité du nombre de
     * semaines écoulées depuis {@link #ancrageSemaineA}. Si aucune date d'ancrage n'est réglée,
     * retourne toujours {@code A} (comportement neutre : sans ancrage, seuls les créneaux
     * {@code TOUTES} existent, donc rien ne change visuellement).
     */
    public TypeSemaine semainePour(LocalDate date) {
        if (ancrageSemaineA == null) {
            return TypeSemaine.A;
        }
        LocalDate lundiAncrage = ancrageSemaineA.with(DayOfWeek.MONDAY);
        LocalDate lundiCible = date.with(DayOfWeek.MONDAY);
        long semaines = ChronoUnit.WEEKS.between(lundiAncrage, lundiCible);
        return Math.floorMod(semaines, 2) == 0 ? TypeSemaine.A : TypeSemaine.B;
    }
}
