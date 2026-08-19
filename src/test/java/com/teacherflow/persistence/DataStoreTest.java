package com.teacherflow.persistence;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.PlageHoraire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStoreTest {

    @Test
    void chargerSansFichierExistantRenvoieUnEmploiDuTempsVide(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("data.json"));

        EmploiDuTemps emploiDuTemps = dataStore.charger();

        assertTrue(emploiDuTemps.getCours().isEmpty());
        assertTrue(emploiDuTemps.getCreneaux().isEmpty());
    }

    @Test
    void sauvegarderPuisRechargerRestitueLesMemesDonnees(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("sous-dossier").resolve("data.json"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices-fractions.pdf", "Exercices");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), maths.getId());
        creneau.selectionnerFichier(exercices.getId());

        dataStore.sauvegarder(emploiDuTemps);
        EmploiDuTemps recharge = dataStore.charger();

        assertEquals(1, recharge.getCours().size());
        assertEquals("6e A - Mathématiques", recharge.getCours().get(0).getNom());

        Creneau creneauRecharge = recharge.getCreneaux().get(0);
        List<Fichier> fichiersResolus = recharge.fichiersPourCreneau(creneauRecharge);
        assertEquals(1, fichiersResolus.size());
        assertEquals("/docs/maths/exercices-fractions.pdf", fichiersResolus.get(0).getChemin());
    }

    @Test
    void sauvegarderPuisRechargerRestitueLesParametres(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("data.json"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        emploiDuTemps.getParametres().setJoursAffiches(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        emploiDuTemps.getParametres().setPasMinutes(15);
        emploiDuTemps.getParametres().getPlagesParJour().put(DayOfWeek.MONDAY, List.of(
                new PlageHoraire(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new PlageHoraire(LocalTime.of(13, 30), LocalTime.of(17, 0))));

        dataStore.sauvegarder(emploiDuTemps);
        EmploiDuTemps recharge = dataStore.charger();

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), recharge.getParametres().getJoursAffiches());
        assertEquals(15, recharge.getParametres().getPasMinutes());
        assertEquals(2, recharge.getParametres().plagesPour(DayOfWeek.MONDAY).size());
        assertEquals(LocalTime.of(13, 30), recharge.getParametres().plagesPour(DayOfWeek.MONDAY).get(1).getDebut());
    }
}
