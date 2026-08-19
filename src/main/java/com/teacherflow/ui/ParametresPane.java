package com.teacherflow.ui;

import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Parametres;
import com.teacherflow.model.PlageHoraire;
import com.teacherflow.util.NomsJours;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Réglages de l'emploi du temps : jours affichés, incrément minimal pour le déplacement/
 * redimensionnement des créneaux, et le modèle de blocs horaires (ex. 9h-10h puis
 * 10h20-11h20) reproduit à l'identique chaque jour. Ce modèle se manipule comme la grille
 * de l'emploi du temps : clic sur une zone libre pour ajouter un bloc, glisser pour le
 * déplacer ou le redimensionner, clic sur un bloc pour le modifier ou le supprimer. Chaque
 * modification est sauvegardée immédiatement ; la grille de l'emploi du temps se met à jour
 * au prochain affichage de son onglet.
 */
public class ParametresPane extends BorderPane {

    private static final int PAS_AFFICHAGE_MINUTES = 30;
    private static final int DUREE_MIN_MINUTES = 10;
    private static final int DUREE_DEFAUT_MINUTES = 60;
    private static final double PIXELS_PAR_MINUTE = 1.5;
    private static final double LARGEUR_COLONNE_HEURES = 60;
    private static final double LARGEUR_BLOC = 220;
    private static final double HAUTEUR_POIGNEE = 6;
    private static final double SEUIL_CLIC_PIXELS = 4;
    private static final double MARGE_VERTICALE = 10;

    private enum ModeInteraction { DEPLACER, REDIMENSIONNER_HAUT, REDIMENSIONNER_BAS }

    private final Parametres parametres;
    private final Runnable surChangement;
    private final Pane grilleBlocs = new Pane();
    private final HBox ligneGrille = new HBox();

    public ParametresPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.parametres = emploiDuTemps.getParametres();
        this.surChangement = surChangement;

        setPadding(new Insets(16));

        grilleBlocs.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        grilleBlocs.setOnMouseClicked(this::surClicGrilleVide);
        actualiserGrilleBlocs();

        Label titreBlocs = titreSection("Blocs horaires (identiques chaque jour)");
        Label aideBlocs = new Label("Clique dans la zone libre pour ajouter un bloc, glisse-le pour le déplacer "
                + "ou ses bords pour le redimensionner, clique dessus pour le modifier ou le supprimer. "
                + "Un nouveau créneau ne pourra être créé dans l'emploi du temps que sur ces horaires.");
        aideBlocs.setWrapText(true);
        aideBlocs.setMaxWidth(420);

        VBox contenu = new VBox(20,
                construireSectionJours(),
                construireSectionIncrement(),
                construireSectionPlageGrille(),
                new VBox(6, titreBlocs, aideBlocs, ligneGrille));
        contenu.setPadding(new Insets(0, 12, 0, 0));

        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        setCenter(defilement);
    }

    private VBox construireSectionJours() {
        VBox conteneur = new VBox(6, titreSection("Jours affichés dans l'emploi du temps"));
        for (DayOfWeek jour : DayOfWeek.values()) {
            CheckBox caseJour = new CheckBox(NomsJours.nom(jour));
            caseJour.setSelected(parametres.getJoursAffiches().contains(jour));
            caseJour.selectedProperty().addListener((obs, ancien, coche) -> {
                List<DayOfWeek> jours = new ArrayList<>(parametres.getJoursAffiches());
                if (coche) {
                    if (!jours.contains(jour)) {
                        jours.add(jour);
                        jours.sort(Comparator.comparingInt(DayOfWeek::getValue));
                    }
                } else {
                    jours.remove(jour);
                }
                parametres.setJoursAffiches(jours);
                notifierChangement();
            });
            conteneur.getChildren().add(caseJour);
        }
        return conteneur;
    }

    private HBox construireSectionIncrement() {
        ComboBox<Integer> choixPas = new ComboBox<>();
        choixPas.getItems().addAll(5, 10, 15, 20, 30, 60);
        choixPas.setValue(parametres.getPasMinutes());
        choixPas.setOnAction(e -> {
            Integer valeur = choixPas.getValue();
            if (valeur != null) {
                parametres.setPasMinutes(valeur);
                notifierChangement();
            }
        });

        HBox ligne = new HBox(8, new Label("Incrément minimal (minutes)"), choixPas);
        ligne.setAlignment(Pos.CENTER_LEFT);
        return ligne;
    }

    private HBox construireSectionPlageGrille() {
        List<LocalTime> options = optionsHeuresGrille();

        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(options);
        choixDebut.setValue(parametres.getHeureDebutGrille());

        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(options);
        choixFin.setValue(parametres.getHeureFinGrille());

        choixDebut.setOnAction(e -> {
            LocalTime valeur = choixDebut.getValue();
            if (valeur == null || !valeur.isBefore(parametres.getHeureFinGrille())) {
                choixDebut.setValue(parametres.getHeureDebutGrille());
                return;
            }
            parametres.setHeureDebutGrille(valeur);
            actualiserGrilleBlocs();
            notifierChangement();
        });
        choixFin.setOnAction(e -> {
            LocalTime valeur = choixFin.getValue();
            if (valeur == null || !valeur.isAfter(parametres.getHeureDebutGrille())) {
                choixFin.setValue(parametres.getHeureFinGrille());
                return;
            }
            parametres.setHeureFinGrille(valeur);
            actualiserGrilleBlocs();
            notifierChangement();
        });

        HBox ligne = new HBox(8, new Label("Plage horaire de la grille"), choixDebut, new Label("à"), choixFin);
        ligne.setAlignment(Pos.CENTER_LEFT);
        return ligne;
    }

    private static List<LocalTime> optionsHeuresGrille() {
        List<LocalTime> options = new ArrayList<>();
        LocalTime heure = LocalTime.MIDNIGHT;
        for (int i = 0; i < 48; i++) {
            options.add(heure);
            heure = heure.plusMinutes(30);
        }
        return options;
    }

    private Pane construireColonneHeures(double hauteur) {
        Pane pane = new Pane();
        pane.setPrefSize(LARGEUR_COLONNE_HEURES, hauteur);
        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            LocalTime heure = parametres.getHeureDebutGrille().plusMinutes(minute);
            if (heure.getMinute() == 0) {
                Label label = new Label(heure.toString());
                label.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
                label.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE - 6);
                label.setLayoutX(4);
                pane.getChildren().add(label);
            }
        }
        return pane;
    }

    private void actualiserGrilleBlocs() {
        double hauteurGrille = minutesGrille() * PIXELS_PAR_MINUTE + 2 * MARGE_VERTICALE;
        grilleBlocs.setPrefSize(LARGEUR_BLOC, hauteurGrille);
        ligneGrille.getChildren().setAll(construireColonneHeures(hauteurGrille), grilleBlocs);
        redessinerGrilleBlocs();
    }

    private void redessinerGrilleBlocs() {
        grilleBlocs.getChildren().clear();
        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            Region ligne = new Region();
            ligne.setPrefSize(LARGEUR_BLOC, 1);
            ligne.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE);
            ligne.setStyle("-fx-background-color: #eeeeee;");
            ligne.setMouseTransparent(true);
            grilleBlocs.getChildren().add(ligne);
        }
        for (PlageHoraire bloc : new ArrayList<>(parametres.getBlocs())) {
            grilleBlocs.getChildren().add(new BlocEditeur(bloc));
        }
    }

    private void surClicGrilleVide(MouseEvent e) {
        int pas = Math.max(1, parametres.getPasMinutes());
        int minuteClic = (int) ((e.getY() - MARGE_VERTICALE) / PIXELS_PAR_MINUTE);
        minuteClic = Math.round((float) minuteClic / pas) * pas;
        minuteClic = clamp(minuteClic, 0, minutesGrille() - DUREE_MIN_MINUTES);

        LocalTime debut = parametres.getHeureDebutGrille().plusMinutes(minuteClic);
        LocalTime finMax = parametres.getHeureFinGrille();
        LocalTime fin = debut.plusMinutes(DUREE_DEFAUT_MINUTES);
        if (fin.isAfter(finMax)) {
            fin = finMax;
        }
        if (!fin.isAfter(debut)) {
            return;
        }

        List<PlageHoraire> blocs = new ArrayList<>(parametres.getBlocs());
        blocs.add(new PlageHoraire(debut, fin));
        blocs.sort(Comparator.comparing(PlageHoraire::getDebut));
        parametres.setBlocs(blocs);
        redessinerGrilleBlocs();
        notifierChangement();
    }

    private void ouvrirDialogueBloc(PlageHoraire bloc) {
        List<LocalTime> limites = genererLimites();
        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(limites.subList(0, limites.size() - 1));
        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(limites.subList(1, limites.size()));
        choixDebut.setValue(bloc.getDebut());
        choixFin.setValue(bloc.getFin());

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(8);
        formulaire.addRow(0, new Label("Début"), choixDebut);
        formulaire.addRow(1, new Label("Fin"), choixFin);

        ButtonType boutonValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle("Bloc horaire");
        dialogue.getDialogPane().setContent(formulaire);
        dialogue.getDialogPane().getButtonTypes().addAll(boutonSupprimer, boutonValider, ButtonType.CANCEL);

        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() == ButtonType.CANCEL) {
            return;
        }
        if (resultat.get() == boutonSupprimer) {
            supprimerBloc(bloc);
            return;
        }

        LocalTime debut = choixDebut.getValue();
        LocalTime fin = choixFin.getValue();
        if (debut == null || fin == null || !fin.isAfter(debut)) {
            return;
        }
        bloc.setDebut(debut);
        bloc.setFin(fin);
        redessinerGrilleBlocs();
        notifierChangement();
    }

    private void supprimerBloc(PlageHoraire bloc) {
        List<PlageHoraire> blocs = new ArrayList<>(parametres.getBlocs());
        blocs.remove(bloc);
        parametres.setBlocs(blocs);
        redessinerGrilleBlocs();
        notifierChangement();
    }

    private List<LocalTime> genererLimites() {
        List<LocalTime> limites = new ArrayList<>();
        int pas = Math.max(1, parametres.getPasMinutes());
        LocalTime t = parametres.getHeureDebutGrille();
        while (!t.isAfter(parametres.getHeureFinGrille())) {
            limites.add(t);
            t = t.plusMinutes(pas);
        }
        return limites;
    }

    private int minutesGrille() {
        return (int) Duration.between(parametres.getHeureDebutGrille(), parametres.getHeureFinGrille()).toMinutes();
    }

    private static Label titreSection(String texte) {
        Label label = new Label(texte);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void notifierChangement() {
        if (surChangement != null) {
            surChangement.run();
        }
    }

    private static int toMinutes(LocalTime heure) {
        return heure.getHour() * 60 + heure.getMinute();
    }

    private static LocalTime minutesVersHeure(int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    private static int clamp(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }

    /**
     * Représentation déplaçable/redimensionnable d'un {@link PlageHoraire} du modèle de
     * blocs, avec la même interaction que les créneaux de l'emploi du temps : glisser le
     * corps pour déplacer, glisser un bord pour redimensionner, cliquer pour ouvrir la
     * boîte de dialogue de modification/suppression.
     */
    private final class BlocEditeur extends StackPane {

        private final PlageHoraire bloc;
        private final Label libelle = new Label();

        private int minutesDebutInitial;
        private int minutesFinInitial;
        private double pressSceneY;
        private boolean enTrainDeBouger;
        private ModeInteraction mode;

        BlocEditeur(PlageHoraire bloc) {
            this.bloc = bloc;

            libelle.setMouseTransparent(true);
            libelle.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
            getChildren().add(libelle);
            setAlignment(Pos.TOP_LEFT);
            setPadding(new Insets(3, 2, 2, 4));
            setPrefWidth(LARGEUR_BLOC - 4);
            setStyle("-fx-background-color: #3498db;");

            actualiser(toMinutes(bloc.getDebut()), toMinutes(bloc.getFin()));

            setOnMousePressed(this::surAppui);
            setOnMouseDragged(this::surGlissement);
            setOnMouseReleased(this::surRelachement);
            setOnMouseMoved(this::surSurvol);
            setOnMouseClicked(Event::consume);
        }

        private void actualiser(int debutMinutes, int finMinutes) {
            setLayoutX(2);
            setLayoutY(MARGE_VERTICALE + (debutMinutes - toMinutes(parametres.getHeureDebutGrille())) * PIXELS_PAR_MINUTE);
            setPrefHeight(Math.max(DUREE_MIN_MINUTES, finMinutes - debutMinutes) * PIXELS_PAR_MINUTE);
            libelle.setText(minutesVersHeure(debutMinutes) + " - " + minutesVersHeure(finMinutes));
        }

        private ModeInteraction determinerMode(double y) {
            if (y <= HAUTEUR_POIGNEE) {
                return ModeInteraction.REDIMENSIONNER_HAUT;
            }
            if (y >= getHeight() - HAUTEUR_POIGNEE) {
                return ModeInteraction.REDIMENSIONNER_BAS;
            }
            return ModeInteraction.DEPLACER;
        }

        private void surSurvol(MouseEvent e) {
            setCursor(determinerMode(e.getY()) == ModeInteraction.DEPLACER ? Cursor.MOVE : Cursor.V_RESIZE);
        }

        private void surAppui(MouseEvent e) {
            pressSceneY = e.getSceneY();
            minutesDebutInitial = toMinutes(bloc.getDebut());
            minutesFinInitial = toMinutes(bloc.getFin());
            mode = determinerMode(e.getY());
            enTrainDeBouger = false;
            e.consume();
        }

        private void surGlissement(MouseEvent e) {
            double deltaY = e.getSceneY() - pressSceneY;
            if (Math.abs(deltaY) > SEUIL_CLIC_PIXELS) {
                enTrainDeBouger = true;
            }
            if (!enTrainDeBouger) {
                e.consume();
                return;
            }

            int pas = Math.max(1, parametres.getPasMinutes());
            long pasArrondi = Math.round((deltaY / PIXELS_PAR_MINUTE) / pas);
            int deltaMinutes = (int) (pasArrondi * pas);
            int minGrille = toMinutes(parametres.getHeureDebutGrille());
            int maxGrille = toMinutes(parametres.getHeureFinGrille());

            int nouveauDebut = minutesDebutInitial;
            int nouveauFin = minutesFinInitial;
            switch (mode) {
                case DEPLACER -> {
                    int duree = minutesFinInitial - minutesDebutInitial;
                    nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, maxGrille - duree);
                    nouveauFin = nouveauDebut + duree;
                }
                case REDIMENSIONNER_HAUT ->
                        nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, minutesFinInitial - DUREE_MIN_MINUTES);
                case REDIMENSIONNER_BAS ->
                        nouveauFin = clamp(minutesFinInitial + deltaMinutes, minutesDebutInitial + DUREE_MIN_MINUTES, maxGrille);
            }

            actualiser(nouveauDebut, nouveauFin);
            e.consume();
        }

        private void surRelachement(MouseEvent e) {
            if (!enTrainDeBouger) {
                ouvrirDialogueBloc(bloc);
                e.consume();
                return;
            }

            int debutFinal = toMinutes(parametres.getHeureDebutGrille()) + (int) Math.round((getLayoutY() - MARGE_VERTICALE) / PIXELS_PAR_MINUTE);
            int finFinal = debutFinal + (int) Math.round(getPrefHeight() / PIXELS_PAR_MINUTE);
            bloc.setDebut(minutesVersHeure(debutFinal));
            bloc.setFin(minutesVersHeure(finFinal));
            notifierChangement();
            e.consume();
        }
    }
}
