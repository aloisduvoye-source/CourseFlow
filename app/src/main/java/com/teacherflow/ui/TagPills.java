package com.teacherflow.ui;

import javafx.scene.control.Label;

/**
 * Rendu visuel partagé des tags de fichiers sous forme de petites pastilles colorées, utilisé à
 * la fois dans la liste de fichiers d'un cours et dans les listes de sélection de fichiers.
 */
final class TagPills {

    private TagPills() {
    }

    static Label pastille(String tag, String couleurHex) {
        Label pastille = new Label(tag);
        pastille.setStyle("-fx-background-color: " + couleurHex + "; -fx-background-radius: 8; "
                + "-fx-padding: 1 8 1 8; -fx-text-fill: white; -fx-font-size: 10;");
        return pastille;
    }
}
