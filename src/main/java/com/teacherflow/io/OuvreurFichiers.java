package com.teacherflow.io;

import com.teacherflow.model.Fichier;

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
     * @return les libellés des fichiers qui n'ont pas pu être ouverts.
     */
    public static List<String> ouvrir(List<Fichier> fichiers) {
        List<String> echecs = new ArrayList<>();
        for (Fichier fichier : fichiers) {
            try {
                new ProcessBuilder(commandeOuverture(fichier.getChemin())).start();
            } catch (IOException e) {
                echecs.add(fichier.getNomAffichage() != null ? fichier.getNomAffichage() : fichier.getChemin());
            }
        }
        return echecs;
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
