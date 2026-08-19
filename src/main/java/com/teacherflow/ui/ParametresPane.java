package com.teacherflow.ui;

import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Parametres;
import com.teacherflow.model.PlageHoraire;
import com.teacherflow.util.NomsJours;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Réglages de l'emploi du temps : jours affichés, incrément minimal pour le déplacement/
 * redimensionnement des créneaux, et plages horaires actives par jour (pour représenter des
 * pauses irrégulières). Chaque modification est sauvegardée immédiatement ; la grille de
 * l'emploi du temps se met à jour au prochain affichage de son onglet.
 */
public class ParametresPane extends BorderPane {

    private final Runnable surChangement;

    public ParametresPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.surChangement = surChangement;

        setPadding(new Insets(16));

        Parametres parametres = emploiDuTemps.getParametres();

        VBox contenu = new VBox(20,
                construireSectionJours(parametres),
                construireSectionIncrement(parametres),
                titreSection("Plages horaires par jour"),
                new Label("Laisser un jour sans plage le garde actif sur toute l'amplitude affichée."));
        for (DayOfWeek jour : DayOfWeek.values()) {
            contenu.getChildren().add(construireLignePourJour(jour, parametres));
        }
        contenu.setPadding(new Insets(0, 12, 0, 0));

        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        setCenter(defilement);
    }

    private VBox construireSectionJours(Parametres parametres) {
        VBox conteneur = new VBox(6, titreSection("Jours affichés dans l'emploi du temps"));
        for (DayOfWeek jour : DayOfWeek.values()) {
            CheckBox caseJour = new CheckBox(NomsJours.nom(jour));
            caseJour.setSelected(parametres.getJoursAffiches().contains(jour));
            caseJour.selectedProperty().addListener((obs, ancien, coche) -> {
                List<DayOfWeek> jours = new ArrayList<>(parametres.getJoursAffiches());
                if (coche) {
                    if (!jours.contains(jour)) {
                        jours.add(jour);
                        jours.sort(Comparator.comparingInt(DayOfWeek::getValue));
                    }
                } else {
                    jours.remove(jour);
                }
                parametres.setJoursAffiches(jours);
                notifierChangement();
            });
            conteneur.getChildren().add(caseJour);
        }
        return conteneur;
    }

    private HBox construireSectionIncrement(Parametres parametres) {
        ComboBox<Integer> choixPas = new ComboBox<>();
        choixPas.getItems().addAll(5, 10, 15, 20, 30, 60);
        choixPas.setValue(parametres.getPasMinutes());
        choixPas.setOnAction(e -> {
            Integer valeur = choixPas.getValue();
            if (valeur != null) {
                parametres.setPasMinutes(valeur);
                notifierChangement();
            }
        });

        HBox ligne = new HBox(8, new Label("Incrément minimal (minutes)"), choixPas);
        ligne.setAlignment(Pos.CENTER_LEFT);
        return ligne;
    }

    private VBox construireLignePourJour(DayOfWeek jour, Parametres parametres) {
        Label titre = new Label(NomsJours.nom(jour));
        titre.setStyle("-fx-font-weight: bold;");

        ListView<PlageHoraire> listePlages = new ListView<>();
        listePlages.setPrefHeight(90);
        listePlages.getItems().addAll(parametres.plagesPour(jour));
        listePlages.setCellFactory(vue -> new PlageCell(jour, listePlages, parametres));

        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(optionsHeures());
        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(optionsHeures());

        Button boutonAjouter = new Button("Ajouter une plage");
        boutonAjouter.setOnAction(e -> {
            LocalTime debut = choixDebut.getValue();
            LocalTime fin = choixFin.getValue();
            if (debut == null || fin == null || !fin.isAfter(debut)) {
                return;
            }
            List<PlageHoraire> plages = new ArrayList<>(parametres.plagesPour(jour));
            plages.add(new PlageHoraire(debut, fin));
            parametres.getPlagesParJour().put(jour, plages);
            listePlages.getItems().setAll(plages);
            notifierChangement();
        });

        HBox ligneAjout = new HBox(8, choixDebut, choixFin, boutonAjouter);
        ligneAjout.setAlignment(Pos.CENTER_LEFT);

        VBox conteneur = new VBox(4, titre, listePlages, ligneAjout);
        conteneur.setPadding(new Insets(0, 0, 8, 0));
        return conteneur;
    }

    private static List<LocalTime> optionsHeures() {
        List<LocalTime> options = new ArrayList<>();
        LocalTime heure = LocalTime.of(6, 0);
        LocalTime fin = LocalTime.of(22, 0);
        while (!heure.isAfter(fin)) {
            options.add(heure);
            heure = heure.plusMinutes(30);
        }
        return options;
    }

    private static Label titreSection(String texte) {
        Label label = new Label(texte);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
    }

    private class PlageCell extends ListCell<PlageHoraire> {
        private final Label libelle = new Label();
        private final Button boutonSupprimer = new Button();
        private final HBox ligne = new HBox();

        PlageCell(DayOfWeek jour, ListView<PlageHoraire> listeAssociee, Parametres parametres) {
            boutonSupprimer.setGraphic(Icons.poubelle());
            boutonSupprimer.setOnAction(e -> {
                PlageHoraire plage = getItem();
                if (plage == null) {
                    return;
                }
                List<PlageHoraire> plages = new ArrayList<>(parametres.plagesPour(jour));
                plages.remove(plage);
                parametres.getPlagesParJour().put(jour, plages);
                listeAssociee.getItems().remove(plage);
                notifierChangement();
            });

            Region espaceur = new Region();
            HBox.setHgrow(espaceur, Priority.ALWAYS);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getChildren().addAll(libelle, espaceur, boutonSupprimer);
        }

        @Override
        protected void updateItem(PlageHoraire plage, boolean vide) {
            super.updateItem(plage, vide);
            if (vide || plage == null) {
                setGraphic(null);
                return;
            }
            libelle.setText(plage.getDebut() + " - " + plage.getFin());
            setGraphic(ligne);
        }
    }
}
