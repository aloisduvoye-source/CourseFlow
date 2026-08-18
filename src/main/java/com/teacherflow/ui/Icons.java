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

    private Icons() {
    }

    static Node poubelle() {
        SVGPath chemin = new SVGPath();
        chemin.setContent(CHEMIN_POUBELLE);
        chemin.getStyleClass().add("icone-poubelle");
        Group groupe = new Group(chemin);
        groupe.getTransforms().add(new Scale(0.6, 0.6));
        return groupe;
    }
}
