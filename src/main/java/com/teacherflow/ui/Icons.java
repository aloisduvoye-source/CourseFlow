package com.teacherflow.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Petites icônes vectorielles réutilisées dans l'interface (évite d'embarquer des fichiers image).
 */
final class Icons {

    private static final String CHEMIN_POUBELLE =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    private static final String CHEMIN_FERMER =
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";

    private Icons() {
    }

    static Node poubelle() {
        return icone(CHEMIN_POUBELLE, "icone-poubelle");
    }

    static Node fermer() {
        return icone(CHEMIN_FERMER, "icone-fermer");
    }

    private static Node icone(String chemin, String classeStyle) {
        SVGPath forme = new SVGPath();
        forme.setContent(chemin);
        forme.getStyleClass().add(classeStyle);
        Group groupe = new Group(forme);
        groupe.getTransforms().add(new Scale(0.6, 0.6));
        return groupe;
    }
}
