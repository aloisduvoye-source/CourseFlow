package com.courseflow.persistence;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Levée quand le fichier de données existe mais n'a pas pu être désérialisé (JSON corrompu ou
 * incompatible). Le fichier fautif a été mis de côté sous {@link #getFichierQuarantaine()} : il
 * n'est donc plus à l'emplacement normal, et une sauvegarde ultérieure ne risque pas de l'écraser.
 */
public class DonneesIllisiblesException extends IOException {

    private final transient Path fichierQuarantaine;

    public DonneesIllisiblesException(Path fichierQuarantaine, Throwable cause) {
        super("Fichier de données illisible, mis de côté sous " + fichierQuarantaine, cause);
        this.fichierQuarantaine = fichierQuarantaine;
    }

    public Path getFichierQuarantaine() {
        return fichierQuarantaine;
    }
}
