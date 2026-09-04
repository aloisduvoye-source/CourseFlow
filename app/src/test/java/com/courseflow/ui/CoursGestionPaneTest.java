package com.courseflow.ui;

import com.courseflow.model.Cours;
import com.courseflow.model.EmploiDuTemps;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pilote {@link CoursGestionPane} comme un utilisateur (clics, saisie clavier) plutôt que
 * d'appeler ses méthodes directement : c'est le seul moyen de vérifier que le bouton, la liste et
 * le champ de nom sont réellement câblés ensemble. Premier test automatisé sur {@code app}
 * (voir doc/BILAN_ET_PISTES.md §8, "0 test sur app").
 */
class CoursGestionPaneTest extends ApplicationTest {

    private EmploiDuTemps emploiDuTemps;
    private AtomicInteger nombreSauvegardes;

    @Override
    public void start(Stage stage) {
        emploiDuTemps = new EmploiDuTemps();
        nombreSauvegardes = new AtomicInteger();
        CoursGestionPane pane = new CoursGestionPane(emploiDuTemps, nombreSauvegardes::incrementAndGet);
        stage.setScene(new Scene(pane, 900, 600));
        stage.show();
    }

    @Test
    void creerUnCoursLAjouteAuModeleEtLeSelectionne() {
        clickOn("Nouveau cours");

        assertEquals(1, emploiDuTemps.getCours().size());
        assertEquals("Nouveau cours", emploiDuTemps.getCours().get(0).getNom());
        assertTrue(nombreSauvegardes.get() > 0, "la création doit déclencher une sauvegarde");
    }

    @Test
    void renommerLeCoursSelectionneMetAJourLeModeleEnDirect() {
        clickOn("Nouveau cours");

        // Le champ de nom reçoit le focus avec le texte par défaut sélectionné (voir
        // CoursGestionPane#creerCours) : taper remplace la sélection, comme un vrai renommage.
        write("6e A - Mathématiques");
        push(KeyCode.ENTER);

        Cours cours = emploiDuTemps.getCours().get(0);
        assertEquals("6e A - Mathématiques", cours.getNom());
    }

    @Test
    void creerDeuxCoursLesAjouteTousLesDeuxAuModele() {
        clickOn("Nouveau cours");
        write("Premier cours");
        clickOn("Nouveau cours");
        write("Second cours");

        assertEquals(2, emploiDuTemps.getCours().size());
        assertTrue(emploiDuTemps.getCours().stream().anyMatch(c -> "Premier cours".equals(c.getNom())));
        assertTrue(emploiDuTemps.getCours().stream().anyMatch(c -> "Second cours".equals(c.getNom())));
    }
}
