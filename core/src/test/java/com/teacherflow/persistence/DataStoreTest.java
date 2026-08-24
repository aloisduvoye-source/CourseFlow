package com.teacherflow.persistence;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.DossierReference;
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
        exercices.setTags(List.of("à corriger", "brouillon"));
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
        assertEquals(List.of("à corriger", "brouillon"), fichiersResolus.get(0).getTags());
    }

    @Test
    void sauvegarderPuisRechargerRestitueLesParametres(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("data.json"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        emploiDuTemps.getParametres().setJoursAffiches(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        emploiDuTemps.getParametres().setPasMinutes(15);
        emploiDuTemps.getParametres().setHeureDebutGrille(LocalTime.of(8, 30));
        emploiDuTemps.getParametres().setHeureFinGrille(LocalTime.of(18, 30));
        emploiDuTemps.getParametres().setBlocs(List.of(
                new PlageHoraire(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new PlageHoraire(LocalTime.of(10, 20), LocalTime.of(11, 20))));
        emploiDuTemps.getParametres().setTagsDisponibles(List.of("dm", "td", "correction", "cm", "projet"));
        emploiDuTemps.getParametres().setThemeSombre(true);

        dataStore.sauvegarder(emploiDuTemps);
        EmploiDuTemps recharge = dataStore.charger();

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), recharge.getParametres().getJoursAffiches());
        assertEquals(15, recharge.getParametres().getPasMinutes());
        assertEquals(LocalTime.of(8, 30), recharge.getParametres().getHeureDebutGrille());
        assertEquals(LocalTime.of(18, 30), recharge.getParametres().getHeureFinGrille());
        assertEquals(2, recharge.getParametres().getBlocs().size());
        assertEquals(LocalTime.of(10, 20), recharge.getParametres().getBlocs().get(1).getDebut());
        assertEquals(List.of("dm", "td", "correction", "cm", "projet"), recharge.getParametres().getTagsDisponibles());
        assertTrue(recharge.getParametres().isThemeSombre());
    }

    @Test
    void sauvegarderPuisRechargerRestitueLeCoursParDefautEtLesFichiersLies(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("data.json"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices.pdf", "Exercices");
        emploiDuTemps.getParametres().setCoursDefautId(maths.getId());

        Cours soutien = emploiDuTemps.ajouterCours("Soutien scolaire", "#e67e22");
        soutien.ajouterFichierLie(exercices.getId());

        dataStore.sauvegarder(emploiDuTemps);
        EmploiDuTemps recharge = dataStore.charger();

        assertEquals(maths.getId(), recharge.getParametres().getCoursDefautId());
        Cours soutienRecharge = recharge.getCours().stream()
                .filter(c -> c.getNom().equals("Soutien scolaire"))
                .findFirst().orElseThrow();
        assertEquals(List.of(exercices.getId()), soutienRecharge.getFichiersLies());
        assertEquals(1, recharge.fichiersVisibles(soutienRecharge).size());
    }

    @Test
    void sauvegarderPuisRechargerRestitueLesDossiersReferences(@TempDir Path repertoireTemp) throws IOException {
        DataStore dataStore = new DataStore(repertoireTemp.resolve("data.json"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        DossierReference reference = new DossierReference("/docs/maths");
        reference.getFichiersImportes().add("/docs/maths/exercices.pdf");
        maths.getDossiersReferences().add(reference);

        dataStore.sauvegarder(emploiDuTemps);
        EmploiDuTemps recharge = dataStore.charger();

        DossierReference referenceRechargee = recharge.getCours().get(0).getDossiersReferences().get(0);
        assertEquals("/docs/maths", referenceRechargee.getChemin());
        assertEquals(List.of("/docs/maths/exercices.pdf"), referenceRechargee.getFichiersImportes());
    }
}
