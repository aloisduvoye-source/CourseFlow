package com.courseflow.app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.persistence.DataStore;
import com.courseflow.ui.AccueilPane;
import com.courseflow.ui.CoursGestionPane;
import com.courseflow.ui.EmploiDuTempsPane;
import com.courseflow.ui.ParametresPane;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private final DataStore dataStore = new DataStore();
    private EmploiDuTemps emploiDuTemps;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        emploiDuTemps = chargerDonnees();
        appliquerTheme(emploiDuTemps.getParametres().isThemeSombre());

        CoursGestionPane vueCours = new CoursGestionPane(emploiDuTemps, this::sauvegarder);
        EmploiDuTempsPane vueEmploiDuTemps = new EmploiDuTempsPane(emploiDuTemps, this::sauvegarder);
        ParametresPane vueParametres = new ParametresPane(emploiDuTemps, dataStore.getFichierDonnees(), this::sauvegarder);

        ToggleGroup groupeNavigation = new ToggleGroup();
        ToggleButton boutonAccueil = new ToggleButton("Accueil");
        boutonAccueil.setToggleGroup(groupeNavigation);
        boutonAccueil.setSelected(true);
        boutonAccueil.setMaxWidth(Double.MAX_VALUE);

        ToggleButton boutonCours = new ToggleButton("Cours");
        boutonCours.setToggleGroup(groupeNavigation);
        boutonCours.setMaxWidth(Double.MAX_VALUE);

        ToggleButton boutonEmploiDuTemps = new ToggleButton("Emploi du temps");
        boutonEmploiDuTemps.setToggleGroup(groupeNavigation);
        boutonEmploiDuTemps.setMaxWidth(Double.MAX_VALUE);

        ToggleButton boutonParametres = new ToggleButton("Paramètres");
        boutonParametres.setToggleGroup(groupeNavigation);
        boutonParametres.setMaxWidth(Double.MAX_VALUE);

        // Le raccourci "Modifier" de l'Accueil bascule sur l'onglet Emploi du temps (comme un
        // vrai clic, via fire()) avant d'ouvrir la boîte de dialogue d'édition du créneau.
        AccueilPane vueAccueil = new AccueilPane(emploiDuTemps, creneau -> {
            boutonEmploiDuTemps.fire();
            vueEmploiDuTemps.ouvrirEdition(creneau);
        });

        vueCours.setVisible(false);
        vueCours.setManaged(false);
        vueEmploiDuTemps.setVisible(false);
        vueEmploiDuTemps.setManaged(false);
        vueParametres.setVisible(false);
        vueParametres.setManaged(false);
        StackPane contenu = new StackPane(vueAccueil, vueCours, vueEmploiDuTemps, vueParametres);

        boutonAccueil.setOnAction(e -> {
            afficherVue(vueAccueil, vueAccueil, vueCours, vueEmploiDuTemps, vueParametres);
            vueAccueil.rafraichir();
        });
        boutonCours.setOnAction(e -> afficherVue(vueCours, vueAccueil, vueCours, vueEmploiDuTemps, vueParametres));
        boutonEmploiDuTemps.setOnAction(e -> {
            afficherVue(vueEmploiDuTemps, vueAccueil, vueCours, vueEmploiDuTemps, vueParametres);
            vueEmploiDuTemps.rafraichir();
            vueEmploiDuTemps.requestFocus();
        });
        boutonParametres.setOnAction(e -> afficherVue(vueParametres, vueAccueil, vueCours, vueEmploiDuTemps, vueParametres));

        HBox enTeteMarque = construireEnTeteMarque();

        Region espaceur = new Region();
        VBox.setVgrow(espaceur, Priority.ALWAYS);

        CheckBox caseThemeSombre = new CheckBox("Thème sombre");
        caseThemeSombre.setSelected(emploiDuTemps.getParametres().isThemeSombre());
        caseThemeSombre.setOnAction(e -> {
            boolean sombre = caseThemeSombre.isSelected();
            emploiDuTemps.getParametres().setThemeSombre(sombre);
            appliquerTheme(sombre);
            sauvegarder();
        });

        VBox barreLaterale = new VBox(4, enTeteMarque, boutonAccueil, boutonCours, boutonEmploiDuTemps, boutonParametres,
                espaceur, caseThemeSombre);
        barreLaterale.setPadding(new Insets(16, 8, 16, 8));
        barreLaterale.setPrefWidth(190);

        BorderPane racine = new BorderPane();
        racine.setLeft(barreLaterale);
        racine.setCenter(contenu);

        scene = new Scene(racine, 1050, 700);
        appliquerTheme(emploiDuTemps.getParametres().isThemeSombre());
        stage.setScene(scene);
        stage.setTitle("CourseFlow");
        chargerIcone(stage);
        stage.setOnCloseRequest(e -> sauvegarder());
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Icone de fenetre / barre des taches. Placeholder pour l'instant : voir
     * {@code packaging/icon/courseflow.svg} et {@code bin/generer-icone}.
     */
    private void chargerIcone(Stage stage) {
        var flux = App.class.getResourceAsStream("icon.png");
        if (flux != null) {
            stage.getIcons().add(new Image(flux));
        }
    }

    /** Logo + nom de l'application en tête de la barre latérale. */
    private HBox construireEnTeteMarque() {
        Label titre = new Label("CourseFlow");
        titre.getStyleClass().add("marque-titre");

        HBox enTete = new HBox(8, titre);
        enTete.setAlignment(Pos.CENTER_LEFT);
        enTete.setPadding(new Insets(4, 4, 12, 4));

        var flux = App.class.getResourceAsStream("icon.png");
        if (flux != null) {
            ImageView logo = new ImageView(new Image(flux, 24, 24, true, true));
            enTete.getChildren().add(0, logo);
        }
        return enTete;
    }

    private void appliquerTheme(boolean sombre) {
        Application.setUserAgentStylesheet(sombre ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
        if (scene != null) {
            String charte = sombre ? "charte-sombre.css" : "charte-claire.css";
            scene.getStylesheets().setAll(App.class.getResource(charte).toExternalForm());
        }
    }

    private void afficherVue(Node aAfficher, Node... vues) {
        for (Node vue : vues) {
            boolean visible = vue == aAfficher;
            vue.setVisible(visible);
            vue.setManaged(visible);
        }
    }

    private EmploiDuTemps chargerDonnees() {
        try {
            return dataStore.charger();
        } catch (IOException e) {
            afficherErreur("Chargement impossible",
                    "Les données n'ont pas pu être chargées depuis " + dataStore.getFichierDonnees()
                            + ".\n" + e.getMessage());
            return new EmploiDuTemps();
        }
    }

    private void sauvegarder() {
        try {
            dataStore.sauvegarder(emploiDuTemps);
        } catch (IOException e) {
            afficherErreur("Sauvegarde impossible",
                    "Les données n'ont pas pu être sauvegardées dans " + dataStore.getFichierDonnees()
                            + ".\n" + e.getMessage());
        }
    }

    private void afficherErreur(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message);
        alerte.setTitle(titre);
        alerte.setHeaderText(titre);
        alerte.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
