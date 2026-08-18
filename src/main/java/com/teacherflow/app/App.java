package com.teacherflow.app;

import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.persistence.DataStore;
import com.teacherflow.ui.CoursGestionPane;
import com.teacherflow.ui.EmploiDuTempsPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private final DataStore dataStore = new DataStore();
    private EmploiDuTemps emploiDuTemps;

    @Override
    public void start(Stage stage) {
        emploiDuTemps = chargerDonnees();

        CoursGestionPane ongletCours = new CoursGestionPane(emploiDuTemps, this::sauvegarder);
        EmploiDuTempsPane ongletEmploiDuTemps = new EmploiDuTempsPane(emploiDuTemps, this::sauvegarder);

        Tab tabCours = new Tab("Cours", ongletCours);
        tabCours.setClosable(false);
        Tab tabEmploiDuTemps = new Tab("Emploi du temps", ongletEmploiDuTemps);
        tabEmploiDuTemps.setClosable(false);

        TabPane onglets = new TabPane(tabCours, tabEmploiDuTemps);
        onglets.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == tabEmploiDuTemps) {
                ongletEmploiDuTemps.rafraichir();
            }
        });

        stage.setScene(new Scene(onglets, 1000, 700));
        stage.setTitle("TeacherFlow");
        stage.setOnCloseRequest(e -> sauvegarder());
        stage.show();
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
