package com.teacherflow.ui;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Grille hebdomadaire (jours × heures) permettant d'assigner un {@link Cours} à chaque
 * {@link Creneau}, puis de le déplacer ou de le redimensionner directement à la souris
 * (par pas de {@value PAS_SNAP_MINUTES} minutes), ou de le modifier/supprimer via une
 * boîte de dialogue.
 */
public class EmploiDuTempsPane extends BorderPane {

    private static final LocalTime HEURE_DEBUT_GRILLE = LocalTime.of(7, 0);
    private static final LocalTime HEURE_FIN_GRILLE = LocalTime.of(20, 0);
    private static final int PAS_SNAP_MINUTES = 10;
    private static final int PAS_AFFICHAGE_MINUTES = 30;
    private static final int DUREE_MIN_MINUTES = 10;
    private static final double PIXELS_PAR_MINUTE = 1.5;
    private static final double LARGEUR_COLONNE_HEURES = 60;
    private static final double LARGEUR_COLONNE_JOUR_MIN = 110;
    private static final double LARGEUR_COLONNE_JOUR_DEFAUT = 140;
    private static final double HAUTEUR_POIGNEE = 6;
    private static final double SEUIL_CLIC_PIXELS = 4;
    private static final double MARGE_VERTICALE = 10;

    private static final DayOfWeek[] JOURS_AFFICHES = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    private enum ModeInteraction { DEPLACER, REDIMENSIONNER_HAUT, REDIMENSIONNER_BAS }

    private final EmploiDuTemps emploiDuTemps;
    private final Runnable surChangement;
    private final HBox ligneEntetes = new HBox();
    private final HBox ligneColonnes = new HBox();
    private final ScrollPane defilement = new ScrollPane();
    private double largeurColonneJour = LARGEUR_COLONNE_JOUR_DEFAUT;

    private final VBox panneauCreneau = new VBox(10);
    private final Label titrePanneau = new Label();
    private final ComboBox<Cours> choixCoursPanneau = new ComboBox<>();
    private final ComboBox<LocalTime> choixDebutPanneau = new ComboBox<>();
    private final ComboBox<LocalTime> choixFinPanneau = new ComboBox<>();
    private final ListView<Fichier> listeFichiersPanneau = new ListView<>();
    private final Set<UUID> fichiersCochesPanneau = new LinkedHashSet<>();
    private final Button boutonSupprimerPanneau = new Button("Supprimer");
    private Creneau creneauEnEdition;
    private DayOfWeek jourEnEdition;

    public EmploiDuTempsPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;

        setPadding(new Insets(16));

        defilement.setContent(ligneColonnes);
        defilement.setFitToWidth(false);
        defilement.viewportBoundsProperty().addListener((obs, ancien, nouveau) -> rafraichir());

        VBox.setVgrow(defilement, Priority.ALWAYS);
        setCenter(new VBox(ligneEntetes, defilement));

        construirePanneauCreneau();
        rafraichir();
    }

    private void construirePanneauCreneau() {
        Button boutonFermer = new Button();
        boutonFermer.setGraphic(Icons.fermer());
        boutonFermer.getStyleClass().add("bouton-icone");
        boutonFermer.setOnAction(e -> fermerPanneauCreneau());

        titrePanneau.getStyleClass().add("titre-section");
        Region espaceurTitre = new Region();
        HBox.setHgrow(espaceurTitre, Priority.ALWAYS);
        HBox ligneTitre = new HBox(titrePanneau, espaceurTitre, boutonFermer);
        ligneTitre.setAlignment(Pos.CENTER_LEFT);

        choixCoursPanneau.setMaxWidth(Double.MAX_VALUE);
        choixDebutPanneau.setMaxWidth(Double.MAX_VALUE);
        choixFinPanneau.setMaxWidth(Double.MAX_VALUE);

        listeFichiersPanneau.setPrefHeight(180);
        listeFichiersPanneau.setCellFactory(CheckBoxListCell.forListView(fichier -> {
            SimpleBooleanProperty propriete = new SimpleBooleanProperty(fichiersCochesPanneau.contains(fichier.getId()));
            propriete.addListener((obs, etaitCoche, estCoche) -> {
                if (estCoche) {
                    fichiersCochesPanneau.add(fichier.getId());
                } else {
                    fichiersCochesPanneau.remove(fichier.getId());
                }
            });
            return propriete;
        }));

        choixCoursPanneau.setOnAction(e -> {
            Cours coursChoisi = choixCoursPanneau.getValue();
            fichiersCochesPanneau.clear();
            if (coursChoisi != null) {
                coursChoisi.getFichiers().forEach(f -> fichiersCochesPanneau.add(f.getId()));
            }
            listeFichiersPanneau.getItems().setAll(coursChoisi != null ? coursChoisi.getFichiers() : List.of());
        });

        Button boutonToutCocher = new Button("Tout cocher");
        boutonToutCocher.setOnAction(e -> {
            fichiersCochesPanneau.clear();
            listeFichiersPanneau.getItems().forEach(f -> fichiersCochesPanneau.add(f.getId()));
            listeFichiersPanneau.refresh();
        });
        Button boutonToutDecocher = new Button("Tout décocher");
        boutonToutDecocher.setOnAction(e -> {
            fichiersCochesPanneau.clear();
            listeFichiersPanneau.refresh();
        });
        HBox boutonsCocher = new HBox(8, boutonToutCocher, boutonToutDecocher);

        Button boutonOuvrir = new Button("Ouvrir maintenant");
        boutonOuvrir.getStyleClass().add("bouton-secondaire");
        boutonOuvrir.setMaxWidth(Double.MAX_VALUE);
        boutonOuvrir.setOnAction(e -> ouvrirFichiersDepuisPanneau());

        Button boutonValider = new Button("Valider");
        boutonValider.getStyleClass().add("bouton-primaire");
        boutonValider.setMaxWidth(Double.MAX_VALUE);
        boutonValider.setOnAction(e -> validerPanneauCreneau());

        boutonSupprimerPanneau.getStyleClass().add("bouton-danger");
        boutonSupprimerPanneau.setMaxWidth(Double.MAX_VALUE);
        boutonSupprimerPanneau.setOnAction(e -> supprimerPanneauCreneau());

        panneauCreneau.getChildren().addAll(
                ligneTitre,
                titreChamp("Cours"), choixCoursPanneau,
                titreChamp("De"), choixDebutPanneau,
                titreChamp("À"), choixFinPanneau,
                titreChamp("Fichiers à utiliser pour cette séance"),
                listeFichiersPanneau, boutonsCocher,
                boutonOuvrir, boutonValider, boutonSupprimerPanneau);
        panneauCreneau.setPadding(new Insets(16));
        panneauCreneau.setPrefWidth(300);
        panneauCreneau.getStyleClass().add("panneau-lateral");
    }

    private static Label titreChamp(String texte) {
        Label label = new Label(texte);
        label.getStyleClass().add("titre-section");
        return label;
    }

    private void mettreAJourEntetes() {
        ligneEntetes.getChildren().clear();
        ligneEntetes.getChildren().add(espace(LARGEUR_COLONNE_HEURES));
        for (DayOfWeek jour : JOURS_AFFICHES) {
            Label enTete = new Label(nomJour(jour));
            enTete.setPrefWidth(largeurColonneJour);
            enTete.setAlignment(Pos.CENTER);
            enTete.getStyleClass().add("titre-jour");
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
        return Math.max(LARGEUR_COLONNE_JOUR_MIN, largeurJours / JOURS_AFFICHES.length);
    }

    private Pane construireColonneHeures(double hauteur) {
        Pane pane = new Pane();
        pane.setPrefSize(LARGEUR_COLONNE_HEURES, hauteur);
        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            LocalTime heure = HEURE_DEBUT_GRILLE.plusMinutes(minute);
            if (heure.getMinute() == 0) {
                Label label = new Label(heure.toString());
                label.getStyleClass().add("grille-heure-label");
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
        double largeurTotale = JOURS_AFFICHES.length * largeurColonneJour;
        Pane pane = new Pane();
        pane.setPrefSize(largeurTotale, hauteur);
        pane.getStyleClass().add("grille-fond");

        int indexAujourdhui = indexDuJour(LocalDate.now().getDayOfWeek());
        if (indexAujourdhui >= 0) {
            Region surbrillance = new Region();
            surbrillance.setPrefSize(largeurColonneJour, hauteur);
            surbrillance.setLayoutX(indexAujourdhui * largeurColonneJour);
            surbrillance.getStyleClass().add("grille-aujourdhui");
            surbrillance.setMouseTransparent(true);
            pane.getChildren().add(surbrillance);
        }

        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            Region ligne = new Region();
            ligne.setPrefSize(largeurTotale, 1);
            ligne.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE);
            ligne.getStyleClass().add("grille-ligne");
            ligne.setMouseTransparent(true);
            pane.getChildren().add(ligne);
        }
        for (int i = 1; i < JOURS_AFFICHES.length; i++) {
            Region separateur = new Region();
            separateur.setPrefSize(1, hauteur);
            separateur.setLayoutX(i * largeurColonneJour);
            separateur.getStyleClass().add("grille-separateur");
            separateur.setMouseTransparent(true);
            pane.getChildren().add(separateur);
        }

        pane.setOnMouseClicked(e -> ouvrirPanneauCreneau(null, jourDepuisX(e.getX()), heureDepuisY(e.getY())));

        emploiDuTemps.getCreneaux().stream()
                .filter(c -> indexDuJour(c.getJour()) >= 0)
                .forEach(c -> pane.getChildren().add(new BlocCreneau(c)));

        return pane;
    }

    private LocalTime heureDepuisY(double y) {
        long pas = Math.round(((y - MARGE_VERTICALE) / PIXELS_PAR_MINUTE) / PAS_SNAP_MINUTES);
        int minutes = clamp((int) (pas * PAS_SNAP_MINUTES), 0, minutesGrille() - DUREE_MIN_MINUTES);
        return HEURE_DEBUT_GRILLE.plusMinutes(minutes);
    }

    private DayOfWeek jourDepuisX(double x) {
        int index = clamp((int) (x / largeurColonneJour), 0, JOURS_AFFICHES.length - 1);
        return JOURS_AFFICHES[index];
    }

    private int minutesGrille() {
        return (int) Duration.between(HEURE_DEBUT_GRILLE, HEURE_FIN_GRILLE).toMinutes();
    }

    private static int indexDuJour(DayOfWeek jour) {
        for (int i = 0; i < JOURS_AFFICHES.length; i++) {
            if (JOURS_AFFICHES[i] == jour) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Un créneau affiché dans la grille : déplaçable (glisser le corps) et redimensionnable
     * (glisser le bord haut ou bas), par pas de {@link #PAS_SNAP_MINUTES} minutes. Un clic
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
            libelle.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;");
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
            return "-fx-background-color: " + couleur + "; -fx-background-radius: 5;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 3, 0, 0, 1);";
        }

        private void actualiser(int dayIndex, int debutMinutes, int finMinutes) {
            dayIndexCourant = dayIndex;
            setLayoutX(dayIndex * largeurColonneJour + 2);
            setLayoutY(MARGE_VERTICALE + (debutMinutes - toMinutes(HEURE_DEBUT_GRILLE)) * PIXELS_PAR_MINUTE);
            setPrefHeight(Math.max(DUREE_MIN_MINUTES, finMinutes - debutMinutes) * PIXELS_PAR_MINUTE);
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";
            libelle.setText(nomCours + "\n" + minutesVersHeure(debutMinutes) + " - " + minutesVersHeure(finMinutes));
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

            long pas = Math.round((deltaY / PIXELS_PAR_MINUTE) / PAS_SNAP_MINUTES);
            int deltaMinutes = (int) (pas * PAS_SNAP_MINUTES);
            int minGrille = toMinutes(HEURE_DEBUT_GRILLE);
            int maxGrille = toMinutes(HEURE_FIN_GRILLE);

            int nouveauDayIndex = dayIndexInitial;
            int nouveauDebut = minutesDebutInitial;
            int nouveauFin = minutesFinInitial;
            switch (mode) {
                case DEPLACER -> {
                    int duree = minutesFinInitial - minutesDebutInitial;
                    nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, maxGrille - duree);
                    nouveauFin = nouveauDebut + duree;
                    int deltaJours = (int) Math.round(deltaX / largeurColonneJour);
                    nouveauDayIndex = clamp(dayIndexInitial + deltaJours, 0, JOURS_AFFICHES.length - 1);
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
                ouvrirPanneauCreneau(creneau, creneau.getJour(), creneau.getHeureDebut());
                e.consume();
                return;
            }

            int debutFinal = toMinutes(HEURE_DEBUT_GRILLE) + (int) Math.round((getLayoutY() - MARGE_VERTICALE) / PIXELS_PAR_MINUTE);
            int finFinal = debutFinal + (int) Math.round(getPrefHeight() / PIXELS_PAR_MINUTE);
            creneau.setJour(JOURS_AFFICHES[dayIndexCourant]);
            creneau.setHeureDebut(minutesVersHeure(debutFinal));
            creneau.setHeureFin(minutesVersHeure(finFinal));
            notifierChangement();
            e.consume();
        }
    }

    private void ouvrirPanneauCreneau(Creneau creneauExistant, DayOfWeek jour, LocalTime heureDebutParDefaut) {
        if (emploiDuTemps.getCours().isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION,
                    "Crée d'abord un cours dans l'onglet \"Cours\" avant de remplir l'emploi du temps.");
            alerte.setTitle("Aucun cours disponible");
            alerte.setHeaderText(null);
            Styles.appliquer(alerte);
            alerte.showAndWait();
            return;
        }

        creneauEnEdition = creneauExistant;
        jourEnEdition = jour;
        titrePanneau.setText((creneauExistant == null ? "Nouveau créneau — " : "Modifier — ") + nomJour(jour));
        boutonSupprimerPanneau.setVisible(creneauExistant != null);
        boutonSupprimerPanneau.setManaged(creneauExistant != null);

        List<LocalTime> limites = genererLimites();
        List<LocalTime> optionsDebut = limites.subList(0, limites.size() - 1);
        List<LocalTime> optionsFin = limites.subList(1, limites.size());
        choixDebutPanneau.getItems().setAll(optionsDebut);
        choixFinPanneau.getItems().setAll(optionsFin);
        choixCoursPanneau.getItems().setAll(emploiDuTemps.getCours());

        Cours coursInitial;
        fichiersCochesPanneau.clear();
        if (creneauExistant != null) {
            coursInitial = emploiDuTemps.trouverCours(creneauExistant.getCoursId())
                    .orElse(emploiDuTemps.getCours().get(0));
            fichiersCochesPanneau.addAll(creneauExistant.getFichiersSelectionnesIds());
            choixDebutPanneau.setValue(creneauExistant.getHeureDebut());
            choixFinPanneau.setValue(creneauExistant.getHeureFin());
        } else {
            coursInitial = emploiDuTemps.getCours().get(0);
            coursInitial.getFichiers().forEach(f -> fichiersCochesPanneau.add(f.getId()));
            choixDebutPanneau.setValue(heureDebutParDefaut);
            LocalTime finParDefaut = heureDebutParDefaut.plusHours(1);
            choixFinPanneau.setValue(optionsFin.contains(finParDefaut)
                    ? finParDefaut : optionsFin.get(optionsFin.size() - 1));
        }
        choixCoursPanneau.setValue(coursInitial);
        listeFichiersPanneau.getItems().setAll(coursInitial.getFichiers());
        listeFichiersPanneau.refresh();

        setRight(panneauCreneau);
    }

    private void fermerPanneauCreneau() {
        setRight(null);
        creneauEnEdition = null;
    }

    /**
     * Valide le formulaire du panneau et enregistre le créneau (création ou mise à jour).
     * @return le créneau enregistré, ou {@code null} si le formulaire est invalide.
     */
    private Creneau enregistrerPanneau() {
        Cours coursChoisi = choixCoursPanneau.getValue();
        LocalTime debut = choixDebutPanneau.getValue();
        LocalTime fin = choixFinPanneau.getValue();
        if (coursChoisi == null || debut == null || fin == null || !fin.isAfter(debut)) {
            Alert erreur = new Alert(Alert.AlertType.ERROR,
                    "Choisis un cours et une plage horaire valide (fin après le début).");
            erreur.setTitle("Créneau invalide");
            erreur.setHeaderText(null);
            Styles.appliquer(erreur);
            erreur.showAndWait();
            return null;
        }

        List<UUID> idsSelectionnes = coursChoisi.getFichiers().stream()
                .map(Fichier::getId)
                .filter(fichiersCochesPanneau::contains)
                .collect(Collectors.toList());

        Creneau creneau;
        if (creneauEnEdition == null) {
            creneau = emploiDuTemps.ajouterCreneau(jourEnEdition, debut, fin, coursChoisi.getId());
        } else {
            creneau = creneauEnEdition;
            creneau.setJour(jourEnEdition);
            creneau.setHeureDebut(debut);
            creneau.setHeureFin(fin);
            creneau.setCoursId(coursChoisi.getId());
        }
        creneau.setFichiersSelectionnesIds(idsSelectionnes);
        notifierChangement();
        return creneau;
    }

    private void validerPanneauCreneau() {
        if (enregistrerPanneau() == null) {
            return;
        }
        fermerPanneauCreneau();
        rafraichir();
    }

    private void supprimerPanneauCreneau() {
        if (creneauEnEdition == null) {
            return;
        }
        emploiDuTemps.supprimerCreneau(creneauEnEdition.getId());
        notifierChangement();
        fermerPanneauCreneau();
        rafraichir();
    }

    private void ouvrirFichiersDepuisPanneau() {
        Creneau creneau = enregistrerPanneau();
        if (creneau == null) {
            return;
        }
        List<Fichier> fichiersAOuvrir = emploiDuTemps.fichiersPourCreneau(creneau);
        fermerPanneauCreneau();
        rafraichir();
        ouvrirFichiers(fichiersAOuvrir);
    }

    /**
     * Ouvre les fichiers avec l'application associée du système, en passant par une commande
     * native ({@code xdg-open}/{@code open}/{@code start}) plutôt que {@code java.awt.Desktop} :
     * initialiser AWT dans une appli JavaFX sur Linux charge un toolkit GTK concurrent de celui
     * de JavaFX et fait planter la JVM.
     */
    private void ouvrirFichiers(List<Fichier> fichiers) {
        if (fichiers.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION, "Aucun fichier sélectionné pour ce créneau.");
            alerte.setTitle("Rien à ouvrir");
            alerte.setHeaderText(null);
            Styles.appliquer(alerte);
            alerte.showAndWait();
            return;
        }
        List<String> echecs = new ArrayList<>();
        for (Fichier fichier : fichiers) {
            try {
                new ProcessBuilder(commandeOuverture(fichier.getChemin())).start();
            } catch (IOException e) {
                echecs.add(fichier.getNomAffichage() != null ? fichier.getNomAffichage() : fichier.getChemin());
            }
        }
        if (!echecs.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.WARNING,
                    "Certains fichiers n'ont pas pu être ouverts :\n" + String.join("\n", echecs));
            alerte.setTitle("Ouverture partielle");
            alerte.setHeaderText(null);
            Styles.appliquer(alerte);
            alerte.showAndWait();
        }
    }

    private static List<String> commandeOuverture(String chemin) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return List.of("cmd", "/c", "start", "\"\"", chemin);
        }
        if (os.contains("mac")) {
            return List.of("open", chemin);
        }
        return List.of("xdg-open", chemin);
    }

    private List<LocalTime> genererLimites() {
        List<LocalTime> limites = new ArrayList<>();
        LocalTime t = HEURE_DEBUT_GRILLE;
        while (!t.isAfter(HEURE_FIN_GRILLE)) {
            limites.add(t);
            t = t.plusMinutes(PAS_SNAP_MINUTES);
        }
        return limites;
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

    private static int clamp(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }

    private static String nomJour(DayOfWeek jour) {
        return switch (jour) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
            case SATURDAY -> "Samedi";
            case SUNDAY -> "Dimanche";
        };
    }
}
