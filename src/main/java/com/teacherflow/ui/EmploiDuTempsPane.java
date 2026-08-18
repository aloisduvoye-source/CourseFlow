package com.teacherflow.ui;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Grille hebdomadaire (jours × heures) permettant d'assigner un {@link Cours} à chaque
 * {@link Creneau}, de les modifier ou de les supprimer.
 */
public class EmploiDuTempsPane extends BorderPane {

    private static final LocalTime HEURE_DEBUT_GRILLE = LocalTime.of(8, 0);
    private static final LocalTime HEURE_FIN_GRILLE = LocalTime.of(19, 0);
    private static final int PAS_MINUTES = 30;
    private static final double HAUTEUR_LIGNE = 26;
    private static final DayOfWeek[] JOURS_AFFICHES = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    };

    private final EmploiDuTemps emploiDuTemps;
    private final Runnable surChangement;
    private final GridPane grille = new GridPane();

    public EmploiDuTempsPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;

        setPadding(new Insets(12));
        ScrollPane defilement = new ScrollPane(grille);
        defilement.setFitToWidth(true);
        setCenter(defilement);

        rafraichir();
    }

    /**
     * Reconstruit entièrement la grille à partir de l'état courant de l'{@link EmploiDuTemps}.
     * À appeler après toute modification faite ailleurs (ex. suppression d'un cours).
     */
    public void rafraichir() {
        grille.getChildren().clear();
        grille.getColumnConstraints().clear();
        grille.getRowConstraints().clear();

        List<LocalTime> limites = genererLimites();
        int nbLignes = limites.size() - 1;

        ColumnConstraints colonneHeures = new ColumnConstraints(60);
        grille.getColumnConstraints().add(colonneHeures);
        for (int i = 0; i < JOURS_AFFICHES.length; i++) {
            ColumnConstraints colonne = new ColumnConstraints();
            colonne.setPercentWidth(100.0 / JOURS_AFFICHES.length);
            colonne.setHgrow(Priority.ALWAYS);
            grille.getColumnConstraints().add(colonne);
        }

        grille.getRowConstraints().add(new RowConstraints(30));
        for (int r = 0; r < nbLignes; r++) {
            grille.getRowConstraints().add(new RowConstraints(HAUTEUR_LIGNE));
        }

        grille.add(new Label(""), 0, 0);
        for (int i = 0; i < JOURS_AFFICHES.length; i++) {
            Label enTete = new Label(nomJour(JOURS_AFFICHES[i]));
            enTete.setStyle("-fx-font-weight: bold;");
            enTete.setMaxWidth(Double.MAX_VALUE);
            enTete.setAlignment(Pos.CENTER);
            grille.add(enTete, i + 1, 0);
        }

        for (int r = 0; r < nbLignes; r++) {
            LocalTime debutLigne = limites.get(r);
            if (debutLigne.getMinute() == 0) {
                Label labelHeure = new Label(debutLigne.toString());
                labelHeure.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
                grille.add(labelHeure, 0, r + 1);
            }
        }

        for (int jourIndex = 0; jourIndex < JOURS_AFFICHES.length; jourIndex++) {
            construireColonneJour(JOURS_AFFICHES[jourIndex], jourIndex, nbLignes, limites);
        }
    }

    private void construireColonneJour(DayOfWeek jour, int jourIndex, int nbLignes, List<LocalTime> limites) {
        boolean[] occupe = new boolean[nbLignes];

        List<Creneau> creneauxDuJour = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == jour)
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .toList();

        for (Creneau creneau : creneauxDuJour) {
            int ligneDebut = Math.max(0, indexLigne(creneau.getHeureDebut()));
            int ligneFin = Math.min(nbLignes, indexLigne(creneau.getHeureFin()));
            if (ligneDebut >= nbLignes || ligneFin <= ligneDebut) {
                continue;
            }
            int span = ligneFin - ligneDebut;
            for (int r = ligneDebut; r < ligneFin; r++) {
                occupe[r] = true;
            }

            Node bloc = construireBlocCreneau(creneau);
            grille.add(bloc, jourIndex + 1, ligneDebut + 1, 1, span);
        }

        for (int r = 0; r < nbLignes; r++) {
            if (!occupe[r]) {
                LocalTime heureDebut = limites.get(r);
                grille.add(construireCelluleVide(jour, heureDebut), jourIndex + 1, r + 1);
            }
        }
    }

    private Node construireBlocCreneau(Creneau creneau) {
        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";
        String couleur = cours != null && cours.getCouleur() != null ? cours.getCouleur() : "#95a5a6";

        Label libelle = new Label(nomCours + "\n" + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
        libelle.setWrapText(true);
        libelle.setStyle("-fx-text-fill: white; -fx-font-size: 11;");

        StackPane bloc = new StackPane(libelle);
        bloc.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 4;");
        bloc.setPadding(new Insets(2, 4, 2, 4));
        bloc.setMaxWidth(Double.MAX_VALUE);
        bloc.setMaxHeight(Double.MAX_VALUE);
        bloc.setCursor(Cursor.HAND);
        bloc.setOnMouseClicked(e -> ouvrirDialogueCreneau(creneau, creneau.getJour(), creneau.getHeureDebut()));
        return bloc;
    }

    private Node construireCelluleVide(DayOfWeek jour, LocalTime heureDebut) {
        Button cellule = new Button();
        cellule.setMaxWidth(Double.MAX_VALUE);
        cellule.setMaxHeight(Double.MAX_VALUE);
        cellule.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 1;");
        cellule.setOnAction(e -> ouvrirDialogueCreneau(null, jour, heureDebut));
        return cellule;
    }

    private void ouvrirDialogueCreneau(Creneau creneauExistant, DayOfWeek jour, LocalTime heureDebutParDefaut) {
        if (emploiDuTemps.getCours().isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION,
                    "Crée d'abord un cours dans l'onglet \"Cours\" avant de remplir l'emploi du temps.");
            alerte.setTitle("Aucun cours disponible");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }

        List<LocalTime> limites = genererLimites();
        List<LocalTime> optionsDebut = limites.subList(0, limites.size() - 1);
        List<LocalTime> optionsFin = limites.subList(1, limites.size());

        ComboBox<Cours> choixCours = new ComboBox<>();
        choixCours.getItems().addAll(emploiDuTemps.getCours());
        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(optionsDebut);
        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(optionsFin);

        if (creneauExistant != null) {
            emploiDuTemps.trouverCours(creneauExistant.getCoursId()).ifPresent(choixCours::setValue);
            choixDebut.setValue(creneauExistant.getHeureDebut());
            choixFin.setValue(creneauExistant.getHeureFin());
        } else {
            choixCours.setValue(emploiDuTemps.getCours().get(0));
            choixDebut.setValue(heureDebutParDefaut);
            LocalTime finParDefaut = heureDebutParDefaut.plusHours(1);
            choixFin.setValue(optionsFin.contains(finParDefaut)
                    ? finParDefaut : optionsFin.get(optionsFin.size() - 1));
        }

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(8);
        formulaire.addRow(0, new Label("Cours"), choixCours);
        formulaire.addRow(1, new Label("De"), choixDebut);
        formulaire.addRow(2, new Label("À"), choixFin);

        ButtonType boutonValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle(creneauExistant == null ? "Nouveau créneau" : "Modifier le créneau");
        dialogue.setHeaderText(nomJour(jour));
        dialogue.getDialogPane().setContent(formulaire);
        if (creneauExistant != null) {
            dialogue.getDialogPane().getButtonTypes().add(boutonSupprimer);
        }
        dialogue.getDialogPane().getButtonTypes().addAll(boutonValider, ButtonType.CANCEL);

        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() == ButtonType.CANCEL) {
            return;
        }

        if (resultat.get() == boutonSupprimer) {
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

        if (creneauExistant == null) {
            emploiDuTemps.ajouterCreneau(jour, debut, fin, coursChoisi.getId());
        } else {
            creneauExistant.setJour(jour);
            creneauExistant.setHeureDebut(debut);
            creneauExistant.setHeureFin(fin);
            creneauExistant.setCoursId(coursChoisi.getId());
        }
        notifierChangement();
        rafraichir();
    }

    private List<LocalTime> genererLimites() {
        List<LocalTime> limites = new ArrayList<>();
        LocalTime t = HEURE_DEBUT_GRILLE;
        while (!t.isAfter(HEURE_FIN_GRILLE)) {
            limites.add(t);
            t = t.plusMinutes(PAS_MINUTES);
        }
        return limites;
    }

    private int indexLigne(LocalTime heure) {
        return (int) Duration.between(HEURE_DEBUT_GRILLE, heure).toMinutes() / PAS_MINUTES;
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
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
