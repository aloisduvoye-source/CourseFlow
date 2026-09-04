package com.courseflow.ui;

import com.courseflow.io.OuvreurFichiers;
import com.courseflow.util.TypeFichier;
import javafx.scene.Node;

/**
 * Icône de type de fichier partagée entre les listes de fichiers de l'application (hors
 * vignette image, propre à {@link AccueilPane} qui est seule à en afficher un aperçu).
 */
final class IconesFichier {

    private IconesFichier() {
    }

    static Node parType(String chemin) {
        if (chemin == null) {
            return Icons.document();
        }
        if (TypeFichier.estDocumentTexte(chemin)) {
            return Icons.crayon();
        }
        if (TypeFichier.estPresentation(chemin)) {
            return Icons.graphique();
        }
        if (OuvreurFichiers.estUrl(chemin)) {
            return Icons.lien();
        }
        return Icons.document();
    }
}
