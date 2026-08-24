package com.teacherflow.io;

import com.teacherflow.model.Fichier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ouvre des fichiers avec l'application associée du système, via une commande native
 * ({@code xdg-open}/{@code open}/{@code start}) plutôt que {@code java.awt.Desktop} :
 * initialiser AWT dans une appli JavaFX sur Linux charge un toolkit GTK concurrent de celui
 * de JavaFX et fait planter la JVM. Partagé entre l'interface graphique et la commande CLI
 * {@code lecture}.
 */
public final class OuvreurFichiers {

    private OuvreurFichiers() {
    }

    /**
     * Tente d'ouvrir chaque fichier ; n'interrompt pas le traitement des suivants en cas d'échec.
     * Un chemin local manquant est détecté avant de tenter l'ouverture (la commande native
     * comme {@code xdg-open} ne renvoie pas d'erreur exploitable pour un fichier introuvable :
     * son propre sous-processus échoue silencieusement de notre point de vue).
     * @return un message clair par fichier qui n'a pas pu être ouvert (libellé + raison).
     */
    public static List<String> ouvrir(List<Fichier> fichiers) {
        List<String> echecs = new ArrayList<>();
        for (Fichier fichier : fichiers) {
            String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                    ? fichier.getNomAffichage() : fichier.getChemin();
            String chemin = fichier.getChemin();

            if (chemin == null || chemin.isBlank()) {
                echecs.add(libelle + " (chemin invalide)");
                continue;
            }
            if (!estUrl(chemin) && !new File(chemin).exists()) {
                echecs.add(libelle + " (fichier introuvable : " + chemin + ")");
                continue;
            }
            try {
                new ProcessBuilder(commandeOuverture(chemin)).start();
            } catch (IOException e) {
                echecs.add(libelle + " (" + e.getMessage() + ")");
            }
        }
        return echecs;
    }

    /**
     * @return true si le chemin est une URL (ex. {@code https://...}) plutôt qu'un chemin de
     * fichier local — auquel cas son existence sur le disque n'a pas à être vérifiée.
     */
    public static boolean estUrl(String chemin) {
        return chemin != null && chemin.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.+");
    }

    private static List<String> commandeOuverture(String chemin) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return List.of("cmd", "/c", "start", "\"\"", chemin);
        }
        if (os.contains("mac")) {
            return List.of("open", chemin);
        }
        return List.of("xdg-open", chemin);
    }
}
