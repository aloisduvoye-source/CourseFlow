package com.teacherflow.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Petites icônes vectorielles réutilisées dans l'interface (évite d'embarquer des fichiers image).
 */
final class Icons {

    private static final String CHEMIN_POUBELLE =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    private static final String CHEMIN_DOSSIER =
            "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z";
    private static final double TAILLE_CONTENEUR = 16;

    private Icons() {
    }

    static Node poubelle() {
        return icone(CHEMIN_POUBELLE);
    }

    static Node dossier() {
        return icone(CHEMIN_DOSSIER);
    }

    /**
     * Construit une icône réellement centrée dans un carré de taille fixe : le tracé SVG n'a pas
     * un centre géométrique qui coïncide avec son origine (0,0) locale, donc le recentrer
     * explicitement avant application de l'échelle évite qu'il ne dérive vers un coin une fois
     * mis à l'échelle autour de l'origine par défaut. Le résultat, de taille fixe et déjà centré,
     * reste centré quel que soit le padding du thème du bouton qui l'affichera.
     */
    private static Node icone(String chemin) {
        SVGPath forme = new SVGPath();
        forme.setContent(chemin);
        forme.setFill(Color.GRAY);

        Bounds bornes = forme.getBoundsInLocal();
        forme.setTranslateX(-(bornes.getMinX() + bornes.getWidth() / 2));
        forme.setTranslateY(-(bornes.getMinY() + bornes.getHeight() / 2));

        Group groupe = new Group(forme);
        groupe.getTransforms().add(new Scale(0.6, 0.6));

        StackPane conteneur = new StackPane(groupe);
        conteneur.setAlignment(Pos.CENTER);
        conteneur.setPrefSize(TAILLE_CONTENEUR, TAILLE_CONTENEUR);
        conteneur.setMinSize(TAILLE_CONTENEUR, TAILLE_CONTENEUR);
        conteneur.setMaxSize(TAILLE_CONTENEUR, TAILLE_CONTENEUR);
        return conteneur;
    }
}
