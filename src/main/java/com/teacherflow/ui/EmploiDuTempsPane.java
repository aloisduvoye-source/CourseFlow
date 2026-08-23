package com.teacherflow.ui;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.Parametres;
import com.teacherflow.model.PlageHoraire;
import com.teacherflow.util.NomsJours;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
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

    public EmploiDuTempsPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;

        setPadding(new Insets(16));
        setFocusTraversable(true);
        setOnKeyPressed(this::gererRaccourciClavier);

        defilement.setContent(ligneColonnes);
        defilement.setFitToWidth(false);
        defilement.viewportBoundsProperty().addListener((obs, ancien, nouveau) -> rafraichir());

        VBox.setVgrow(defilement, Priority.ALWAYS);
        setCenter(new VBox(ligneEntetes, defilement));

        rafraichir();
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

    /**
     * Capture un instantané profond des créneaux avant une mutation, pour pouvoir l'annuler
     * (Ctrl+Z). Toute nouvelle action après un "annuler" invalide la pile "rétablir".
     */
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
        return copie;
    }

    private void mettreAJourEntetes() {
        ligneEntetes.getChildren().clear();
        ligneEntetes.getChildren().add(espace(LARGEUR_COLONNE_HEURES));
        for (DayOfWeek jour : joursAffiches) {
            Label enTete = new Label(NomsJours.nom(jour));
            enTete.setPrefWidth(largeurColonneJour);
            enTete.setAlignment(Pos.CENTER);
            enTete.setStyle("-fx-font-weight: bold;");
            ligneEntetes.getChildren().add(enTete);
        }
    }

    private static Region espace(double largeur) {
        Region region = new Region();
        region.setPrefWidth(largeur);
        return region;
    }

    /**
     * Reconstruit entièrement la grille à partir de l'état courant de l'{@link EmploiDuTemps},
     * en recalculant au passage la largeur des colonnes pour occuper toute la largeur
     * disponible du panneau (avec un minimum lisible, au-delà duquel un défilement horizontal
     * prend le relais).
     */
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

    /**
     * Zone de dessin unique regroupant les 6 jours affichés, plutôt qu'un panneau par jour :
     * cela permet à un {@link BlocCreneau} de se déplacer librement en X (jour) et pas
     * seulement en Y (heure) lors d'un glisser.
     */
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

        emploiDuTemps.getCreneaux().stream()
                .filter(c -> indexDuJour(c.getJour()) >= 0)
                .filter(c -> chevaucheGrille(c.getHeureDebut(), c.getHeureFin()))
                .forEach(c -> pane.getChildren().add(new BlocCreneau(c)));

        return pane;
    }

    /**
     * Ajoute une zone cliquable par jour pour chaque bloc horaire défini dans les paramètres
     * (ex. 9h-10h puis 10h20-11h20), plutôt qu'un unique clic-n'importe-où sur toute la
     * grille : cela limite les points de création possibles à un ensemble prévisible de
     * blocs, identique chaque jour. Le déplacement/redimensionnement d'un créneau existant
     * (glisser-déposer) n'est pas concerné et reste libre.
     */
    private void ajouterCellulesCliquables(Pane pane) {
        List<PlageHoraire> blocs = emploiDuTemps.getParametres().getBlocs();
        int debutGrille = toMinutes(heureDebutGrille);
        for (int jourIndex = 0; jourIndex < joursAffiches.length; jourIndex++) {
            DayOfWeek jour = joursAffiches[jourIndex];
            for (PlageHoraire bloc : blocs) {
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
                cellule.setStyle("-fx-background-color: -color-accent-subtle;");
                cellule.setOnMouseClicked(e -> ouvrirDialogueCreneau(null, jour, bloc.getDebut(), bloc.getFin()));
                pane.getChildren().add(cellule);
            }
        }
    }

    private int minutesGrille() {
        return (int) Duration.between(heureDebutGrille, heureFinGrille).toMinutes();
    }

    /**
     * @return true si [debut, fin) recouvre au moins partiellement la plage horaire actuelle
     * de la grille (utilisé pour ne pas afficher un créneau devenu totalement invisible après
     * un rétrécissement de cette plage dans les paramètres).
     */
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

    /**
     * Un créneau affiché dans la grille : déplaçable (glisser le corps) et redimensionnable
     * (glisser le bord haut ou bas), par pas de {@link #pasMinutes} minutes. Un clic
     * sans déplacement ouvre la boîte de dialogue d'édition.
     */
    private final class BlocCreneau extends StackPane {

        private final Creneau creneau;
        private final Label libelle = new Label();

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

            libelle.setWrapText(true);
            libelle.setMouseTransparent(true);
            libelle.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
            getChildren().add(libelle);
            setAlignment(Pos.TOP_LEFT);
            setPadding(new Insets(3, 2, 2, 4));
            setPrefWidth(largeurColonneJour - 4);
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
            return "-fx-background-color: " + couleur + ";";
        }

        private void actualiser(int dayIndex, int debutMinutes, int finMinutes) {
            dayIndexCourant = dayIndex;
            int grilleDebut = toMinutes(heureDebutGrille);
            int grilleFin = toMinutes(heureFinGrille);
            int debutAffiche = clamp(debutMinutes, grilleDebut, grilleFin);
            int finAffiche = clamp(finMinutes, grilleDebut, grilleFin);
            setLayoutX(dayIndex * largeurColonneJour + 2);
            setLayoutY(MARGE_VERTICALE + (debutAffiche - grilleDebut) * PIXELS_PAR_MINUTE);
            setPrefHeight(Math.max(DUREE_MIN_MINUTES, finAffiche - debutAffiche) * PIXELS_PAR_MINUTE);
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";

            StringBuilder texte = new StringBuilder(nomCours);
            if (creneau.getSalle() != null && !creneau.getSalle().isBlank()) {
                texte.append(" · ").append(creneau.getSalle());
            }
            texte.append('\n').append(minutesVersHeure(debutMinutes)).append(" - ").append(minutesVersHeure(finMinutes));
            if (creneau.getDescription() != null && !creneau.getDescription().isBlank()) {
                texte.append('\n').append(creneau.getDescription());
            }
            libelle.setText(texte.toString());
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

        ListView<Fichier> listeFichiers = new ListView<>();
        listeFichiers.setPrefHeight(140);
        listeFichiers.setCellFactory(CheckBoxListCell.forListView(fichier -> {
            SimpleBooleanProperty propriete = new SimpleBooleanProperty(fichiersCoches.contains(fichier.getId()));
            propriete.addListener((obs, etaitCoche, estCoche) -> {
                if (estCoche) {
                    fichiersCoches.add(fichier.getId());
                } else {
                    fichiersCoches.remove(fichier.getId());
                }
            });
            return propriete;
        }));
        listeFichiers.getItems().setAll(emploiDuTemps.fichiersVisibles(coursInitial));

        choixCours.setOnAction(e -> {
            Cours coursChoisi = choixCours.getValue();
            fichiersCoches.clear();
            if (coursChoisi != null) {
                emploiDuTemps.fichiersVisibles(coursChoisi).forEach(f -> fichiersCoches.add(f.getId()));
            }
            listeFichiers.getItems().setAll(coursChoisi != null ? emploiDuTemps.fichiersVisibles(coursChoisi) : List.of());
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

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(8);
        formulaire.addRow(0, new Label("Cours"), choixCours);
        formulaire.addRow(1, new Label("De"), choixDebut);
        formulaire.addRow(2, new Label("À"), choixFin);
        formulaire.addRow(3, new Label("Salle"), champSalle);
        formulaire.addRow(4, new Label("Description"), champDescription);
        formulaire.add(new Label("Fichiers à utiliser pour cette séance"), 0, 5, 2, 1);
        formulaire.add(listeFichiers, 0, 6, 2, 1);
        formulaire.add(boutonsFichiers, 0, 7, 2, 1);

        ButtonType boutonValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonDupliquer = new ButtonType("Dupliquer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonOuvrir = new ButtonType("Ouvrir maintenant", ButtonBar.ButtonData.APPLY);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle(creneauExistant == null ? "Nouveau créneau" : "Modifier le créneau");
        dialogue.setHeaderText(NomsJours.nom(jour));
        dialogue.getDialogPane().setContent(formulaire);
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
