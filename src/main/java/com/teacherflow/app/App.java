package com.teacherflow.app;

import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.persistence.DataStore;
import com.teacherflow.ui.CoursGestionPane;
import com.teacherflow.ui.EmploiDuTempsPane;
import com.teacherflow.ui.ParametresPane;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private final DataStore dataStore = new DataStore();
    private EmploiDuTemps emploiDuTemps;

    @Override
    public void start(Stage stage) {
        emploiDuTemps = chargerDonnees();

        CoursGestionPane vueCours = new CoursGestionPane(emploiDuTemps, this::sauvegarder);
        EmploiDuTempsPane vueEmploiDuTemps = new EmploiDuTempsPane(emploiDuTemps, this::sauvegarder);
        ParametresPane vueParametres = new ParametresPane(emploiDuTemps, this::sauvegarder);

        vueEmploiDuTemps.setVisible(false);
        vueEmploiDuTemps.setManaged(false);
        vueParametres.setVisible(false);
        vueParametres.setManaged(false);
        StackPane contenu = new StackPane(vueCours, vueEmploiDuTemps, vueParametres);

        ToggleGroup groupeNavigation = new ToggleGroup();
        ToggleButton boutonCours = new ToggleButton("Cours");
        boutonCours.setToggleGroup(groupeNavigation);
        boutonCours.setSelected(true);
        boutonCours.setMaxWidth(Double.MAX_VALUE);

        ToggleButton boutonEmploiDuTemps = new ToggleButton("Emploi du temps");
        boutonEmploiDuTemps.setToggleGroup(groupeNavigation);
        boutonEmploiDuTemps.setMaxWidth(Double.MAX_VALUE);

        ToggleButton boutonParametres = new ToggleButton("Paramètres");
        boutonParametres.setToggleGroup(groupeNavigation);
        boutonParametres.setMaxWidth(Double.MAX_VALUE);

        boutonCours.setOnAction(e -> afficherVue(vueCours, vueCours, vueEmploiDuTemps, vueParametres));
        boutonEmploiDuTemps.setOnAction(e -> {
            afficherVue(vueEmploiDuTemps, vueCours, vueEmploiDuTemps, vueParametres);
            vueEmploiDuTemps.rafraichir();
        });
        boutonParametres.setOnAction(e -> afficherVue(vueParametres, vueCours, vueEmploiDuTemps, vueParametres));

        Label titreApp = new Label("TeacherFlow");

        Region espaceur = new Region();
        VBox.setVgrow(espaceur, Priority.ALWAYS);

        VBox barreLaterale = new VBox(4, titreApp, boutonCours, boutonEmploiDuTemps, boutonParametres);
        barreLaterale.setPadding(new Insets(16, 8, 16, 8));
        barreLaterale.setPrefWidth(190);

        BorderPane racine = new BorderPane();
        racine.setLeft(barreLaterale);
        racine.setCenter(contenu);

        Scene scene = new Scene(racine, 1050, 700);
        stage.setScene(scene);
        stage.setTitle("TeacherFlow");
        stage.setOnCloseRequest(e -> sauvegarder());
        stage.setMaximized(true);
        stage.show();
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
