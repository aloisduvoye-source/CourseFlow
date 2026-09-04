package com.courseflow.ui;

import atlantafx.base.theme.Styles;
import com.courseflow.io.OuvreurFichiers;
import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.TypeSemaine;
import com.courseflow.util.NomsJours;
import com.courseflow.util.TypeFichier;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Écran d'accueil : agenda du jour affiché — une carte par créneau (cours, salle, description,
 * fichiers avec bouton "Ouvrir"), triées chronologiquement, avec navigation au jour près
 * (flèches ◀▶) plutôt que créneau par créneau. Équivalent graphique de la commande
 * {@code lecture schedule} pour un seul jour.
 */
public class AccueilPane extends BorderPane {

    private static final double TAILLE_VIGNETTE = 40;
    private static final double LARGEUR_COLONNE = 720;
    private static final int SEUIL_FICHIERS_REPLIES = 3;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final EmploiDuTemps emploiDuTemps;
    private final Consumer<Creneau> surDemandeEdition;

    private final Label libelleJour = new Label();
    private final Label libelleDate = new Label();
    private final VBox listeCartes = new VBox(14);
    private final StackPane conteneurCentre = new StackPane();

    private LocalDate dateAffichee;

    public AccueilPane(EmploiDuTemps emploiDuTemps, Consumer<Creneau> surDemandeEdition) {
        this.emploiDuTemps = emploiDuTemps;
        this.surDemandeEdition = surDemandeEdition;

        setPadding(new Insets(0));
        setStyle("-fx-background-color: -color-bg-subtle;");

        libelleJour.setStyle("-fx-font-size: 26; -fx-font-weight: bold;");
        libelleDate.setStyle("-fx-font-size: 13; -fx-text-fill: -color-fg-muted;");

        // Le jour/date + séparateur restent fixes en haut (setTop), en dehors du ScrollPane :
        // seule la liste de cartes défile en dessous.
        VBox entete = new VBox(2, libelleJour, libelleDate);
        VBox blocEntete = new VBox(8, entete, new Separator());
        blocEntete.setMaxWidth(LARGEUR_COLONNE);
        StackPane enteteCentree = new StackPane(blocEntete);
        enteteCentree.setAlignment(Pos.TOP_CENTER);
        enteteCentree.setPadding(new Insets(24, 32, 0, 32));
        setTop(enteteCentree);

        VBox contenuCartes = new VBox(listeCartes);
        contenuCartes.setPadding(new Insets(12, 32, 32, 32));

        ScrollPane defilement = new ScrollPane(contenuCartes);
        defilement.setFitToWidth(true);
        defilement.setPrefViewportWidth(LARGEUR_COLONNE);
        defilement.setMaxWidth(LARGEUR_COLONNE + 20);
        defilement.setStyle("-fx-background-color: transparent;");

        // Les flèches sont regroupées avec la colonne dans un même HBox (plutôt que posées aux
        // coins de la fenêtre via setLeft/setRight du BorderPane) : le groupe entier se recentre
        // ainsi ensemble, et les flèches restent collées aux bords de la colonne quelle que soit
        // la largeur de la fenêtre.
        HBox ligne = new HBox(construireFlecheJour(Icons.flecheGauche(), -1), defilement, construireFlecheJour(Icons.flecheDroite(), 1));
        ligne.setAlignment(Pos.CENTER);

        conteneurCentre.getChildren().add(ligne);
        setCenter(conteneurCentre);

        rafraichir();
    }

    private Button construireFlecheJour(Node icone, int delta) {
        Button bouton = new Button();
        bouton.setGraphic(icone);
        bouton.getStyleClass().add(Styles.BUTTON_CIRCLE);
        bouton.setStyle("-fx-border-color: -color-border-default;"
                + " -fx-border-radius: 100; -fx-background-color: -color-bg-default;");
        bouton.setMinSize(44, 44);
        bouton.setPrefSize(44, 44);
        String libelle = delta < 0 ? "Jour précédent" : "Jour suivant";
        bouton.setTooltip(new Tooltip(libelle));
        bouton.setAccessibleText(libelle);
        bouton.setOnAction(e -> naviguerJour(delta));
        HBox.setMargin(bouton, new Insets(0, 16, 0, 16));
        return bouton;
    }

    /**
     * Revient au jour courant, abandonnant toute navigation en cours. Appelé à la construction
     * et à chaque affichage de l'onglet.
     */
    public void rafraichir() {
        dateAffichee = LocalDate.now();
        afficherJour();
    }

    private void naviguerJour(int delta) {
        dateAffichee = dateAffichee.plusDays(delta);
        afficherJour();
    }

    private void afficherJour() {
        libelleJour.setText(NomsJours.nom(dateAffichee.getDayOfWeek()));
        libelleDate.setText(capitaliser(dateAffichee.format(FORMAT_DATE)));

        TypeSemaine semaine = emploiDuTemps.getParametres().semainePour(dateAffichee);
        List<Creneau> creneauxDuJour = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == dateAffichee.getDayOfWeek())
                .filter(c -> c.correspondA(semaine))
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .collect(Collectors.toList());

        listeCartes.getChildren().clear();
        if (creneauxDuJour.isEmpty()) {
            listeCartes.getChildren().add(construireEtatVide());
        } else {
            boolean aujourdhui = dateAffichee.equals(LocalDate.now());
            for (Creneau creneau : creneauxDuJour) {
                boolean enCours = aujourdhui && creneau.contient(LocalTime.now());
                listeCartes.getChildren().add(construireCarteCreneau(creneau, enCours));
            }
        }
        animerApparition();
    }

    private static String capitaliser(String texte) {
        return texte.isEmpty() ? texte : Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }

    private Label construireEtatVide() {
        Label vide = new Label("Aucun cours ce jour-là");
        vide.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-font-size: 14;");
        vide.setPadding(new Insets(24, 0, 0, 0));
        return vide;
    }

    private VBox construireCarteCreneau(Creneau creneau, boolean enCours) {
        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        String couleur = cours != null && cours.getCouleur() != null ? cours.getCouleur() : Couleurs.COURS_SANS_COULEUR;

        Label libelleHeure = new Label(creneau.getHeureDebut() + " - " + creneau.getHeureFin());
        libelleHeure.setStyle("-fx-font-family: monospace; -fx-text-fill: " + couleur + "; -fx-font-weight: bold;");

        Label libelleNomCours = new Label(cours != null ? cours.getNom() : "(cours supprimé)");
        libelleNomCours.setStyle("-fx-font-size: 15; -fx-font-weight: bold;");

        HBox ligneEntete = new HBox(8, libelleHeure, libelleNomCours);
        ligneEntete.setAlignment(Pos.CENTER_LEFT);
        if (creneau.getSalle() != null && !creneau.getSalle().isBlank()) {
            Label libelleSalle = new Label("— " + creneau.getSalle());
            libelleSalle.setStyle("-fx-text-fill: -color-fg-muted;");
            ligneEntete.getChildren().add(libelleSalle);
        }

        Region espaceur = new Region();
        HBox.setHgrow(espaceur, Priority.ALWAYS);
        ligneEntete.getChildren().add(espaceur);

        Button boutonEditer = new Button();
        boutonEditer.setGraphic(Icons.crayon());
        boutonEditer.getStyleClass().add(Styles.FLAT);
        boutonEditer.setTooltip(new Tooltip("Modifier ce créneau dans l'emploi du temps"));
        boutonEditer.setAccessibleText("Modifier ce créneau dans l'emploi du temps");
        boutonEditer.setOnAction(e -> {
            if (surDemandeEdition != null) {
                surDemandeEdition.accept(creneau);
            }
        });
        ligneEntete.getChildren().add(boutonEditer);

        if (enCours) {
            Label badge = new Label("EN COURS");
            badge.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: white;"
                    + " -fx-background-radius: 10; -fx-padding: 2 8 2 8; -fx-font-size: 10; -fx-font-weight: bold;");
            ligneEntete.getChildren().add(badge);
        }

        VBox carte = new VBox(10, ligneEntete);
        carte.setPadding(new Insets(14, 16, 16, 16));
        carte.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 8;"
                + " -fx-border-color: " + couleur + "; -fx-border-width: 1.5; -fx-border-radius: 8;");

        if (creneau.getDescription() != null && !creneau.getDescription().isBlank()) {
            Label libelleDescription = new Label(creneau.getDescription());
            libelleDescription.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
            libelleDescription.setWrapText(true);
            carte.getChildren().add(libelleDescription);
        }

        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        if (!fichiers.isEmpty()) {
            carte.getChildren().add(construirePanneauFichiers(fichiers));
        }

        Button boutonOuvrir = new Button("▶ " + (fichiers.isEmpty() ? "Aucun fichier" : "Ouvrir " + fichiers.size()
                + (fichiers.size() > 1 ? " fichiers" : " fichier")));
        boutonOuvrir.getStyleClass().add(Styles.ACCENT);
        boutonOuvrir.setMaxWidth(Double.MAX_VALUE);
        boutonOuvrir.setDisable(fichiers.isEmpty());
        boutonOuvrir.setOnAction(e -> ouvrirFichiersDuCreneau(creneau));
        carte.getChildren().add(boutonOuvrir);

        return carte;
    }

    /**
     * Regroupe les fichiers d'un créneau dans un panneau dépliable, pour qu'un cours avec
     * beaucoup de fichiers ne prenne pas toute la place de la carte : replié par défaut au-delà
     * de {@link #SEUIL_FICHIERS_REPLIES} fichiers, déplié sinon (peu de fichiers = pas besoin
     * de cacher).
     */
    private TitledPane construirePanneauFichiers(List<Fichier> fichiers) {
        VBox contenu = new VBox(4);
        for (Fichier fichier : fichiers) {
            contenu.getChildren().add(construireLigneFichier(fichier));
        }

        TitledPane panneau = new TitledPane();
        panneau.setText("Fichiers (" + fichiers.size() + ")");
        panneau.setContent(contenu);
        panneau.setExpanded(fichiers.size() <= SEUIL_FICHIERS_REPLIES);
        return panneau;
    }

    /**
     * Construit une ligne "vignette/icône + libellé + extension" pour un fichier. La vignette
     * n'est affichée que pour les formats d'image usuels ({@link TypeFichier#estImage}), que
     * JavaFX sait charger nativement (pas de dépendance supplémentaire, contrairement à un
     * aperçu PDF) ; le chargement est fait en arrière-plan pour ne pas geler l'interface sur un
     * gros fichier ou une URL distante lente, et la vignette est simplement absente en cas
     * d'échec (fichier manquant, image corrompue, URL injoignable). Les autres fichiers
     * affichent une petite icône selon leur type (texte, présentation, lien web, générique).
     */
    private HBox construireLigneFichier(Fichier fichier) {
        String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                ? fichier.getNomAffichage() : fichier.getChemin();
        String chemin = fichier.getChemin();

        Label libelleFichier = new Label(libelle);
        HBox.setHgrow(libelleFichier, Priority.ALWAYS);

        HBox ligne = new HBox(8, iconeFichier(chemin), libelleFichier);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(6, 10, 6, 8));
        ligne.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4;");

        String extension = TypeFichier.extension(chemin);
        if (extension != null) {
            Label libelleExtension = new Label(extension);
            libelleExtension.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10;");
            ligne.getChildren().add(libelleExtension);
        }
        return ligne;
    }

    private Node iconeFichier(String chemin) {
        if (chemin == null) {
            return Icons.document();
        }
        if (TypeFichier.estImage(chemin)) {
            Optional<StackPane> vignette = construireVignette(chemin);
            return vignette.isPresent() ? vignette.get() : Icons.document();
        }
        return IconesFichier.parType(chemin);
    }

    private Optional<StackPane> construireVignette(String chemin) {
        String url;
        if (OuvreurFichiers.estUrl(chemin)) {
            url = chemin;
        } else {
            File fichierLocal = new File(chemin);
            if (!fichierLocal.exists()) {
                return Optional.empty();
            }
            url = fichierLocal.toURI().toString();
        }

        Image image = new Image(url, TAILLE_VIGNETTE, TAILLE_VIGNETTE, true, true, true);
        ImageView vue = new ImageView(image);
        vue.setFitWidth(TAILLE_VIGNETTE);
        vue.setFitHeight(TAILLE_VIGNETTE);
        vue.setPreserveRatio(true);
        vue.setSmooth(true);

        StackPane conteneur = new StackPane(vue);
        conteneur.setMinSize(TAILLE_VIGNETTE, TAILLE_VIGNETTE);
        conteneur.setPrefSize(TAILLE_VIGNETTE, TAILLE_VIGNETTE);
        conteneur.setStyle("-fx-background-color: -color-bg-inset; -fx-background-radius: 4;");
        image.errorProperty().addListener((obs, ancien, enErreur) -> {
            if (enErreur) {
                conteneur.setVisible(false);
                conteneur.setManaged(false);
            }
        });
        return Optional.of(conteneur);
    }

    private void animerApparition() {
        FadeTransition fondu = new FadeTransition(Duration.millis(180), listeCartes);
        fondu.setFromValue(0.3);
        fondu.setToValue(1);
        fondu.play();
    }

    private void ouvrirFichiersDuCreneau(Creneau creneau) {
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        if (fichiers.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION, "Aucun fichier sélectionné pour ce créneau.");
            alerte.setTitle("Rien à ouvrir");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }
        List<String> echecs = OuvreurFichiers.ouvrir(fichiers);
        if (echecs.isEmpty()) {
            Toast.montrer(conteneurCentre, fichiers.size() == 1 ? "1 fichier ouvert" : fichiers.size() + " fichiers ouverts");
        } else {
            Alert alerte = new Alert(Alert.AlertType.WARNING,
                    "Certains fichiers n'ont pas pu être ouverts :\n" + String.join("\n", echecs));
            alerte.setTitle("Ouverture partielle");
            alerte.setHeaderText(null);
            alerte.showAndWait();
        }
    }
}
