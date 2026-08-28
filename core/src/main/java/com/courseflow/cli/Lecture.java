package com.courseflow.cli;

import com.courseflow.model.EmploiDuTemps;
import com.courseflow.persistence.DataStore;
import com.courseflow.persistence.DonneesIllisiblesException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * Point d'entrée CLI headless (sans interface graphique) : ouvre ou consulte les fichiers et
 * informations liés aux créneaux/cours de l'emploi du temps, via une sous-commande optionnelle
 * ({@code slot}, {@code slots}, {@code schedule}, {@code courses}, {@code course},
 * {@code open-file}, {@code week}). Sans sous-commande, ouvre les fichiers du créneau ciblé
 * (courant, ou via {@code --next}/{@code --previous}/{@code --day}/{@code --date}+{@code --time}).
 *
 * <p>Cette classe se limite à l'analyse des arguments, au chargement des données et à l'aiguillage
 * vers la classe qui traite chaque sous-commande. {@code lecture .} lance l'interface graphique :
 * interceptée par {@code bin/lecture} en mode développement, ou via {@link LanceurGraphiqueVoisin}
 * pour le binaire packagé.
 */
public final class Lecture {

    private Lecture() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals(".")) {
            LanceurGraphiqueVoisin.lancer();
            return;
        }
        if (Arrays.asList(args).contains("--help")) {
            AideLecture.afficher();
            return;
        }

        ArgumentsLecture arguments;
        try {
            arguments = ArgumentsLecture.analyser(args, LocalDate.now(), LocalTime.now());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage : lecture [COMMAND] [OPTIONS] (voir lecture --help)");
            System.exit(2);
            return;
        }

        DataStore dataStore = new DataStore();
        EmploiDuTemps emploiDuTemps = charger(dataStore);

        switch (arguments.getCommande()) {
            case OUVRIR -> CommandeOuverture.ouvrirCreneauCible(emploiDuTemps, arguments);
            case SLOT -> CommandesConsultation.slot(emploiDuTemps, arguments);
            case SLOTS -> CommandesConsultation.slotsDuJour(emploiDuTemps, arguments.getDate());
            case SCHEDULE -> System.out.print(GrilleAscii.construire(emploiDuTemps, LocalDate.now()));
            case COURSES -> CommandesConsultation.listerCours(emploiDuTemps, arguments.isMissingInfo());
            case COURSE -> CommandesConsultation.afficherCours(emploiDuTemps, arguments.getNomCours());
            case OPEN_FILE -> CommandeOuverture.ouvrirFichierCible(emploiDuTemps, arguments);
            case WEEK -> CommandeSemaine.traiter(dataStore, emploiDuTemps, arguments);
        }
    }

    private static EmploiDuTemps charger(DataStore dataStore) {
        try {
            return dataStore.charger();
        } catch (DonneesIllisiblesException e) {
            System.err.println("Fichier de données illisible : il a été mis de côté sous "
                    + e.getFichierQuarantaine() + ".");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Impossible de charger les données depuis " + dataStore.getFichierDonnees()
                    + " : " + e.getMessage());
            System.exit(1);
        }
        throw new AssertionError("inatteignable : System.exit a déjà interrompu le programme");
    }
}
