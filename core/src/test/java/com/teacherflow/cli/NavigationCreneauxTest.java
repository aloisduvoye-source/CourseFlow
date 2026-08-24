package com.teacherflow.cli;

import com.teacherflow.model.Creneau;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationCreneauxTest {

    private static Creneau creneau(DayOfWeek jour, int heure) {
        return new Creneau(jour, LocalTime.of(heure, 0), LocalTime.of(heure + 1, 0), UUID.randomUUID());
    }

    @Test
    void suivantTrouveLeProchainCreneauDeLaSemaine() {
        Creneau lundi = creneau(DayOfWeek.MONDAY, 8);
        Creneau mercredi = creneau(DayOfWeek.WEDNESDAY, 10);
        List<Creneau> creneaux = List.of(lundi, mercredi);

        Optional<Creneau> resultat = NavigationCreneaux.suivant(creneaux, DayOfWeek.MONDAY, LocalTime.of(9, 0));

        assertEquals(mercredi, resultat.orElseThrow());
    }

    @Test
    void suivantBoucleSurLePremierCreneauApresLeDimanche() {
        Creneau lundi = creneau(DayOfWeek.MONDAY, 8);
        Creneau mercredi = creneau(DayOfWeek.WEDNESDAY, 10);
        List<Creneau> creneaux = List.of(lundi, mercredi);

        Optional<Creneau> resultat = NavigationCreneaux.suivant(creneaux, DayOfWeek.SUNDAY, LocalTime.of(23, 0));

        assertEquals(lundi, resultat.orElseThrow());
    }

    @Test
    void precedentTrouveLeCreneauAvant() {
        Creneau lundi = creneau(DayOfWeek.MONDAY, 8);
        Creneau mercredi = creneau(DayOfWeek.WEDNESDAY, 10);
        List<Creneau> creneaux = List.of(lundi, mercredi);

        Optional<Creneau> resultat = NavigationCreneaux.precedent(creneaux, DayOfWeek.WEDNESDAY, LocalTime.of(11, 0));

        assertEquals(mercredi, resultat.orElseThrow());
    }

    @Test
    void precedentBoucleSurLeDernierCreneauAvantLeLundi() {
        Creneau lundi = creneau(DayOfWeek.MONDAY, 8);
        Creneau mercredi = creneau(DayOfWeek.WEDNESDAY, 10);
        List<Creneau> creneaux = List.of(lundi, mercredi);

        Optional<Creneau> resultat = NavigationCreneaux.precedent(creneaux, DayOfWeek.MONDAY, LocalTime.of(7, 0));

        assertEquals(mercredi, resultat.orElseThrow());
    }

    @Test
    void uneReferenceExactementSurUnCreneauNeRenvoiePasCeCreneauCommeSuivant() {
        Creneau lundi = creneau(DayOfWeek.MONDAY, 8);
        Creneau mercredi = creneau(DayOfWeek.WEDNESDAY, 10);
        List<Creneau> creneaux = List.of(lundi, mercredi);

        Optional<Creneau> resultat = NavigationCreneaux.suivant(creneaux, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(mercredi, resultat.orElseThrow());
    }

    @Test
    void listeVideNeRenvoieRien() {
        assertTrue(NavigationCreneaux.suivant(List.of(), DayOfWeek.MONDAY, LocalTime.of(8, 0)).isEmpty());
        assertTrue(NavigationCreneaux.precedent(List.of(), DayOfWeek.MONDAY, LocalTime.of(8, 0)).isEmpty());
    }
}
