package com.courseflow.ui;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pilote {@link EmploiDuTempsPane} comme un utilisateur : clic sur une case libre de la grille
 * pour créer un créneau, puis annulation/rétablissement au clavier. Parcours critique choisi en
 * priorité (voir doc/BILAN_ET_PISTES.md §8) car undo/redo n'a jusqu'ici jamais été vérifié que
 * manuellement.
 */
class EmploiDuTempsPaneTest extends ApplicationTest {

    private EmploiDuTemps emploiDuTemps;
    private EmploiDuTempsPane pane;
    private Cours cours;

    @Override
    public void start(Stage stage) {
        emploiDuTemps = new EmploiDuTemps();
        cours = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        pane = new EmploiDuTempsPane(emploiDuTemps, () -> { });
        stage.setScene(new Scene(pane, 1000, 700));
        stage.show();
    }

    @Test
    void cliquerUneCaseLibreOuvreLeDialogueEtValiderCreeLeCreneau() {
        cliquerLaPremiereCaseLibre();
        clickOn("Valider");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, emploiDuTemps.getCreneaux().size());
        Creneau creneau = emploiDuTemps.getCreneaux().get(0);
        // La première case cliquable de la grille correspond au premier jour affiché
        // (lundi, par défaut) et au premier bloc horaire par défaut (8h-9h) : voir
        // Parametres#blocsParDefaut et EmploiDuTempsPane#ajouterCellulesCliquables.
        assertEquals(DayOfWeek.MONDAY, creneau.getJour());
        assertEquals(LocalTime.of(8, 0), creneau.getHeureDebut());
        assertEquals(LocalTime.of(9, 0), creneau.getHeureFin());
        assertEquals(cours.getId(), creneau.getCoursId());
    }

    @Test
    void ctrlZAnnuleLaCreationPuisCtrlMajZLaRetablit() {
        cliquerLaPremiereCaseLibre();
        clickOn("Valider");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, emploiDuTemps.getCreneaux().size(), "précondition : le créneau a bien été créé");

        interact(() -> pane.requestFocus());
        push(KeyCode.CONTROL, KeyCode.Z);
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(emploiDuTemps.getCreneaux().isEmpty(), "Ctrl+Z doit annuler la création");

        push(KeyCode.CONTROL, KeyCode.SHIFT, KeyCode.Z);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, emploiDuTemps.getCreneaux().size(), "Ctrl+Maj+Z doit rétablir le créneau annulé");
    }

    /** Clique sur la case cliquable correspondant à lundi 8h-9h (la première dans l'ordre de construction). */
    private void cliquerLaPremiereCaseLibre() {
        WaitForAsyncUtils.waitForFxEvents();
        List<Region> cellules = casesCliquables(pane);
        assertTrue(!cellules.isEmpty(), "la grille doit exposer au moins une case cliquable");
        clickOn(cellules.get(0));
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Retrouve les cases vides cliquables de la grille par leur seul signal distinctif visible
     * depuis l'extérieur du pane (curseur "main", type {@link Region} exact) : elles n'ont ni id
     * ni classe dédiée, seul le code de {@code EmploiDuTempsPane#ajouterCellulesCliquables} sait
     * qu'elles existent. Ordre de parcours = ordre de construction (jour par jour, bloc par bloc).
     */
    private static List<Region> casesCliquables(Parent racine) {
        List<Region> resultat = new ArrayList<>();
        collecter(racine, resultat);
        return resultat;
    }

    private static void collecter(Parent parent, List<Region> resultat) {
        for (Node enfant : parent.getChildrenUnmodifiable()) {
            if (enfant.getClass() == Region.class && enfant.getCursor() == Cursor.HAND) {
                resultat.add((Region) enfant);
            }
            if (enfant instanceof Parent sousParent) {
                collecter(sousParent, resultat);
            }
        }
    }
}
