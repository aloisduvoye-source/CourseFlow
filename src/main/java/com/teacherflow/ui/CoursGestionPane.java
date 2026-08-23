package com.teacherflow.ui;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.DossierReference;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.Parametres;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import java.util.UUID;

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
    private final VBox conteneurDossiersReferences = new VBox(4);
    private final ObservableList<Cours> tousLesCours = FXCollections.observableArrayList();
    private final FilteredList<Cours> coursFiltres = new FilteredList<>(tousLesCours);
    private final ObservableList<Fichier> tousLesFichiers = FXCollections.observableArrayList();
    private final FilteredList<Fichier> fichiersFiltres = new FilteredList<>(tousLesFichiers);
    private final TextField rechercheCours = new TextField();
    private final TextField rechercheFichiers = new TextField();
    private final TextField champNom = new TextField();
    private final ColorPicker selecteurCouleur = new ColorPicker();
    private final CheckBox caseCoursDefaut = new CheckBox("Cours par défaut");
    private final TitledPane panneauCoursDefaut = new TitledPane();
    private final ListView<Fichier> listeFichiersCoursDefaut = new ListView<>();
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

        caseCoursDefaut.setOnAction(e -> toggleCoursDefaut());

        listeFichiersCoursDefaut.setPrefHeight(140);
        listeFichiersCoursDefaut.setCellFactory(CheckBoxListCell.forListView(this::proprieteFichierLie));
        panneauCoursDefaut.setContent(listeFichiersCoursDefaut);
        panneauCoursDefaut.setExpanded(false);

        listeFichiers.setCellFactory(vue -> new FichierCell());
        listeFichiers.setItems(fichiersFiltres);
        VBox.setVgrow(listeFichiers, Priority.ALWAYS);

        rechercheFichiers.setPromptText("Rechercher un fichier...");
        rechercheFichiers.textProperty().addListener((obs, ancien, texte) -> fichiersFiltres.setPredicate(this::fichierCorrespond));

        Button boutonAjouterFichier = new Button("Ajouter des fichiers...");
        boutonAjouterFichier.setOnAction(e -> ajouterFichiers());

        Button boutonReferencerDossier = new Button("Référencer un dossier...");
        boutonReferencerDossier.setOnAction(e -> referencerDossier());

        Button boutonAjouterLien = new Button("Ajouter un lien web...");
        boutonAjouterLien.setOnAction(e -> ajouterLienWeb());

        HBox boutonsFichiers = new HBox(8, boutonAjouterFichier, boutonReferencerDossier, boutonAjouterLien);

        Label titreNomCouleur = new Label("Nom et couleur");
        Label titreFichiers = new Label("Fichiers du cours");
        Label titreDossiersReferences = new Label("Dossiers référencés");

        panneauDetails.getChildren().addAll(
                titreNomCouleur, ligneNomCouleur, caseCoursDefaut,
                titreFichiers, rechercheFichiers, listeFichiers, boutonsFichiers,
                titreDossiersReferences, conteneurDossiersReferences,
                panneauCoursDefaut);
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
        tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(cours));
        rafraichirDossiersReferences(cours);

        UUID coursDefautId = emploiDuTemps.getParametres().getCoursDefautId();
        boolean estCoursDefaut = cours.getId().equals(coursDefautId);
        caseCoursDefaut.setSelected(estCoursDefaut);

        Optional<Cours> coursDefaut = emploiDuTemps.trouverCoursDefaut();
        boolean afficherPanneauDefaut = coursDefaut.isPresent() && !estCoursDefaut;
        panneauCoursDefaut.setVisible(afficherPanneauDefaut);
        panneauCoursDefaut.setManaged(afficherPanneauDefaut);
        if (afficherPanneauDefaut) {
            panneauCoursDefaut.setText("Cours par défaut : " + coursDefaut.get().getNom());
            listeFichiersCoursDefaut.getItems().setAll(coursDefaut.get().getFichiers());
        }
    }

    private void rafraichirDossiersReferences(Cours cours) {
        conteneurDossiersReferences.getChildren().clear();
        for (DossierReference reference : cours.getDossiersReferences()) {
            conteneurDossiersReferences.getChildren().add(construireLigneDossierReference(cours, reference));
        }
    }

    /**
     * Reconstruit uniquement le contenu des lignes de dossier référencé déjà dépliées (sans
     * changer l'ordre ni l'état déplié/replié), pour refléter un fichier supprimé/ajouté par
     * ailleurs (ex. via la corbeille de la liste principale) sans attendre un nouveau dépliage.
     */
    private void rafraichirContenuDossiersReferencesOuverts(Cours cours) {
        List<DossierReference> references = cours.getDossiersReferences();
        for (int i = 0; i < conteneurDossiersReferences.getChildren().size() && i < references.size(); i++) {
            if (conteneurDossiersReferences.getChildren().get(i) instanceof TitledPane panneau && panneau.isExpanded()) {
                panneau.setContent(construireContenuDossierReference(cours, references.get(i)));
            }
        }
    }

    /**
     * Une ligne dépliable par dossier référencé : icône dossier + chemin en en-tête, et une
     * fois dépliée, une case à cocher par fichier trouvé dans le dossier (coché = importé
     * dans le cours). Le scan du disque n'a lieu qu'au dépliage, pour rester réactif.
     */
    private TitledPane construireLigneDossierReference(Cours cours, DossierReference reference) {
        TitledPane panneau = new TitledPane();
        panneau.setGraphic(Icons.dossier());
        panneau.setText(reference.getChemin());
        panneau.setExpanded(false);
        panneau.setMaxWidth(Double.MAX_VALUE);
        panneau.setContent(new VBox());
        panneau.expandedProperty().addListener((obs, etaitDeplie, estDeplie) -> {
            if (estDeplie) {
                panneau.setContent(construireContenuDossierReference(cours, reference));
            }
        });
        return panneau;
    }

    private VBox construireContenuDossierReference(Cours cours, DossierReference reference) {
        File dossierBase = new File(reference.getChemin());
        List<File> fichiersTrouves = scannerDossier(dossierBase);

        VBox contenu = new VBox(4);
        contenu.setPadding(new Insets(4, 0, 4, 12));
        if (fichiersTrouves.isEmpty()) {
            contenu.getChildren().add(new Label("(aucun fichier trouvé)"));
        } else {
            for (File fichier : fichiersTrouves) {
                CheckBox caseFichier = new CheckBox(fichier.getName());
                caseFichier.setSelected(reference.getFichiersImportes().contains(fichier.getAbsolutePath()));
                caseFichier.setOnAction(e -> toggleFichierDossierReference(cours, reference, fichier, caseFichier.isSelected()));
                contenu.getChildren().add(caseFichier);
            }
        }

        Button boutonDelier = new Button("Délier ce dossier (retire aussi ses fichiers importés)");
        boutonDelier.setOnAction(e -> delierDossierReference(reference));
        contenu.getChildren().add(boutonDelier);

        return contenu;
    }

    private void toggleFichierDossierReference(Cours cours, DossierReference reference, File fichier, boolean coche) {
        if (coche) {
            if (!reference.getFichiersImportes().contains(fichier.getAbsolutePath())) {
                cours.ajouterFichier(fichier.getAbsolutePath(), fichier.getName());
                reference.getFichiersImportes().add(fichier.getAbsolutePath());
            }
        } else {
            cours.getFichiers().stream()
                    .filter(f -> f.getChemin().equals(fichier.getAbsolutePath()))
                    .findFirst()
                    .ifPresent(f -> cours.retirerFichier(f.getId()));
            reference.getFichiersImportes().remove(fichier.getAbsolutePath());
        }
        tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(cours));
        notifierChangement();
    }

    private void toggleCoursDefaut() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        emploiDuTemps.getParametres().setCoursDefautId(
                caseCoursDefaut.isSelected() ? selectionne.getId() : null);
        notifierChangement();
        afficherDetails(selectionne);
    }

    private ObservableValue<Boolean> proprieteFichierLie(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        boolean lie = selectionne != null && selectionne.getFichiersLies().contains(fichier.getId());
        SimpleBooleanProperty propriete = new SimpleBooleanProperty(lie);
        propriete.addListener((obs, etaitCoche, estCoche) -> {
            Cours coursActuel = listeCours.getSelectionModel().getSelectedItem();
            if (coursActuel == null) {
                return;
            }
            if (estCoche) {
                coursActuel.ajouterFichierLie(fichier.getId());
            } else {
                coursActuel.retirerFichierLie(fichier.getId());
            }
            tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(coursActuel));
            notifierChangement();
        });
        return propriete;
    }

    private boolean estFichierLie(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        return selectionne != null && selectionne.getFichiersLies().contains(fichier.getId());
    }

    private void delierFichier(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        selectionne.retirerFichierLie(fichier.getId());
        tousLesFichiers.remove(fichier);
        listeFichiersCoursDefaut.refresh();
        notifierChangement();
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
        tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(selectionne));
        notifierChangement();
    }

    private void referencerDossier() {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        DirectoryChooser selecteurDossier = new DirectoryChooser();
        selecteurDossier.setTitle("Choisir un dossier pour \"" + selectionne.getNom() + "\"");
        Window fenetre = getScene() != null ? getScene().getWindow() : null;
        File dossier = selecteurDossier.showDialog(fenetre);
        if (dossier == null) {
            return;
        }

        CheckBox caseRecursif = new CheckBox("Recherche récursive (référencer aussi chaque sous-dossier séparément)");
        Dialog<ButtonType> dialogueMode = new Dialog<>();
        dialogueMode.setTitle("Référencer un dossier");
        dialogueMode.getDialogPane().setContent(caseRecursif);
        dialogueMode.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> reponseMode = dialogueMode.showAndWait();
        if (reponseMode.isEmpty() || reponseMode.get() != ButtonType.OK) {
            return;
        }

        List<File> dossiersAReferencer = caseRecursif.isSelected() ? listerArborescence(dossier) : List.of(dossier);
        TitledPane premiereLigne = null;
        for (File dossierAReferencer : dossiersAReferencer) {
            DossierReference reference = new DossierReference(dossierAReferencer.getAbsolutePath());
            selectionne.getDossiersReferences().add(reference);
            TitledPane ligne = construireLigneDossierReference(selectionne, reference);
            conteneurDossiersReferences.getChildren().add(ligne);
            if (premiereLigne == null) {
                premiereLigne = ligne;
            }
        }
        if (premiereLigne != null) {
            premiereLigne.setExpanded(true);
        }
        notifierChangement();
    }

    /**
     * @return le dossier donné suivi de chacun de ses sous-dossiers (à toute profondeur),
     * pour créer une {@link DossierReference} indépendante par dossier de l'arborescence
     * plutôt qu'une seule référence "profonde" — délier l'un n'affecte pas les autres.
     */
    private static List<File> listerArborescence(File racine) {
        List<File> resultat = new ArrayList<>();
        resultat.add(racine);
        File[] enfants = racine.listFiles();
        if (enfants != null) {
            for (File enfant : enfants) {
                if (enfant.isDirectory() && !enfant.isHidden()) {
                    resultat.addAll(listerArborescence(enfant));
                }
            }
        }
        return resultat;
    }

    /**
     * Délie le dossier référencé : retire aussi du cours tous les fichiers qui avaient été
     * importés depuis lui (pas seulement la référence elle-même).
     */
    private void delierDossierReference(DossierReference reference) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        for (String chemin : reference.getFichiersImportes()) {
            selectionne.getFichiers().stream()
                    .filter(f -> f.getChemin().equals(chemin))
                    .findFirst()
                    .ifPresent(f -> selectionne.retirerFichier(f.getId()));
        }
        selectionne.getDossiersReferences().remove(reference);
        tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(selectionne));
        rafraichirDossiersReferences(selectionne);
        notifierChangement();
    }

    private static List<File> scannerDossier(File dossier) {
        List<File> resultat = new ArrayList<>();
        File[] enfants = dossier.listFiles();
        if (enfants == null) {
            return resultat;
        }
        for (File enfant : enfants) {
            if (enfant.isFile() && !enfant.isHidden()) {
                resultat.add(enfant);
            }
        }
        return resultat;
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
        if (!OuvreurFichiers.estUrl(url)) {
            Alert erreur = new Alert(Alert.AlertType.ERROR,
                    "L'URL doit commencer par un schéma (ex. https://).");
            erreur.setTitle("URL invalide");
            erreur.setHeaderText(null);
            erreur.showAndWait();
            return;
        }
        selectionne.ajouterFichier(url, url);
        tousLesFichiers.setAll(emploiDuTemps.fichiersVisibles(selectionne));
        notifierChangement();
    }

    private void retirerFichier(Fichier fichier) {
        Cours selectionne = listeCours.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            return;
        }
        selectionne.retirerFichier(fichier.getId());
        for (DossierReference reference : selectionne.getDossiersReferences()) {
            reference.getFichiersImportes().remove(fichier.getChemin());
        }
        tousLesFichiers.remove(fichier);
        rafraichirContenuDossiersReferencesOuverts(selectionne);
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
                if (fichier == null) {
                    return;
                }
                if (estFichierLie(fichier)) {
                    delierFichier(fichier);
                } else {
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
            if (estFichierLie(fichier)) {
                texte += " (lié)";
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
