package com.teacherflow.ui;

import com.teacherflow.model.Cours;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Écran de gestion des Cours : liste des cours existants, création/renommage/couleur,
 * et gestion de la bibliothèque de fichiers de chaque cours.
 */
public class CoursGestionPane extends BorderPane {

    private static final String COULEUR_PAR_DEFAUT = "#3498db";

    private final EmploiDuTemps emploiDuTemps;
    private final Runnable surChangement;

    private final ListView<Cours> listeCours = new ListView<>();
    private final ListView<Fichier> listeFichiers = new ListView<>();
    private final TextField champNom = new TextField();
    private final ColorPicker selecteurCouleur = new ColorPicker();
    private final Label messageVide = new Label("Sélectionnez un cours ou créez-en un nouveau.");
    private final VBox panneauDetails = new VBox(12);

    public CoursGestionPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;

        setPadding(new Insets(12));
        setLeft(construireColonneListe());
        setCenter(construireDetails());

        listeCours.getItems().addAll(emploiDuTemps.getCours());
        listeCours.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> afficherDetails(nouveau));
        afficherDetails(null);
    }

    private VBox construireColonneListe() {
        listeCours.setCellFactory(vue -> new CoursCell());
        VBox.setVgrow(listeCours, Priority.ALWAYS);

        Button boutonNouveau = new Button("Nouveau cours");
        boutonNouveau.setMaxWidth(Double.MAX_VALUE);
        boutonNouveau.setOnAction(e -> creerCours());

        Button boutonSupprimer = new Button("Supprimer le cours");
        boutonSupprimer.setMaxWidth(Double.MAX_VALUE);
        boutonSupprimer.setOnAction(e -> supprimerCoursSelectionne());

        VBox colonne = new VBox(8, listeCours, boutonNouveau, boutonSupprimer);
        colonne.setPadding(new Insets(0, 12, 0, 0));
        colonne.setPrefWidth(220);
        return colonne;
    }

    private VBox construireDetails() {
        champNom.setPromptText("Nom du cours");
        champNom.textProperty().addListener((obs, ancien, nouveau) -> mettreAJourNomEnMemoire(nouveau));
        champNom.focusedProperty().addListener((obs, avaitFocus, aFocus) -> {
            if (!aFocus) {
                notifierChangement();
            }
        });
        champNom.setOnAction(e -> notifierChangement());

        selecteurCouleur.setOnAction(e -> recolorerCoursSelectionne());

        HBox ligneNomCouleur = new HBox(8, champNom, selecteurCouleur);
        ligneNomCouleur.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(champNom, Priority.ALWAYS);

        listeFichiers.setCellFactory(vue -> new FichierCell());
        VBox.setVgrow(listeFichiers, Priority.ALWAYS);

        Button boutonAjouterFichier = new Button("Ajouter des fichiers...");
        boutonAjouterFichier.setOnAction(e -> ajouterFichiers());

        Button boutonRetirerFichier = new Button("Retirer le fichier sélectionné");
        boutonRetirerFichier.setOnAction(e -> retirerFichierSelectionne());

        HBox boutonsFichiers = new HBox(8, boutonAjouterFichier, boutonRetirerFichier);

        panneauDetails.getChildren().addAll(
                new Label("Nom et couleur"), ligneNomCouleur,
                new Label("Fichiers du cours"), listeFichiers, boutonsFichiers);
        panneauDetails.setPadding(new Insets(0, 0, 0, 12));
        return panneauDetails;
    }

    private void afficherDetails(Cours cours) {
        boolean unCoursSelectionne = cours != null;
        setCenter(unCoursSelectionne ? panneauDetails : messageVide);

        if (!unCoursSelectionne) {
            return;
        }

        champNom.setText(cours.getNom());
        selecteurCouleur.setValue(Color.web(
                cours.getCouleur() != null ? cours.getCouleur() : COULEUR_PAR_DEFAUT));
        listeFichiers.getItems().setAll(cours.getFichiers());
    }

    private void creerCours() {
        Cours nouveauCours = emploiDuTemps.ajouterCours("Nouveau cours", COULEUR_PAR_DEFAUT);
        listeCours.getItems().add(nouveauCours);
        listeCours.getSelectionModel().select(nouveauCours);
        champNom.requestFocus();
        champNom.selectAll();
        notifierChangement();
    }

    private void supprimerCoursSelectionne() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le cours \"" + selectionne.getNom() + "\" et ses créneaux associés ?");
        confirmation.setTitle("Supprimer le cours");
        confirmation.setHeaderText(null);
        Optional<ButtonType> reponse = confirmation.showAndWait();
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            emploiDuTemps.supprimerCours(selectionne.getId());
            listeCours.getItems().remove(selectionne);
            notifierChangement();
        }
    }

    private void mettreAJourNomEnMemoire(String nouveauNom) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        selectionne.setNom(nouveauNom);
        listeCours.refresh();
    }

    private void recolorerCoursSelectionne() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        Color couleur = selecteurCouleur.getValue();
        selectionne.setCouleur(String.format("#%02X%02X%02X",
                (int) Math.round(couleur.getRed() * 255),
                (int) Math.round(couleur.getGreen() * 255),
                (int) Math.round(couleur.getBlue() * 255)));
        listeCours.refresh();
        notifierChangement();
    }

    private void ajouterFichiers() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        FileChooser selecteur = new FileChooser();
        selecteur.setTitle("Choisir des fichiers pour \"" + selectionne.getNom() + "\"");
        Window fenetre = getScene() != null ? getScene().getWindow() : null;
        List<File> fichiers = selecteur.showOpenMultipleDialog(fenetre);
        if (fichiers == null || fichiers.isEmpty()) {
            return;
        }
        for (File fichier : fichiers) {
            selectionne.ajouterFichier(fichier.getAbsolutePath(), fichier.getName());
        }
        listeFichiers.getItems().setAll(selectionne.getFichiers());
        notifierChangement();
    }

    private void retirerFichierSelectionne() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        Fichier fichierSelectionne = listeFichiers.getSelectionModel().getSelectedItem();
        if (selectionne == null || fichierSelectionne == null) {
            return;
        }
        selectionne.retirerFichier(fichierSelectionne.getId());
        listeFichiers.getItems().remove(fichierSelectionne);
        notifierChangement();
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
    }

    private static class CoursCell extends ListCell<Cours> {
        private final Circle pastille = new Circle(6);

        @Override
        protected void updateItem(Cours cours, boolean vide) {
            super.updateItem(cours, vide);
            if (vide || cours == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            pastille.setFill(Color.web(cours.getCouleur() != null ? cours.getCouleur() : COULEUR_PAR_DEFAUT));
            setGraphic(pastille);
            String nom = cours.getNom();
            setText(nom == null || nom.isBlank() ? "(sans nom)" : nom);
        }
    }

    private static class FichierCell extends ListCell<Fichier> {
        @Override
        protected void updateItem(Fichier fichier, boolean vide) {
            super.updateItem(fichier, vide);
            if (vide || fichier == null) {
                setText(null);
                return;
            }
            String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                    ? fichier.getNomAffichage() : fichier.getChemin();
            setText(libelle + "  (" + fichier.getChemin() + ")");
        }
    }
}
