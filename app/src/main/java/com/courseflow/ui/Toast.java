package com.courseflow.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Message temporaire non bloquant, superposé en bas d'un conteneur, avec un lien d'action
 * optionnel (ex. "Annuler"). Contrairement à une {@link javafx.scene.control.Alert}, ne
 * force pas l'utilisateur à cliquer pour continuer : disparaît de lui-même après quelques
 * secondes.
 */
final class Toast {

    private Toast() {
    }

    static void montrer(StackPane conteneur, String message) {
        montrer(conteneur, message, null, null);
    }

    static void montrer(StackPane conteneur, String message, String libelleAction, Runnable surAction) {
        Label texte = new Label(message);
        texte.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        HBox contenu = new HBox(16, texte);
        contenu.setAlignment(Pos.CENTER_LEFT);
        contenu.setStyle("-fx-background-color: -color-accent-emphasis; -fx-background-radius: 6;"
                + " -fx-padding: 10 18 10 18;");
        StackPane.setAlignment(contenu, Pos.BOTTOM_CENTER);
        StackPane.setMargin(contenu, new Insets(0, 0, 24, 0));
        contenu.setOpacity(0);
        conteneur.getChildren().add(contenu);

        FadeTransition apparition = new FadeTransition(Duration.millis(150), contenu);
        apparition.setToValue(1);
        PauseTransition attente = new PauseTransition(Duration.millis(4000));
        FadeTransition disparition = new FadeTransition(Duration.millis(300), contenu);
        disparition.setToValue(0);
        SequentialTransition sequence = new SequentialTransition(apparition, attente, disparition);
        sequence.setOnFinished(e -> conteneur.getChildren().remove(contenu));

        if (libelleAction != null && surAction != null) {
            Hyperlink lienAction = new Hyperlink(libelleAction);
            lienAction.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0;"
                    + " -fx-border-color: transparent; -fx-underline: true;");
            lienAction.setOnAction(e -> {
                sequence.stop();
                conteneur.getChildren().remove(contenu);
                surAction.run();
            });
            contenu.getChildren().add(lienAction);
        }

        sequence.play();
    }
}
