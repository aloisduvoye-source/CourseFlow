package com.courseflow.cli;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.TypeSemaine;
import com.courseflow.persistence.DataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Génère un jeu de données aléatoire (cours, fichiers, créneaux) dans {@code ~/.courseflow/data.json},
 * pour explorer l'interface sans tout créer manuellement. Les fichiers factices sont créés dans le
 * dossier {@code test/} du projet. Les données existantes sont sauvegardées en {@code .bak} avant
 * d'être remplacées. La date d'ancrage de la semaine A est réglée sur le lundi de la semaine
 * courante, et une partie des créneaux générés sont réglés sur semaine A ou B (dont quelques
 * paires au même horaire) pour illustrer l'alternance.
 */
public final class GenererDonneesTest {

    private static final Random ALEA = new Random();

    private static final String[] NIVEAUX = {
            "6e A", "6e B", "5e A", "5e B", "4e A", "3e A", "Terminale"
    };
    private static final String[] MATIERES = {
            "Mathématiques", "Français", "Histoire-Géographie", "Physique-Chimie",
            "SVT", "Anglais", "Technologie", "Arts Plastiques"
    };
    private static final String[] NOMS_FICHIERS = {
            "Cours", "Exercices", "Correction", "Controle", "Diaporama",
            "Fiche_methode", "TP", "DM", "Evaluation", "Support_eleve"
    };
    private static final DayOfWeek[] JOURS_ECOLE = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    };
    private static final int[] HEURES_DEBUT_POSSIBLES = {8, 9, 10, 11, 14, 15, 16};

    private GenererDonneesTest() {
    }

    public static void main(String[] args) throws IOException {
        List<Path> fichiersTest = genererFichiersTest(Path.of("test"));

        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        emploiDuTemps.getParametres().setAncrageSemaineA(LocalDate.now().with(DayOfWeek.MONDAY));
        List<Cours> coursGeneres = genererCours(emploiDuTemps, fichiersTest);
        genererCreneaux(emploiDuTemps, coursGeneres);

        DataStore dataStore = new DataStore();
        Path fichierDonnees = dataStore.getFichierDonnees();
        if (Files.exists(fichierDonnees)) {
            Path sauvegarde = Path.of(fichierDonnees + ".bak");
            Files.copy(fichierDonnees, sauvegarde, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Données existantes sauvegardées dans " + sauvegarde);
        }
        dataStore.sauvegarder(emploiDuTemps);

        System.out.println("Généré " + coursGeneres.size() + " cours et " + emploiDuTemps.getCreneaux().size()
                + " créneaux dans " + fichierDonnees);
    }

    private static List<Path> genererFichiersTest(Path dossier) throws IOException {
        Files.createDirectories(dossier);
        List<Path> fichiers = new ArrayList<>();
        for (String nom : NOMS_FICHIERS) {
            Path fichier = dossier.resolve(nom + ".txt");
            if (!Files.exists(fichier)) {
                Files.writeString(fichier, "Fichier de test CourseFlow : " + nom + "\n");
            }
            fichiers.add(fichier);
        }
        return fichiers;
    }

    private static List<Cours> genererCours(EmploiDuTemps emploiDuTemps, List<Path> fichiersTest) {
        List<Cours> coursGeneres = new ArrayList<>();
        for (String niveau : NIVEAUX) {
            String matiere = MATIERES[ALEA.nextInt(MATIERES.length)];
            Cours cours = emploiDuTemps.ajouterCours(niveau + " - " + matiere, couleurAleatoire());

            List<Path> melange = new ArrayList<>(fichiersTest);
            Collections.shuffle(melange, ALEA);
            int nombreFichiers = 2 + ALEA.nextInt(3);
            for (int f = 0; f < nombreFichiers; f++) {
                Path fichier = melange.get(f);
                cours.ajouterFichier(fichier.toAbsolutePath().toString(), fichier.getFileName().toString());
            }
            coursGeneres.add(cours);
        }
        return coursGeneres;
    }

    private static void genererCreneaux(EmploiDuTemps emploiDuTemps, List<Cours> cours) {
        boolean[][] occupe = new boolean[JOURS_ECOLE.length][24];

        genererPairesAlternees(emploiDuTemps, cours, occupe);

        int nombreCreneaux = 18;
        int tentatives = 0;
        int crees = 0;
        while (crees < nombreCreneaux && tentatives < 200) {
            tentatives++;
            int jourIndex = ALEA.nextInt(JOURS_ECOLE.length);
            int heure = HEURES_DEBUT_POSSIBLES[ALEA.nextInt(HEURES_DEBUT_POSSIBLES.length)];
            if (occupe[jourIndex][heure]) {
                continue;
            }
            occupe[jourIndex][heure] = true;

            Cours coursChoisi = cours.get(ALEA.nextInt(cours.size()));
            Creneau creneau = emploiDuTemps.ajouterCreneau(
                    JOURS_ECOLE[jourIndex], LocalTime.of(heure, 0), LocalTime.of(heure + 1, 0), coursChoisi.getId());
            creneau.setTypeSemaine(typeSemaineAleatoire());
            selectionnerFichiersAleatoires(creneau, coursChoisi);
            crees++;
        }
    }

    /**
     * Crée quelques paires de créneaux au même horaire (un cours en semaine A, un autre en
     * semaine B) pour illustrer une vraie alternance, plutôt que de compter uniquement sur le
     * tirage aléatoire de {@link #typeSemaineAleatoire()} qui ne garantit pas ce cas de figure.
     */
    private static void genererPairesAlternees(EmploiDuTemps emploiDuTemps, List<Cours> cours, boolean[][] occupe) {
        int nombrePaires = 3;
        int tentatives = 0;
        int crees = 0;
        while (crees < nombrePaires && tentatives < 100) {
            tentatives++;
            int jourIndex = ALEA.nextInt(JOURS_ECOLE.length);
            int heure = HEURES_DEBUT_POSSIBLES[ALEA.nextInt(HEURES_DEBUT_POSSIBLES.length)];
            if (occupe[jourIndex][heure]) {
                continue;
            }
            occupe[jourIndex][heure] = true;

            Cours coursA = cours.get(ALEA.nextInt(cours.size()));
            Cours coursB = cours.get(ALEA.nextInt(cours.size()));

            Creneau creneauA = emploiDuTemps.ajouterCreneau(
                    JOURS_ECOLE[jourIndex], LocalTime.of(heure, 0), LocalTime.of(heure + 1, 0), coursA.getId());
            creneauA.setTypeSemaine(TypeSemaine.A);
            selectionnerFichiersAleatoires(creneauA, coursA);

            Creneau creneauB = emploiDuTemps.ajouterCreneau(
                    JOURS_ECOLE[jourIndex], LocalTime.of(heure, 0), LocalTime.of(heure + 1, 0), coursB.getId());
            creneauB.setTypeSemaine(TypeSemaine.B);
            selectionnerFichiersAleatoires(creneauB, coursB);

            crees++;
        }
    }

    private static void selectionnerFichiersAleatoires(Creneau creneau, Cours cours) {
        List<Fichier> fichiersCours = cours.getFichiers();
        List<Fichier> melange = new ArrayList<>(fichiersCours);
        Collections.shuffle(melange, ALEA);
        int nombreSelectionnes = 1 + ALEA.nextInt(fichiersCours.size());
        List<UUID> idsSelectionnes = new ArrayList<>();
        for (int k = 0; k < nombreSelectionnes; k++) {
            idsSelectionnes.add(melange.get(k).getId());
        }
        creneau.setFichiersSelectionnesIds(idsSelectionnes);
    }

    /**
     * @return TOUTES la majorité du temps (comportement historique), sinon A ou B à parts
     * égales — pour que la fonctionnalité de semaines alternées soit visible dans les données
     * générées sans dominer l'emploi du temps.
     */
    private static TypeSemaine typeSemaineAleatoire() {
        int tirage = ALEA.nextInt(100);
        if (tirage < 60) {
            return TypeSemaine.TOUTES;
        }
        return tirage < 80 ? TypeSemaine.A : TypeSemaine.B;
    }

    private static String couleurAleatoire() {
        float teinte = ALEA.nextFloat() * 360f;
        float saturation = 0.55f;
        float luminosite = 0.85f;

        float c = luminosite * saturation;
        float x = c * (1 - Math.abs((teinte / 60f) % 2 - 1));
        float m = luminosite - c;
        float r;
        float g;
        float b;
        if (teinte < 60) {
            r = c; g = x; b = 0;
        } else if (teinte < 120) {
            r = x; g = c; b = 0;
        } else if (teinte < 180) {
            r = 0; g = c; b = x;
        } else if (teinte < 240) {
            r = 0; g = x; b = c;
        } else if (teinte < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        int rouge = Math.round((r + m) * 255);
        int vert = Math.round((g + m) * 255);
        int bleu = Math.round((b + m) * 255);
        return String.format("#%02X%02X%02X", rouge, vert, bleu);
    }
}
