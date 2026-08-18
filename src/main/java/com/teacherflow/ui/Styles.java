package com.teacherflow.ui;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

/**
 * Point d'accès unique à la feuille de style de l'application. Les {@link Dialog} et
 * {@link javafx.scene.control.Alert} ouvrent leur propre fenêtre et n'héritent pas
 * automatiquement des styles de la scène principale : il faut la leur appliquer explicitement.
 */
public final class Styles {

    private static final String FEUILLE_STYLE = "/css/teacherflow.css";

    private Styles() {
    }

    public static void appliquer(Scene scene) {
        scene.getStylesheets().add(url());
    }

    public static void appliquer(Dialog<?> dialogue) {
        dialogue.getDialogPane().getStylesheets().add(url());
    }

    private static String url() {
        return Styles.class.getResource(FEUILLE_STYLE).toExternalForm();
    }
}
