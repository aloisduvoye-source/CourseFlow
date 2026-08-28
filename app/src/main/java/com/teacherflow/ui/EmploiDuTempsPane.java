package com.teacherflow.ui;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.Parametres;
import com.teacherflow.model.PlageHoraire;
import com.teacherflow.model.TypeSemaine;
import com.teacherflow.util.NomsJours;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Grille hebdomadaire (jours × heures) permettant d'assigner un {@link Cours} à chaque
 * {@link Creneau}, puis de le déplacer ou de le redimensionner directement à la souris,
 * ou de le modifier/supprimer via une boîte de dialogue. Les jours affichés, l'incrément
 * de déplacement et les plages horaires actives par jour viennent de {@link Parametres}.
 */
public class EmploiDuTempsPane extends BorderPane {

    private static final int PAS_AFFICHAGE_MINUTES = 30;
    private static final int DUREE_MIN_MINUTES = 10;
    private static final double PIXELS_PAR_MINUTE = 1.5;
    private static final double LARGEUR_COLONNE_HEURES = 60;
    private static final double LARGEUR_COLONNE_JOUR_MIN = 110;
    private static final double LARGEUR_COLONNE_JOUR_DEFAUT = 140;
    private static final double HAUTEUR_POIGNEE = 6;
    private static final double SEUIL_CLIC_PIXELS = 4;
    private static final double MARGE_VERTICALE = 10;

    private enum ModeInteraction { DEPLACER, REDIMENSIONNER_HAUT, REDIMENSIONNER_BAS }

    private final EmploiDuTemps emploiDuTemps;
    private final Runnable surChangement;
    private final HBox ligneEntetes = new HBox();
    private final HBox ligneColonnes = new HBox();
    private final ScrollPane defilement = new ScrollPane();
    private double largeurColonneJour = LARGEUR_COLONNE_JOUR_DEFAUT;
    private DayOfWeek[] joursAffiches = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private int pasMinutes = 10;
    private LocalTime heureDebutGrille = LocalTime.of(7, 0);
    private LocalTime heureFinGrille = LocalTime.of(20, 0);
    private final Deque<List<Creneau>> pileAnnuler = new ArrayDeque<>();
    private final Deque<List<Creneau>> pileRetablir = new ArrayDeque<>();
    private TypeSemaine semaineAffichee;

    public EmploiDuTempsPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;
        this.semaineAffichee = emploiDuTemps.getParametres().semainePour(LocalDate.now());

        setPadding(new Insets(16));
        setFocusTraversable(true);
        setOnKeyPressed(this::gererRaccourciClavier);

        defilement.setContent(ligneColonnes);
        defilement.setFitToWidth(false);
        defilement.viewportBoundsProperty().addListener((obs, ancien, nouveau) -> rafraichir());

        VBox.setVgrow(defilement, Priority.ALWAYS);
        setCenter(new VBox(construireSelecteurSemaine(), ligneEntetes, defilement));

        rafraichir();
    }

    /**
     * Ouvre la boîte de dialogue d'édition pour un créneau existant, depuis un autre écran
     * (ex. le raccourci "Modifier" de l'Accueil) sans passer par un clic sur son bloc.
     */
    public void ouvrirEdition(Creneau creneau) {
        if (creneau == null) {
            return;
        }
        if (creneau.getTypeSemaine() != TypeSemaine.TOUTES && creneau.getTypeSemaine() != semaineAffichee) {
            semaineAffichee = creneau.getTypeSemaine();
            rafraichir();
        }
        ouvrirDialogueCreneau(creneau, creneau.getJour(), creneau.getHeureDebut(), creneau.getHeureFin());
    }

    private HBox construireSelecteurSemaine() {
        ToggleGroup groupe = new ToggleGroup();
        ToggleButton boutonA = new ToggleButton("Semaine A");
        ToggleButton boutonB = new ToggleButton("Semaine B");
        boutonA.setToggleGroup(groupe);
        boutonB.setToggleGroup(groupe);
        boutonA.setSelected(semaineAffichee == TypeSemaine.A);
        boutonB.setSelected(semaineAffichee == TypeSemaine.B);
        boutonA.setOnAction(e -> { semaineAffichee = TypeSemaine.A; rafraichir(); });
        boutonB.setOnAction(e -> { semaineAffichee = TypeSemaine.B; rafraichir(); });

        CheckBox caseGuides = new CheckBox("Afficher les guides");
        caseGuides.setSelected(emploiDuTemps.getParametres().isAfficherGuidesBlocs());
        caseGuides.setOnAction(e -> {
            emploiDuTemps.getParametres().setAfficherGuidesBlocs(caseGuides.isSelected());
            notifierChangement();
            rafraichir();
        });

        Region espaceur = new Region();
        HBox.setHgrow(espaceur, Priority.ALWAYS);

        HBox ligne = new HBox(8, boutonA, boutonB, espaceur, caseGuides);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(0, 0, 8, 0));
        return ligne;
    }

    private void gererRaccourciClavier(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.Z) {
            if (e.isShiftDown()) {
                retablir();
            } else {
                annuler();
            }
            e.consume();
        }
    }

    private void enregistrerAvantModification() {
        pileAnnuler.push(copierCreneaux());
        pileRetablir.clear();
    }

    private void annuler() {
        if (pileAnnuler.isEmpty()) {
            return;
        }
        pileRetablir.push(copierCreneaux());
        restaurer(pileAnnuler.pop());
    }

    private void retablir() {
        if (pileRetablir.isEmpty()) {
            return;
        }
        pileAnnuler.push(copierCreneaux());
        restaurer(pileRetablir.pop());
    }

    private void restaurer(List<Creneau> etat) {
        emploiDuTemps.setCreneaux(etat);
        notifierChangement();
        rafraichir();
    }

    private List<Creneau> copierCreneaux() {
        return emploiDuTemps.getCreneaux().stream().map(EmploiDuTempsPane::copierCreneau).collect(Collectors.toList());
    }

    private static Creneau copierCreneau(Creneau original) {
        Creneau copie = new Creneau(original.getJour(), original.getHeureDebut(), original.getHeureFin(), original.getCoursId());
        copie.setId(original.getId());
        copie.setSalle(original.getSalle());
        copie.setDescription(original.getDescription());
        copie.setFichiersSelectionnesIds(new ArrayList<>(original.getFichiersSelectionnesIds()));
        copie.setTypeSemaine(original.getTypeSemaine());
        return copie;
    }

    private void mettreAJourEntetes() {
        ligneEntetes.getChildren().clear();
        ligneEntetes.getChildren().add(espace(LARGEUR_COLONNE_HEURES));
        LocalDate lundiCourant = LocalDate.now().with(DayOfWeek.MONDAY);
        boolean aujourdhuiAffiche = indexDuJour(LocalDate.now().getDayOfWeek()) >= 0;
        for (DayOfWeek jour : joursAffiches) {
            boolean estAujourdhui = aujourdhuiAffiche && jour == LocalDate.now().getDayOfWeek();

            Label nomJour = new Label(NomsJours.nom(jour));
            nomJour.setStyle("-fx-font-weight: bold; -fx-font-size: 13;"
                    + (estAujourdhui ? " -fx-text-fill: -color-accent-emphasis;" : ""));

            Label date = new Label(lundiCourant.plusDays(jour.getValue() - 1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("d/M")));
            date.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11;");

            VBox enTete = new VBox(2, nomJour, date);
            enTete.setAlignment(Pos.CENTER);
            enTete.setPrefWidth(largeurColonneJour);
            enTete.setPadding(new Insets(8, 4, 8, 4));
            enTete.setStyle("-fx-background-color: " + (estAujourdhui ? "-color-accent-subtle" : "-color-bg-subtle")
                    + "; -fx-background-radius: 6 6 0 0; -fx-border-color: -color-border-default;"
                    + " -fx-border-width: 1 1 0 1; -fx-border-radius: 6 6 0 0;");
            ligneEntetes.getChildren().add(enTete);
        }
    }

    private static Region espace(double largeur) {
        Region region = new Region();
        region.setPrefWidth(largeur);
        return region;
    }

    public void rafraichir() {
        Parametres parametres = emploiDuTemps.getParametres();
        List<DayOfWeek> jours = parametres.getJoursAffiches();
        if (!jours.isEmpty()) {
            joursAffiches = jours.toArray(new DayOfWeek[0]);
        }
        pasMinutes = Math.max(1, parametres.getPasMinutes());
        heureDebutGrille = parametres.getHeureDebutGrille();
        heureFinGrille = parametres.getHeureFinGrille();

        largeurColonneJour = calculerLargeurColonneJour();
        mettreAJourEntetes();

        ligneColonnes.getChildren().clear();

        double hauteurGrille = minutesGrille() * PIXELS_PAR_MINUTE + 2 * MARGE_VERTICALE;

        ligneColonnes.getChildren().add(construireColonneHeures(hauteurGrille));
        ligneColonnes.getChildren().add(construireGrilleUnique(hauteurGrille));
    }

    private double calculerLargeurColonneJour() {
        Bounds viewport = defilement.getViewportBounds();
        double largeurDisponible = viewport != null ? viewport.getWidth() : 0;
        if (largeurDisponible <= 0) {
            return LARGEUR_COLONNE_JOUR_DEFAUT;
        }
        double largeurJours = largeurDisponible - LARGEUR_COLONNE_HEURES;
        return Math.max(LARGEUR_COLONNE_JOUR_MIN, largeurJours / joursAffiches.length);
    }

    private Pane construireColonneHeures(double hauteur) {
        Pane pane = new Pane();
        pane.setPrefSize(LARGEUR_COLONNE_HEURES, hauteur);
        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            LocalTime heure = heureDebutGrille.plusMinutes(minute);
            if (heure.getMinute() == 0) {
                Label label = new Label(heure.toString());
                label.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10;");
                label.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE - 6);
                label.setLayoutX(4);
                pane.getChildren().add(label);
            }
        }
        return pane;
    }

   
    private Pane construireGrilleUnique(double hauteur) {
        double largeurTotale = joursAffiches.length * largeurColonneJour;
        Pane pane = new Pane();
        pane.setPrefSize(largeurTotale, hauteur);
        pane.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 0 1;");

        int indexAujourdhui = indexDuJour(LocalDate.now().getDayOfWeek());
        if (indexAujourdhui >= 0) {
            Region surbrillance = new Region();
            surbrillance.setPrefSize(largeurColonneJour, hauteur);
            surbrillance.setLayoutX(indexAujourdhui * largeurColonneJour);
            surbrillance.setStyle("-fx-background-color: -color-bg-subtle;");
            surbrillance.setMouseTransparent(true);
            pane.getChildren().add(surbrillance);
        }

        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            Region ligne = new Region();
            ligne.setPrefSize(largeurTotale, 1);
            ligne.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE);
            ligne.setStyle("-fx-background-color: -color-border-muted;");
            ligne.setMouseTransparent(true);
            pane.getChildren().add(ligne);
        }
        for (int i = 1; i < joursAffiches.length; i++) {
            Region separateur = new Region();
            separateur.setPrefSize(1, hauteur);
            separateur.setLayoutX(i * largeurColonneJour);
            separateur.setStyle("-fx-background-color: -color-border-default;");
            separateur.setMouseTransparent(true);
            pane.getChildren().add(separateur);
        }

        ajouterCellulesCliquables(pane);

        List<Creneau> creneauxVisibles = emploiDuTemps.getCreneaux().stream()
                .filter(c -> indexDuJour(c.getJour()) >= 0)
                .filter(c -> chevaucheGrille(c.getHeureDebut(), c.getHeureFin()))
                .filter(c -> c.correspondA(semaineAffichee))
                .collect(Collectors.toList());
        creneauxVisibles.forEach(c -> pane.getChildren().add(new BlocCreneau(c)));

        for (int jourIndex = 0; jourIndex < joursAffiches.length; jourIndex++) {
            DayOfWeek jour = joursAffiches[jourIndex];
            boolean journeeVide = creneauxVisibles.stream().noneMatch(c -> c.getJour() == jour);
            if (journeeVide) {
                Label vide = new Label("Journée libre");
                vide.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-font-size: 12;");
                vide.setPrefWidth(largeurColonneJour);
                vide.setAlignment(Pos.CENTER);
                vide.setMouseTransparent(true);
                vide.setLayoutX(jourIndex * largeurColonneJour);
                vide.setLayoutY(hauteur / 2 - 8);
                pane.getChildren().add(vide);
            }
        }

        return pane;
    }

    private void ajouterCellulesCliquables(Pane pane) {
        List<PlageHoraire> blocs = new ArrayList<>(emploiDuTemps.getParametres().getBlocs());
        blocs.sort(Comparator.comparing(PlageHoraire::getDebut));
        int debutGrille = toMinutes(heureDebutGrille);
        boolean guidesVisibles = emploiDuTemps.getParametres().isAfficherGuidesBlocs();
        String pourcentageAlternance = emploiDuTemps.getParametres().isThemeSombre() ? "-25%" : "25%";
        double opaciteRepos = guidesVisibles ? 0.6 : 0;
        double opaciteSurvol = guidesVisibles ? 0.9 : 0.35;
        for (int jourIndex = 0; jourIndex < joursAffiches.length; jourIndex++) {
            DayOfWeek jour = joursAffiches[jourIndex];
            for (int i = 0; i < blocs.size(); i++) {
                PlageHoraire bloc = blocs.get(i);
                int minuteDebut = Math.max(0, toMinutes(bloc.getDebut()) - debutGrille);
                int minuteFin = Math.min(minutesGrille(), toMinutes(bloc.getFin()) - debutGrille);
                if (minuteFin <= minuteDebut) {
                    continue;
                }

                Region cellule = new Region();
                cellule.setLayoutX(jourIndex * largeurColonneJour);
                cellule.setLayoutY(MARGE_VERTICALE + minuteDebut * PIXELS_PAR_MINUTE);
                cellule.setPrefSize(largeurColonneJour, (minuteFin - minuteDebut) * PIXELS_PAR_MINUTE);
                cellule.setCursor(Cursor.HAND);
                cellule.setStyle("-fx-background-color: " + (i % 2 == 1
                        ? "derive(-color-accent-subtle, " + pourcentageAlternance + ");" : "-color-accent-subtle;"));
                cellule.setOpacity(opaciteRepos);
                cellule.setOnMouseEntered(e -> cellule.setOpacity(opaciteSurvol));
                cellule.setOnMouseExited(e -> cellule.setOpacity(opaciteRepos));
                cellule.setOnMouseClicked(e -> ouvrirDialogueCreneau(null, jour, bloc.getDebut(), bloc.getFin()));
                pane.getChildren().add(cellule);
            }
        }
    }

    private int minutesGrille() {
        return (int) Duration.between(heureDebutGrille, heureFinGrille).toMinutes();
    }

    private boolean chevaucheGrille(LocalTime debut, LocalTime fin) {
        return fin.isAfter(heureDebutGrille) && debut.isBefore(heureFinGrille);
    }

    private int indexDuJour(DayOfWeek jour) {
        for (int i = 0; i < joursAffiches.length; i++) {
            if (joursAffiches[i] == jour) {
                return i;
            }
        }
        return -1;
    }

    private final class BlocCreneau extends StackPane {

        private final Creneau creneau;
        private final Label titre = new Label();
        private final Label meta = new Label();
        private final VBox contenu = new VBox(2, titre, meta);

        private int dayIndexInitial;
        private int minutesDebutInitial;
        private int minutesFinInitial;
        private int dayIndexCourant;
        private double pressSceneX;
        private double pressSceneY;
        private boolean enTrainDeBouger;
        private ModeInteraction mode;

        BlocCreneau(Creneau creneau) {
            this.creneau = creneau;

            titre.setWrapText(true);
            titre.setMouseTransparent(true);
            titre.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            meta.setWrapText(true);
            meta.setMouseTransparent(true);
            meta.setStyle("-fx-text-fill: rgba(255,255,255,0.88); -fx-font-size: 10.5;");
            contenu.setMouseTransparent(true);
            getChildren().add(contenu);
            setAlignment(Pos.TOP_LEFT);
            setPadding(new Insets(6, 6, 6, 8));
            setPrefWidth(largeurColonneJour - 6);
            setStyle(styleFond());

            actualiser(Math.max(0, indexDuJour(creneau.getJour())),
                    toMinutes(creneau.getHeureDebut()), toMinutes(creneau.getHeureFin()));

            setOnMousePressed(this::surAppui);
            setOnMouseDragged(this::surGlissement);
            setOnMouseReleased(this::surRelachement);
            setOnMouseMoved(this::surSurvol);
            setOnMouseClicked(Event::consume);
        }

        private String styleFond() {
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String couleur = cours != null && cours.getCouleur() != null ? cours.getCouleur() : "#95a5a6";
            return "-fx-background-color: " + couleur + "; -fx-background-radius: 6;"
                    + " -fx-border-color: derive(" + couleur + ", -15%); -fx-border-width: 1; -fx-border-radius: 6;";
        }

        private void actualiser(int dayIndex, int debutMinutes, int finMinutes) {
            dayIndexCourant = dayIndex;
            int grilleDebut = toMinutes(heureDebutGrille);
            int grilleFin = toMinutes(heureFinGrille);
            int debutAffiche = clamp(debutMinutes, grilleDebut, grilleFin);
            int finAffiche = clamp(finMinutes, grilleDebut, grilleFin);
            setLayoutX(dayIndex * largeurColonneJour + 3);
            setLayoutY(MARGE_VERTICALE + (debutAffiche - grilleDebut) * PIXELS_PAR_MINUTE);
            setPrefHeight(Math.max(DUREE_MIN_MINUTES, finAffiche - debutAffiche) * PIXELS_PAR_MINUTE);
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";
            titre.setText(nomCours);

            StringBuilder texte = new StringBuilder();
            texte.append(minutesVersHeure(debutMinutes)).append(" - ").append(minutesVersHeure(finMinutes));
            if (creneau.getSalle() != null && !creneau.getSalle().isBlank()) {
                texte.append('\n').append(creneau.getSalle());
            }
            if (creneau.getTypeSemaine() != TypeSemaine.TOUTES) {
                texte.append(" (").append(creneau.getTypeSemaine()).append(')');
            }
            if (creneau.getDescription() != null && !creneau.getDescription().isBlank()) {
                texte.append('\n').append(creneau.getDescription());
            }
            meta.setText(texte.toString());
        }

        private ModeInteraction determinerMode(double y) {
            if (y <= HAUTEUR_POIGNEE) {
                return ModeInteraction.REDIMENSIONNER_HAUT;
            }
            if (y >= getHeight() - HAUTEUR_POIGNEE) {
                return ModeInteraction.REDIMENSIONNER_BAS;
            }
            return ModeInteraction.DEPLACER;
        }

        private void surSurvol(MouseEvent e) {
            setCursor(determinerMode(e.getY()) == ModeInteraction.DEPLACER ? Cursor.MOVE : Cursor.V_RESIZE);
        }

        private void surAppui(MouseEvent e) {
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            dayIndexInitial = Math.max(0, indexDuJour(creneau.getJour()));
            minutesDebutInitial = toMinutes(creneau.getHeureDebut());
            minutesFinInitial = toMinutes(creneau.getHeureFin());
            mode = determinerMode(e.getY());
            enTrainDeBouger = false;
            e.consume();
        }

        private void surGlissement(MouseEvent e) {
            double deltaX = e.getSceneX() - pressSceneX;
            double deltaY = e.getSceneY() - pressSceneY;
            if (Math.abs(deltaX) > SEUIL_CLIC_PIXELS || Math.abs(deltaY) > SEUIL_CLIC_PIXELS) {
                enTrainDeBouger = true;
            }
            if (!enTrainDeBouger) {
                e.consume();
                return;
            }

            long pas = Math.round((deltaY / PIXELS_PAR_MINUTE) / pasMinutes);
            int deltaMinutes = (int) (pas * pasMinutes);
            int minGrille = toMinutes(heureDebutGrille);
            int maxGrille = toMinutes(heureFinGrille);

            int nouveauDayIndex = dayIndexInitial;
            int nouveauDebut = minutesDebutInitial;
            int nouveauFin = minutesFinInitial;
            switch (mode) {
                case DEPLACER -> {
                    int duree = minutesFinInitial - minutesDebutInitial;
                    nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, maxGrille - duree);
                    nouveauFin = nouveauDebut + duree;
                    int deltaJours = (int) Math.round(deltaX / largeurColonneJour);
                    nouveauDayIndex = clamp(dayIndexInitial + deltaJours, 0, joursAffiches.length - 1);
                }
                case REDIMENSIONNER_HAUT ->
                        nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, minutesFinInitial - DUREE_MIN_MINUTES);
                case REDIMENSIONNER_BAS ->
                        nouveauFin = clamp(minutesFinInitial + deltaMinutes, minutesDebutInitial + DUREE_MIN_MINUTES, maxGrille);
            }

            actualiser(nouveauDayIndex, nouveauDebut, nouveauFin);
            e.consume();
        }

        private void surRelachement(MouseEvent e) {
            if (!enTrainDeBouger) {
                ouvrirDialogueCreneau(creneau, creneau.getJour(), creneau.getHeureDebut(), creneau.getHeureFin());
                e.consume();
                return;
            }

            int debutFinal = toMinutes(heureDebutGrille) + (int) Math.round((getLayoutY() - MARGE_VERTICALE) / PIXELS_PAR_MINUTE);
            int finFinal = debutFinal + (int) Math.round(getPrefHeight() / PIXELS_PAR_MINUTE);
            enregistrerAvantModification();
            creneau.setJour(joursAffiches[dayIndexCourant]);
            creneau.setHeureDebut(minutesVersHeure(debutFinal));
            creneau.setHeureFin(minutesVersHeure(finFinal));
            notifierChangement();
            // Reconstruit toute la grille (pas seulement ce bloc) : le jour de départ peut être
            // devenu vide (ou le jour d'arrivée ne plus l'être), ce qui doit mettre à jour le
            // libellé "Journée libre"/"Pas de cours" affiché pour ce jour.
            rafraichir();
            e.consume();
        }
    }

    private void ouvrirDialogueCreneau(Creneau creneauExistant, DayOfWeek jour, LocalTime heureDebutParDefaut,
            LocalTime heureFinParDefaut) {
        if (emploiDuTemps.getCours().isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION,
                    "Crée d'abord un cours dans l'onglet \"Cours\" avant de remplir l'emploi du temps.");
            alerte.setTitle("Aucun cours disponible");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }

        List<LocalTime> limites = genererLimites();
        List<LocalTime> optionsDebut = avecValeur(limites.subList(0, limites.size() - 1),
                creneauExistant != null ? creneauExistant.getHeureDebut() : heureDebutParDefaut);
        List<LocalTime> optionsFin = avecValeur(limites.subList(1, limites.size()),
                creneauExistant != null ? creneauExistant.getHeureFin() : heureFinParDefaut);

        ComboBox<Cours> choixCours = new ComboBox<>();
        choixCours.getItems().addAll(emploiDuTemps.getCours());
        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(optionsDebut);
        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(optionsFin);
        TextField champSalle = new TextField();
        champSalle.setPromptText("Salle (optionnel)");
        TextField champDescription = new TextField();
        champDescription.setPromptText("Description (optionnel)");

        ComboBox<TypeSemaine> choixSemaine = new ComboBox<>();
        choixSemaine.getItems().addAll(TypeSemaine.TOUTES, TypeSemaine.A, TypeSemaine.B);
        choixSemaine.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeSemaine typeSemaine) {
                return switch (typeSemaine) {
                    case TOUTES -> "Toutes les semaines";
                    case A -> "Semaine A";
                    case B -> "Semaine B";
                };
            }

            @Override
            public TypeSemaine fromString(String texte) {
                return null;
            }
        });
        choixSemaine.setValue(creneauExistant != null ? creneauExistant.getTypeSemaine() : TypeSemaine.TOUTES);

        Set<UUID> fichiersCoches = new LinkedHashSet<>();
        Cours coursInitial;
        if (creneauExistant != null) {
            coursInitial = emploiDuTemps.trouverCours(creneauExistant.getCoursId())
                    .orElse(emploiDuTemps.getCours().get(0));
            fichiersCoches.addAll(creneauExistant.getFichiersSelectionnesIds());
            choixDebut.setValue(creneauExistant.getHeureDebut());
            choixFin.setValue(creneauExistant.getHeureFin());
            champSalle.setText(creneauExistant.getSalle());
            champDescription.setText(creneauExistant.getDescription());
        } else {
            coursInitial = emploiDuTemps.getCours().get(0);
            emploiDuTemps.fichiersVisibles(coursInitial).forEach(f -> fichiersCoches.add(f.getId()));
            choixDebut.setValue(heureDebutParDefaut);
            choixFin.setValue(heureFinParDefaut);
        }
        choixCours.setValue(coursInitial);

        champSalle.setPrefWidth(120);

        ObservableList<Fichier> tousLesFichiersDialogue = FXCollections.observableArrayList();
        FilteredList<Fichier> fichiersFiltresDialogue = new FilteredList<>(tousLesFichiersDialogue);

        ListView<Fichier> listeFichiers = new ListView<>();
        listeFichiers.setPrefHeight(160);
        listeFichiers.setCellFactory(vue -> new ListCell<>() {
            private final CheckBox caseCoche = new CheckBox();
            private final Label libelleFichier = new Label();
            private final HBox conteneurTags = new HBox(4);
            private final HBox ligneCellule = new HBox(8, caseCoche, libelleFichier, conteneurTags);

            {
                conteneurTags.setAlignment(Pos.CENTER_LEFT);
                ligneCellule.setAlignment(Pos.CENTER_LEFT);
                caseCoche.setOnAction(e -> {
                    Fichier fichier = getItem();
                    if (fichier == null) {
                        return;
                    }
                    if (caseCoche.isSelected()) {
                        fichiersCoches.add(fichier.getId());
                    } else {
                        fichiersCoches.remove(fichier.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Fichier fichier, boolean vide) {
                super.updateItem(fichier, vide);
                if (vide || fichier == null) {
                    setGraphic(null);
                    return;
                }
                libelleFichier.setText(fichier.toString());
                caseCoche.setSelected(fichiersCoches.contains(fichier.getId()));
                conteneurTags.getChildren().clear();
                for (String tag : fichier.getTags()) {
                    conteneurTags.getChildren().add(
                            TagPills.pastille(tag, emploiDuTemps.getParametres().couleurTag(tag)));
                }
                setGraphic(ligneCellule);
            }
        });
        listeFichiers.setItems(fichiersFiltresDialogue);
        tousLesFichiersDialogue.setAll(emploiDuTemps.fichiersVisibles(coursInitial));

        TextField rechercheFichiers = new TextField();
        rechercheFichiers.setPromptText("Rechercher un fichier...");
        HBox.setHgrow(rechercheFichiers, Priority.ALWAYS);

        ComboBox<String> filtreTag = new ComboBox<>();
        filtreTag.getItems().add("Tous les tags");
        filtreTag.getItems().addAll(emploiDuTemps.getParametres().getTagsDisponibles());
        filtreTag.setValue("Tous les tags");

        Runnable actualiserFiltreFichiers = () -> fichiersFiltresDialogue.setPredicate(fichier -> {
            String recherche = rechercheFichiers.getText();
            boolean correspondRecherche = recherche == null || recherche.isBlank()
                    || fichier.toString().toLowerCase().contains(recherche.toLowerCase());
            String tagChoisi = filtreTag.getValue();
            boolean correspondTag = tagChoisi == null || tagChoisi.equals("Tous les tags")
                    || fichier.getTags().contains(tagChoisi);
            return correspondRecherche && correspondTag;
        });
        rechercheFichiers.textProperty().addListener((obs, ancien, nouveau) -> actualiserFiltreFichiers.run());
        filtreTag.setOnAction(e -> actualiserFiltreFichiers.run());
        actualiserFiltreFichiers.run();

        HBox ligneFiltresFichiers = new HBox(8, rechercheFichiers, filtreTag);

        choixCours.setOnAction(e -> {
            Cours coursChoisi = choixCours.getValue();
            fichiersCoches.clear();
            if (coursChoisi != null) {
                emploiDuTemps.fichiersVisibles(coursChoisi).forEach(f -> fichiersCoches.add(f.getId()));
            }
            tousLesFichiersDialogue.setAll(coursChoisi != null ? emploiDuTemps.fichiersVisibles(coursChoisi) : List.of());
        });

        Button boutonToutCocher = new Button("Tout cocher");
        boutonToutCocher.setOnAction(e -> {
            fichiersCoches.clear();
            listeFichiers.getItems().forEach(f -> fichiersCoches.add(f.getId()));
            listeFichiers.refresh();
        });
        Button boutonToutDecocher = new Button("Tout décocher");
        boutonToutDecocher.setOnAction(e -> {
            fichiersCoches.clear();
            listeFichiers.refresh();
        });
        HBox boutonsFichiers = new HBox(8, boutonToutCocher, boutonToutDecocher);

        HBox ligneHoraire = new HBox(8, new Label("De"), choixDebut, new Label("À"), choixFin);
        ligneHoraire.setAlignment(Pos.CENTER_LEFT);

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(8);
        formulaire.addRow(0, new Label("Cours"), choixCours);
        formulaire.add(ligneHoraire, 0, 1, 2, 1);
        formulaire.addRow(2, new Label("Semaine"), choixSemaine);
        formulaire.addRow(3, new Label("Salle"), champSalle);
        formulaire.addRow(4, new Label("Description"), champDescription);
        formulaire.add(new Label("Fichiers à utiliser pour cette séance"), 0, 5, 2, 1);
        formulaire.add(ligneFiltresFichiers, 0, 6, 2, 1);
        formulaire.add(listeFichiers, 0, 7, 2, 1);
        formulaire.add(boutonsFichiers, 0, 8, 2, 1);

        ButtonType boutonValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonDupliquer = new ButtonType("Dupliquer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonOuvrir = new ButtonType("Ouvrir maintenant", ButtonBar.ButtonData.APPLY);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle(creneauExistant == null ? "Nouveau créneau" : "Modifier le créneau");
        dialogue.setHeaderText(NomsJours.nom(jour));
        dialogue.setResizable(true);
        dialogue.getDialogPane().setContent(formulaire);
        dialogue.getDialogPane().setPrefWidth(480);
        if (creneauExistant != null) {
            dialogue.getDialogPane().getButtonTypes().addAll(boutonSupprimer, boutonDupliquer);
        }
        dialogue.getDialogPane().getButtonTypes().addAll(boutonOuvrir, boutonValider, ButtonType.CANCEL);

        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() == ButtonType.CANCEL) {
            return;
        }

        if (resultat.get() == boutonSupprimer) {
            enregistrerAvantModification();
            emploiDuTemps.supprimerCreneau(creneauExistant.getId());
            notifierChangement();
            rafraichir();
            return;
        }

        Cours coursChoisi = choixCours.getValue();
        LocalTime debut = choixDebut.getValue();
        LocalTime fin = choixFin.getValue();
        if (coursChoisi == null || debut == null || fin == null || !fin.isAfter(debut)) {
            Alert erreur = new Alert(Alert.AlertType.ERROR,
                    "Choisis un cours et une plage horaire valide (fin après le début).");
            erreur.setTitle("Créneau invalide");
            erreur.setHeaderText(null);
            erreur.showAndWait();
            return;
        }
        enregistrerAvantModification();

        List<UUID> idsSelectionnes = emploiDuTemps.fichiersVisibles(coursChoisi).stream()
                .map(Fichier::getId)
                .filter(fichiersCoches::contains)
                .collect(Collectors.toList());

        Creneau creneau;
        if (creneauExistant == null || resultat.get() == boutonDupliquer) {
            creneau = emploiDuTemps.ajouterCreneau(jour, debut, fin, coursChoisi.getId());
        } else {
            creneau = creneauExistant;
            creneau.setJour(jour);
            creneau.setHeureDebut(debut);
            creneau.setHeureFin(fin);
            creneau.setCoursId(coursChoisi.getId());
        }
        creneau.setFichiersSelectionnesIds(idsSelectionnes);
        creneau.setSalle(videVersNull(champSalle.getText()));
        creneau.setDescription(videVersNull(champDescription.getText()));
        creneau.setTypeSemaine(choixSemaine.getValue());

        notifierChangement();
        rafraichir();

        if (resultat.get() == boutonOuvrir) {
            List<Fichier> fichiersAOuvrir = emploiDuTemps.fichiersPourCreneau(creneau);
            ouvrirFichiers(fichiersAOuvrir);
        }
    }

    private void ouvrirFichiers(List<Fichier> fichiers) {
        if (fichiers.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION, "Aucun fichier sélectionné pour ce créneau.");
            alerte.setTitle("Rien à ouvrir");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }
        List<String> echecs = OuvreurFichiers.ouvrir(fichiers);
        if (!echecs.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.WARNING,
                    "Certains fichiers n'ont pas pu être ouverts :\n" + String.join("\n", echecs));
            alerte.setTitle("Ouverture partielle");
            alerte.setHeaderText(null);
            alerte.showAndWait();
        }
    }

    private List<LocalTime> genererLimites() {
        List<LocalTime> limites = new ArrayList<>();
        LocalTime t = heureDebutGrille;
        while (!t.isAfter(heureFinGrille)) {
            limites.add(t);
            t = t.plusMinutes(pasMinutes);
        }
        return limites;
    }

    /**
     * @return {@code options} augmentée de {@code valeur} si elle n'y figure pas déjà (triée),
     * pour garantir qu'un horaire de bloc non aligné sur {@link #pasMinutes} reste sélectionnable.
     */
    private static List<LocalTime> avecValeur(List<LocalTime> options, LocalTime valeur) {
        if (valeur == null || options.contains(valeur)) {
            return options;
        }
        List<LocalTime> resultat = new ArrayList<>(options);
        resultat.add(valeur);
        resultat.sort(LocalTime::compareTo);
        return resultat;
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
    }

    private static int toMinutes(LocalTime heure) {
        return heure.getHour() * 60 + heure.getMinute();
    }

    private static LocalTime minutesVersHeure(int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    private static String videVersNull(String texte) {
        return texte == null || texte.isBlank() ? null : texte;
    }

    private static int clamp(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }
}
