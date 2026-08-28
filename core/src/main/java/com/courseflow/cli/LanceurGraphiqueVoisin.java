package com.courseflow.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lancement de l'interface graphique depuis la commande {@code lecture .} pour le binaire
 * packagé. En mode développement, {@code bin/lecture} intercepte "." avant d'invoquer Java, donc
 * ce code n'est jamais exercé dans ce mode.
 */
final class LanceurGraphiqueVoisin {

    private LanceurGraphiqueVoisin() {
    }

    /**
     * Démarre l'exécutable graphique voisin ({@code courseflow}), présent dans le même dossier
     * que le binaire natif {@code lecture} produit par jpackage (les deux lanceurs d'une même
     * image jpackage vivent toujours côte à côte, voir {@code bin/build-installer}). Lancé via
     * {@code setsid} pour détacher le processus graphique de la session courante : sans ça, il
     * peut être arrêté avec le reste du groupe de processus quand {@code lecture} se termine,
     * avant même d'avoir eu le temps de s'afficher. Le lanceur natif {@code lecture} tourne avec
     * {@code _JPACKAGE_LAUNCHER} et {@code LD_LIBRARY_PATH} réglés pour SON PROPRE lanceur ;
     * hérités tels quels par l'enfant, ils empêchent le lanceur {@code courseflow} de démarrer
     * correctement (il tente de réutiliser la configuration de {@code lecture}) — on les retire
     * donc avant de le démarrer.
     */
    static void lancer() {
        Path binaireVoisin = resoudreBinaire();
        if (binaireVoisin == null) {
            System.err.println("Impossible de localiser l'exécutable graphique voisin.");
            System.exit(1);
            return;
        }
        if (!Files.isExecutable(binaireVoisin)) {
            System.err.println("Exécutable graphique introuvable : " + binaireVoisin
                    + " (\"lecture .\" n'est disponible que depuis l'application installée via jpackage).");
            System.exit(1);
            return;
        }
        try {
            ProcessBuilder constructeur = new ProcessBuilder("setsid", binaireVoisin.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
            constructeur.environment().remove("_JPACKAGE_LAUNCHER");
            constructeur.environment().remove("LD_LIBRARY_PATH");
            constructeur.start();
        } catch (IOException e) {
            System.err.println("Impossible de lancer l'interface graphique : " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Localise l'exécutable {@code courseflow} voisin. Priorité à la propriété système
     * {@code courseflow.bindir} (positionnée par {@code lecture-fast}, qui appelle le runtime
     * Java packagé directement — {@link ProcessHandle#command()} y renverrait le chemin de
     * {@code java}, pas celui de {@code lecture}) ; à défaut, déduite du chemin de l'exécutable
     * courant (cas du lanceur natif jpackage, qui n'a pas de wrapper devant lui).
     */
    private static Path resoudreBinaire() {
        String repertoireBin = System.getProperty("courseflow.bindir");
        if (repertoireBin != null) {
            return Path.of(repertoireBin, "courseflow");
        }
        return ProcessHandle.current().info().command()
                .map(commande -> Path.of(commande).resolveSibling("courseflow"))
                .orElse(null);
    }
}
