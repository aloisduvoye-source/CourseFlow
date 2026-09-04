package com.courseflow.ui;

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
    private static final String CHEMIN_DOCUMENT =
            "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z";
    private static final String CHEMIN_LIEN =
            "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z";
    private static final String CHEMIN_CRAYON =
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34"
                    + "c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private static final String CHEMIN_GRAPHIQUE = "M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zm5.6 8H19v6h-2.8v-6z";
    private static final String CHEMIN_FLECHE_GAUCHE = "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z";
    private static final String CHEMIN_FLECHE_DROITE = "M8.59 16.59L10 18l6-6-6-6-1.41 1.41L13.17 12z";
    private static final String CHEMIN_TAG =
            "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.41l9 9"
                    + "c.36.36.86.59 1.41.59.55 0 1.05-.23 1.41-.59l7-7c.37-.36.59-.86.59-1.42"
                    + " 0-.55-.23-1.05-.59-1.41zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";
    private static final double TAILLE_CONTENEUR = 16;

    private Icons() {
    }

    static Node poubelle() {
        return icone(CHEMIN_POUBELLE);
    }

    static Node dossier() {
        return icone(CHEMIN_DOSSIER);
    }

    static Node document() {
        return icone(CHEMIN_DOCUMENT);
    }

    static Node lien() {
        return icone(CHEMIN_LIEN);
    }

    static Node crayon() {
        return icone(CHEMIN_CRAYON);
    }

    static Node graphique() {
        return icone(CHEMIN_GRAPHIQUE);
    }

    static Node flecheGauche() {
        return icone(CHEMIN_FLECHE_GAUCHE);
    }

    static Node flecheDroite() {
        return icone(CHEMIN_FLECHE_DROITE);
    }

    static Node tag() {
        return icone(CHEMIN_TAG);
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
