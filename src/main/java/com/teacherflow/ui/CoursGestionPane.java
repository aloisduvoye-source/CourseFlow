package com.teacherflow.ui;

import com.teacherflow.model.Cours;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.Parametres;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final ObservableList<Cours> tousLesCours = FXCollections.observableArrayList();
    private final FilteredList<Cours> coursFiltres = new FilteredList<>(tousLesCours);
    private final ObservableList<Fichier> tousLesFichiers = FXCollections.observableArrayList();
    private final FilteredList<Fichier> fichiersFiltres = new FilteredList<>(tousLesFichiers);
    private final TextField rechercheCours = new TextField();
    private final TextField rechercheFichiers = new TextField();
    private final TextField champNom = new TextField();
    private final ColorPicker selecteurCouleur = new ColorPicker();
    private final Label messageVide = new Label("Sélectionnez un cours ou créez-en un nouveau.");
    private final VBox panneauDetails = new VBox(12);

    public CoursGestionPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;

        setPadding(new Insets(16));
        setLeft(construireColonneListe());
        setCenter(construireDetails());

        tousLesCours.addAll(emploiDuTemps.getCours());
        listeCours.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> afficherDetails(nouveau));
        afficherDetails(null);
    }

    private VBox construireColonneListe() {
        listeCours.setCellFactory(vue -> new CoursCell());
        listeCours.setItems(coursFiltres);
        VBox.setVgrow(listeCours, Priority.ALWAYS);

        rechercheCours.setPromptText("Rechercher un cours...");
        rechercheCours.textProperty().addListener((obs, ancien, texte) -> coursFiltres.setPredicate(this::coursCorrespond));

        Button boutonNouveau = new Button("Nouveau cours");
        boutonNouveau.setMaxWidth(Double.MAX_VALUE);
        boutonNouveau.setOnAction(e -> creerCours());

        VBox colonne = new VBox(8, rechercheCours, listeCours, boutonNouveau);
        colonne.setPadding(new Insets(0, 12, 0, 0));
        colonne.setPrefWidth(220);
        return colonne;
    }

    private boolean coursCorrespond(Cours cours) {
        String texte = rechercheCours.getText();
        return texte == null || texte.isBlank()
                || (cours.getNom() != null && cours.getNom().toLowerCase().contains(texte.toLowerCase()));
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
        listeFichiers.setItems(fichiersFiltres);
        VBox.setVgrow(listeFichiers, Priority.ALWAYS);

        rechercheFichiers.setPromptText("Rechercher un fichier...");
        rechercheFichiers.textProperty().addListener((obs, ancien, texte) -> fichiersFiltres.setPredicate(this::fichierCorrespond));

        Button boutonAjouterFichier = new Button("Ajouter des fichiers...");
        boutonAjouterFichier.setOnAction(e -> ajouterFichiers());

        Button boutonAjouterDossier = new Button("Ajouter un dossier...");
        boutonAjouterDossier.setOnAction(e -> ajouterDossier());

        Button boutonAjouterLien = new Button("Ajouter un lien web...");
        boutonAjouterLien.setOnAction(e -> ajouterLienWeb());

        HBox boutonsFichiers = new HBox(8, boutonAjouterFichier, boutonAjouterDossier, boutonAjouterLien);

        Label titreNomCouleur = new Label("Nom et couleur");
        Label titreFichiers = new Label("Fichiers du cours");

        panneauDetails.getChildren().addAll(
                titreNomCouleur, ligneNomCouleur,
                titreFichiers, rechercheFichiers, listeFichiers, boutonsFichiers);
        panneauDetails.setPadding(new Insets(0, 0, 0, 12));
        return panneauDetails;
    }

    private boolean fichierCorrespond(Fichier fichier) {
        String texte = rechercheFichiers.getText();
        if (texte == null || texte.isBlank()) {
            return true;
        }
        String recherche = texte.toLowerCase();
        String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                ? fichier.getNomAffichage() : fichier.getChemin();
        boolean libelleCorrespond = libelle != null && libelle.toLowerCase().contains(recherche);
        boolean tagCorrespond = fichier.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(recherche));
        return libelleCorrespond || tagCorrespond;
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
        rechercheFichiers.clear();
        tousLesFichiers.setAll(cours.getFichiers());
    }

    private void creerCours() {
        rechercheCours.clear();
        Cours nouveauCours = emploiDuTemps.ajouterCours("Nouveau cours", couleurAleatoire());
        tousLesCours.add(nouveauCours);
        listeCours.getSelectionModel().select(nouveauCours);
        champNom.requestFocus();
        champNom.selectAll();
        notifierChangement();
    }

    private void supprimerCours(Cours cours) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le cours \"" + cours.getNom() + "\" et ses créneaux associés ?");
        confirmation.setTitle("Supprimer le cours");
        confirmation.setHeaderText(null);
        Optional<ButtonType> reponse = confirmation.showAndWait();
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            emploiDuTemps.supprimerCours(cours.getId());
            tousLesCours.remove(cours);
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
        tousLesFichiers.setAll(selectionne.getFichiers());
        notifierChangement();
    }

    private void ajouterDossier() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        DirectoryChooser selecteur = new DirectoryChooser();
        selecteur.setTitle("Choisir un dossier pour \"" + selectionne.getNom() + "\"");
        Window fenetre = getScene() != null ? getScene().getWindow() : null;
        File dossier = selecteur.showDialog(fenetre);
        if (dossier == null) {
            return;
        }
        File[] fichiersDuDossier = dossier.listFiles(f -> f.isFile() && !f.isHidden());
        if (fichiersDuDossier == null || fichiersDuDossier.length == 0) {
            Alert info = new Alert(Alert.AlertType.INFORMATION, "Aucun fichier trouvé dans ce dossier.");
            info.setTitle("Dossier vide");
            info.setHeaderText(null);
            info.showAndWait();
            return;
        }
        for (File fichier : fichiersDuDossier) {
            selectionne.ajouterFichier(fichier.getAbsolutePath(), fichier.getName());
        }
        tousLesFichiers.setAll(selectionne.getFichiers());
        notifierChangement();
    }

    private void ajouterLienWeb() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        TextInputDialog dialogue = new TextInputDialog();
        dialogue.setTitle("Ajouter un lien web");
        dialogue.setHeaderText(null);
        dialogue.setContentText("URL :");
        Optional<String> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get().isBlank()) {
            return;
        }
        String url = resultat.get().trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.+")) {
            Alert erreur = new Alert(Alert.AlertType.ERROR,
                    "L'URL doit commencer par un schéma (ex. https://).");
            erreur.setTitle("URL invalide");
            erreur.setHeaderText(null);
            erreur.showAndWait();
            return;
        }
        selectionne.ajouterFichier(url, url);
        tousLesFichiers.setAll(selectionne.getFichiers());
        notifierChangement();
    }

    private void retirerFichier(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        selectionne.retirerFichier(fichier.getId());
        tousLesFichiers.remove(fichier);
        notifierChangement();
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
    }

    private static String couleurAleatoire() {
        Color couleur = Color.hsb(Math.random() * 360, 0.55, 0.85);
        return String.format("#%02X%02X%02X",
                (int) Math.round(couleur.getRed() * 255),
                (int) Math.round(couleur.getGreen() * 255),
                (int) Math.round(couleur.getBlue() * 255));
    }

    private class CoursCell extends ListCell<Cours> {
        private final Circle pastille = new Circle(7);
        private final Label libelle = new Label();
        private final Button boutonSupprimer = new Button();
        private final HBox ligne = new HBox(8);

        CoursCell() {
            pastille.setStroke(Color.WHITE);
            pastille.setStrokeWidth(1.5);

            boutonSupprimer.setGraphic(Icons.poubelle());
            boutonSupprimer.setOnAction(e -> {
                Cours cours = getItem();
                if (cours != null) {
                    supprimerCours(cours);
                }
            });

            ligne.prefWidthProperty().bind(listeCours.widthProperty().subtract(24));
            libelle.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(libelle, Priority.ALWAYS);

            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getChildren().addAll(pastille, libelle, boutonSupprimer);
        }

        @Override
        protected void updateItem(Cours cours, boolean vide) {
            super.updateItem(cours, vide);
            if (vide || cours == null) {
                setGraphic(null);
                return;
            }
            pastille.setFill(Color.web(cours.getCouleur() != null ? cours.getCouleur() : COULEUR_PAR_DEFAUT));
            String nom = cours.getNom();
            libelle.setText(nom == null || nom.isBlank() ? "(sans nom)" : nom);
            setGraphic(ligne);
        }
    }

    private class FichierCell extends ListCell<Fichier> {
        private final Label libelle = new Label();
        private final Button boutonTags = new Button("Tags");
        private final Button boutonSupprimer = new Button();
        private final HBox ligne = new HBox(8);

        FichierCell() {
            boutonTags.setOnAction(e -> {
                Fichier fichier = getItem();
                if (fichier != null) {
                    modifierTags(fichier);
                }
            });

            boutonSupprimer.setGraphic(Icons.poubelle());
            boutonSupprimer.setOnAction(e -> {
                Fichier fichier = getItem();
                if (fichier != null) {
                    retirerFichier(fichier);
                }
            });

            ligne.prefWidthProperty().bind(listeFichiers.widthProperty().subtract(24));
            libelle.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(libelle, Priority.ALWAYS);

            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getChildren().addAll(libelle, boutonTags, boutonSupprimer);
        }

        @Override
        protected void updateItem(Fichier fichier, boolean vide) {
            super.updateItem(fichier, vide);
            if (vide || fichier == null) {
                setGraphic(null);
                return;
            }
            String texte = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                    ? fichier.getNomAffichage() : fichier.getChemin();
            if (!fichier.getTags().isEmpty()) {
                texte += "  [" + String.join(", ", fichier.getTags()) + "]";
            }
            libelle.setText(texte);
            setGraphic(ligne);
        }
    }

    private void modifierTags(Fichier fichier) {
        Parametres parametres = emploiDuTemps.getParametres();
        Set<String> tagsCoches = new LinkedHashSet<>(fichier.getTags());

        ListView<String> listeTags = new ListView<>();
        listeTags.setPrefHeight(140);
        listeTags.setCellFactory(CheckBoxListCell.forListView(tag -> {
            SimpleBooleanProperty propriete = new SimpleBooleanProperty(tagsCoches.contains(tag));
            propriete.addListener((obs, etaitCoche, estCoche) -> {
                if (estCoche) {
                    tagsCoches.add(tag);
                } else {
                    tagsCoches.remove(tag);
                }
            });
            return propriete;
        }));
        listeTags.getItems().setAll(parametres.getTagsDisponibles());

        TextField champNouveauTag = new TextField();
        champNouveauTag.setPromptText("Nouveau tag...");
        Button boutonAjouterTag = new Button("Ajouter");
        boutonAjouterTag.setOnAction(e -> {
            String nouveauTag = champNouveauTag.getText().trim();
            if (nouveauTag.isBlank() || parametres.getTagsDisponibles().contains(nouveauTag)) {
                return;
            }
            parametres.getTagsDisponibles().add(nouveauTag);
            tagsCoches.add(nouveauTag);
            listeTags.getItems().setAll(parametres.getTagsDisponibles());
            champNouveauTag.clear();
            notifierChangement();
        });
        HBox ligneAjout = new HBox(8, champNouveauTag, boutonAjouterTag);

        VBox contenu = new VBox(8, listeTags, ligneAjout);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle("Étiquettes");
        dialogue.getDialogPane().setContent(contenu);
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() != ButtonType.OK) {
            return;
        }
        fichier.setTags(new ArrayList<>(tagsCoches));
        listeFichiers.refresh();
        notifierChangement();
    }
}
