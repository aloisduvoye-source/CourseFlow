package com.teacherflow.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmploiDuTempsTest {

    @Test
    void unCreneauNeResoutQueLesFichiersSelectionnesParmiCeuxDuCours() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier cours = maths.ajouterFichier("/docs/maths/cours-fractions.pdf", "Cours fractions");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices-fractions.pdf", "Exercices");
        maths.ajouterFichier("/docs/maths/corrige-ds1.pdf", "Corrigé DS1");

        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), maths.getId());
        creneau.selectionnerFichier(cours.getId());
        creneau.selectionnerFichier(exercices.getId());

        List<Fichier> fichiersDuCreneau = emploiDuTemps.fichiersPourCreneau(creneau);

        assertEquals(2, fichiersDuCreneau.size());
        assertTrue(fichiersDuCreneau.contains(cours));
        assertTrue(fichiersDuCreneau.contains(exercices));
    }

    @Test
    void trouverCreneauCourantRepereLeCreneauActifAJourEtHeureDonnes() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours histoire = emploiDuTemps.ajouterCours("3e B - Histoire", "#e67e22");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), histoire.getId());

        assertTrue(emploiDuTemps.trouverCreneauCourant(DayOfWeek.TUESDAY, LocalTime.of(10, 30))
                .filter(c -> c.getId().equals(creneau.getId()))
                .isPresent());

        assertTrue(emploiDuTemps.trouverCreneauCourant(DayOfWeek.TUESDAY, LocalTime.of(11, 0)).isEmpty());
        assertTrue(emploiDuTemps.trouverCreneauCourant(DayOfWeek.WEDNESDAY, LocalTime.of(10, 30)).isEmpty());
    }

    @Test
    void supprimerUnCoursSupprimeAussiSesCreneaux() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), maths.getId());

        emploiDuTemps.supprimerCours(maths.getId());

        assertTrue(emploiDuTemps.trouverCours(maths.getId()).isEmpty());
        assertTrue(emploiDuTemps.trouverCreneau(creneau.getId()).isEmpty());
    }
}
