package com.teacherflow.ui;

import com.teacherflow.cli.NavigationCreneaux;
import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.util.NomsJours;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Écran d'accueil : équivalent graphique de la commande {@code lecture} — affiche directement
 * le créneau du moment (cours, salle, description, fichiers) avec un bouton pour tout ouvrir,
 * et des boutons précédent/suivant pour naviguer vers un autre créneau (notamment pendant un
 * creux entre deux cours, où aucun créneau n'est "courant").
 */
public class AccueilPane extends BorderPane {

    private final EmploiDuTemps emploiDuTemps;
    private final Label titre = new Label();
    private final Label libelleCours = new Label();
    private final Label libelleSalle = new Label();
    private final Label libelleDescription = new Label();
    private final VBox listeFichiers = new VBox(4);
    private final Button boutonOuvrir = new Button("Ouvrir les fichiers");

    private DayOfWeek jourReference;
    private LocalTime heureReference;
    private Creneau creneauAffiche;

    public AccueilPane(EmploiDuTemps emploiDuTemps) {
        this.emploiDuTemps = emploiDuTemps;

        setPadding(new Insets(24));

        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        libelleCours.setStyle("-fx-font-size: 15;");

        boutonOuvrir.setOnAction(e -> ouvrirFichiersDuCreneauAffiche());

        Button boutonPrecedent = new Button("◀ Précédent");
        boutonPrecedent.setOnAction(e -> naviguer(false));
        Button boutonSuivant = new Button("Suivant ▶");
        boutonSuivant.setOnAction(e -> naviguer(true));
        HBox boutonsNavigation = new HBox(8, boutonPrecedent, boutonSuivant);

        VBox contenu = new VBox(12,
                titre, libelleCours, libelleSalle, libelleDescription,
                titreSection("Fichiers"), listeFichiers, boutonOuvrir,
                boutonsNavigation);
        setCenter(contenu);

        rafraichir();
    }

    private static Label titreSection(String texte) {
        Label label = new Label(texte);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    /**
     * Réaffiche le créneau courant (jour/heure système), abandonnant toute navigation en cours.
     * Appelé à la construction et à chaque affichage de l'onglet.
     */
    public void rafraichir() {
        jourReference = LocalDate.now().getDayOfWeek();
        heureReference = LocalTime.now();
        afficherCreneau(emploiDuTemps.trouverCreneauCourant(jourReference, heureReference).orElse(null));
    }

    private void naviguer(boolean suivant) {
        Optional<Creneau> creneauOpt = suivant
                ? NavigationCreneaux.suivant(emploiDuTemps.getCreneaux(), jourReference, heureReference)
                : NavigationCreneaux.precedent(emploiDuTemps.getCreneaux(), jourReference, heureReference);
        if (creneauOpt.isEmpty()) {
            return;
        }
        Creneau creneau = creneauOpt.get();
        jourReference = creneau.getJour();
        heureReference = creneau.getHeureDebut();
        afficherCreneau(creneau);
    }

    private void afficherCreneau(Creneau creneau) {
        creneauAffiche = creneau;
        boolean present = creneau != null;
        boutonOuvrir.setVisible(present);
        boutonOuvrir.setManaged(present);

        if (!present) {
            titre.setText("Aucun créneau en ce moment");
            libelleCours.setText("");
            libelleSalle.setText("");
            libelleDescription.setText("");
            listeFichiers.getChildren().clear();
            return;
        }

        titre.setText(NomsJours.nom(creneau.getJour()) + " " + creneau.getHeureDebut() + " - " + creneau.getHeureFin());

        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        libelleCours.setText(cours != null ? cours.getNom() : "(cours supprimé)");

        libelleSalle.setText(creneau.getSalle() != null && !creneau.getSalle().isBlank()
                ? "Salle : " + creneau.getSalle() : "");
        libelleDescription.setText(creneau.getDescription() != null && !creneau.getDescription().isBlank()
                ? creneau.getDescription() : "");

        listeFichiers.getChildren().clear();
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        if (fichiers.isEmpty()) {
            listeFichiers.getChildren().add(new Label("(aucun fichier sélectionné pour ce créneau)"));
        } else {
            for (Fichier fichier : fichiers) {
                String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                        ? fichier.getNomAffichage() : fichier.getChemin();
                listeFichiers.getChildren().add(new Label(libelle));
            }
        }
    }

    private void ouvrirFichiersDuCreneauAffiche() {
        if (creneauAffiche == null) {
            return;
        }
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneauAffiche);
        if (fichiers.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION, "Aucun fichier sélectionné pour ce créneau.");
            alerte.setTitle("Rien à ouvrir");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }
        List<String> echecs = OuvreurFichiers.ouvrir(fichiers);
        if (!echecs.isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.WARNING,
                    "Certains fichiers n'ont pas pu être ouverts :\n" + String.join("\n", echecs));
            alerte.setTitle("Ouverture partielle");
            alerte.setHeaderText(null);
            alerte.showAndWait();
        }
    }
}
