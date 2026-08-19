package com.teacherflow.ui;

import com.teacherflow.model.Cours;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Écran de gestion des Cours : liste des cours existants, création/renommage/couleur,
 * et gestion de la bibliothèque de fichiers de chaque cours.
 */
public class CoursGestionPane extends BorderPane {

    private static final String COULEUR_PAR_DEFAUT = "#3498db";
    private static final String TOUS_LES_DOSSIERS = "Tous les dossiers";
    private static final String SANS_DOSSIER = "Sans dossier";

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
    private final ComboBox<String> filtreDossier = new ComboBox<>();
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

        filtreDossier.setOnAction(e -> fichiersFiltres.setPredicate(this::fichierCorrespond));

        HBox ligneRecherche = new HBox(8, rechercheFichiers, filtreDossier);
        HBox.setHgrow(rechercheFichiers, Priority.ALWAYS);

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
                titreFichiers, ligneRecherche, listeFichiers, boutonsFichiers);
        panneauDetails.setPadding(new Insets(0, 0, 0, 12));
        return panneauDetails;
    }

    private boolean fichierCorrespond(Fichier fichier) {
        String dossierChoisi = filtreDossier.getValue();
        if (dossierChoisi != null && !dossierChoisi.equals(TOUS_LES_DOSSIERS)) {
            boolean sansDossier = dossierChoisi.equals(SANS_DOSSIER);
            boolean correspondDossier = sansDossier
                    ? fichier.getDossier() == null || fichier.getDossier().isBlank()
                    : dossierChoisi.equals(fichier.getDossier());
            if (!correspondDossier) {
                return false;
            }
        }

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

    private void rafraichirFiltreDossier(Cours cours, boolean conserverSelection) {
        String selectionActuelle = filtreDossier.getValue();
        List<String> dossiers = cours.getFichiers().stream()
                .map(Fichier::getDossier)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        filtreDossier.getItems().setAll(TOUS_LES_DOSSIERS, SANS_DOSSIER);
        filtreDossier.getItems().addAll(dossiers);
        filtreDossier.setValue(
                conserverSelection && selectionActuelle != null && filtreDossier.getItems().contains(selectionActuelle)
                        ? selectionActuelle : TOUS_LES_DOSSIERS);
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
        rafraichirFiltreDossier(cours, false);
        fichiersFiltres.setPredicate(this::fichierCorrespond);
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
        private final Button boutonDossier = new Button("Dossier");
        private final Button boutonTags = new Button("Tags");
        private final Button boutonSupprimer = new Button();
        private final HBox ligne = new HBox(8);

        FichierCell() {
            boutonDossier.setOnAction(e -> {
                Fichier fichier = getItem();
                if (fichier != null) {
                    modifierDossier(fichier);
                }
            });

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
            ligne.getChildren().addAll(libelle, boutonDossier, boutonTags, boutonSupprimer);
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
            if (fichier.getDossier() != null && !fichier.getDossier().isBlank()) {
                texte = fichier.getDossier() + " / " + texte;
            }
            if (!fichier.getTags().isEmpty()) {
                texte += "  [" + String.join(", ", fichier.getTags()) + "]";
            }
            libelle.setText(texte);
            setGraphic(ligne);
        }
    }

    private void modifierDossier(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        TextInputDialog dialogue = new TextInputDialog(fichier.getDossier() != null ? fichier.getDossier() : "");
        dialogue.setTitle("Dossier");
        dialogue.setHeaderText(null);
        dialogue.setContentText("Nom du dossier (vide pour aucun) :");
        Optional<String> resultat = dialogue.showAndWait();
        if (resultat.isEmpty()) {
            return;
        }
        String dossier = resultat.get().trim();
        fichier.setDossier(dossier.isBlank() ? null : dossier);
        listeFichiers.refresh();
        rafraichirFiltreDossier(selectionne, true);
        fichiersFiltres.setPredicate(this::fichierCorrespond);
        notifierChangement();
    }

    private void modifierTags(Fichier fichier) {
        TextInputDialog dialogue = new TextInputDialog(String.join(", ", fichier.getTags()));
        dialogue.setTitle("Étiquettes");
        dialogue.setHeaderText(null);
        dialogue.setContentText("Étiquettes (séparées par des virgules) :");
        Optional<String> resultat = dialogue.showAndWait();
        if (resultat.isEmpty()) {
            return;
        }
        List<String> tags = Arrays.stream(resultat.get().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .collect(Collectors.toList());
        fichier.setTags(tags);
        listeFichiers.refresh();
        notifierChangement();
    }
}
