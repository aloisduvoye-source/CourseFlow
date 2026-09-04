package com.courseflow.ui;

import atlantafx.base.theme.Styles;
import com.courseflow.io.OuvreurFichiers;
import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.Parametres;
import com.courseflow.model.PlageHoraire;
import com.courseflow.model.TypeSemaine;
import com.courseflow.util.NomsJours;
import com.courseflow.util.TypeFichier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Grille hebdomadaire (jours × heures) permettant d'assigner un {@link Cours} à chaque
 * {@link Creneau}, puis de le déplacer ou de le redimensionner directement à la souris,
 * ou de le modifier/supprimer via une boîte de dialogue. Les jours affichés, l'incrément
 * de déplacement et les plages horaires actives par jour viennent de {@link Parametres}.
 */
public class EmploiDuTempsPane extends BorderPane {

    private static final int PAS_AFFICHAGE_MINUTES = 30;
    private static final int DUREE_MIN_MINUTES = 10;
    private static final double PIXELS_PAR_MINUTE = 1.5;
    private static final double LARGEUR_COLONNE_HEURES = 60;
    private static final double LARGEUR_COLONNE_JOUR_MIN = 110;
    private static final double LARGEUR_COLONNE_JOUR_DEFAUT = 140;
    private static final double HAUTEUR_POIGNEE = 6;
    private static final double SEUIL_CLIC_PIXELS = 4;
    private static final double MARGE_VERTICALE = 10;

    private enum ModeInteraction { DEPLACER, REDIMENSIONNER_HAUT, REDIMENSIONNER_BAS }

    private final EmploiDuTemps emploiDuTemps;
    private final Runnable surChangement;
    private final HBox ligneEntetes = new HBox();
    private final HBox ligneColonnes = new HBox();
    private final ScrollPane defilement = new ScrollPane();
    private double largeurColonneJour = LARGEUR_COLONNE_JOUR_DEFAUT;
    private DayOfWeek[] joursAffiches = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private int pasMinutes = 10;
    private LocalTime heureDebutGrille = LocalTime.of(7, 0);
    private LocalTime heureFinGrille = LocalTime.of(20, 0);
    private final Deque<List<Creneau>> pileAnnuler = new ArrayDeque<>();
    private final Deque<List<Creneau>> pileRetablir = new ArrayDeque<>();
    private TypeSemaine semaineAffichee;

    public EmploiDuTempsPane(EmploiDuTemps emploiDuTemps, Runnable surChangement) {
        this.emploiDuTemps = emploiDuTemps;
        this.surChangement = surChangement;
        this.semaineAffichee = emploiDuTemps.getParametres().semainePour(LocalDate.now());

        setPadding(new Insets(16));
        setFocusTraversable(true);
        setOnKeyPressed(this::gererRaccourciClavier);

        defilement.setContent(ligneColonnes);
        defilement.setFitToWidth(false);
        defilement.viewportBoundsProperty().addListener((obs, ancien, nouveau) -> rafraichir());

        VBox.setVgrow(defilement, Priority.ALWAYS);
        setCenter(new VBox(construireSelecteurSemaine(), ligneEntetes, defilement));

        rafraichir();
    }

    /**
     * Ouvre la boîte de dialogue d'édition pour un créneau existant, depuis un autre écran
     * (ex. le raccourci "Modifier" de l'Accueil) sans passer par un clic sur son bloc.
     */
    public void ouvrirEdition(Creneau creneau) {
        if (creneau == null) {
            return;
        }
        if (creneau.getTypeSemaine() != TypeSemaine.TOUTES && creneau.getTypeSemaine() != semaineAffichee) {
            semaineAffichee = creneau.getTypeSemaine();
            rafraichir();
        }
        ouvrirDialogueCreneau(creneau, creneau.getJour(), creneau.getHeureDebut(), creneau.getHeureFin());
    }

    private HBox construireSelecteurSemaine() {
        ToggleGroup groupe = new ToggleGroup();
        ToggleButton boutonA = new ToggleButton("Semaine A");
        ToggleButton boutonB = new ToggleButton("Semaine B");
        boutonA.setToggleGroup(groupe);
        boutonB.setToggleGroup(groupe);
        boutonA.setSelected(semaineAffichee == TypeSemaine.A);
        boutonB.setSelected(semaineAffichee == TypeSemaine.B);
        boutonA.setOnAction(e -> { semaineAffichee = TypeSemaine.A; rafraichir(); });
        boutonB.setOnAction(e -> { semaineAffichee = TypeSemaine.B; rafraichir(); });

        CheckBox caseGuides = new CheckBox("Afficher les guides");
        caseGuides.setSelected(emploiDuTemps.getParametres().isAfficherGuidesBlocs());
        caseGuides.setOnAction(e -> {
            emploiDuTemps.getParametres().setAfficherGuidesBlocs(caseGuides.isSelected());
            notifierChangement();
            rafraichir();
        });

        Region espaceur = new Region();
        HBox.setHgrow(espaceur, Priority.ALWAYS);

        HBox ligne = new HBox(8, boutonA, boutonB, espaceur, caseGuides);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(0, 0, 8, 0));
        return ligne;
    }

    private void gererRaccourciClavier(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.Z) {
            if (e.isShiftDown()) {
                retablir();
            } else {
                annuler();
            }
            e.consume();
        }
    }

    private void enregistrerAvantModification() {
        pileAnnuler.push(copierCreneaux());
        pileRetablir.clear();
    }

    private void annuler() {
        if (pileAnnuler.isEmpty()) {
            return;
        }
        pileRetablir.push(copierCreneaux());
        restaurer(pileAnnuler.pop());
    }

    private void retablir() {
        if (pileRetablir.isEmpty()) {
            return;
        }
        pileAnnuler.push(copierCreneaux());
        restaurer(pileRetablir.pop());
    }

    private void restaurer(List<Creneau> etat) {
        emploiDuTemps.setCreneaux(etat);
        notifierChangement();
        rafraichir();
    }

    private List<Creneau> copierCreneaux() {
        return emploiDuTemps.getCreneaux().stream().map(EmploiDuTempsPane::copierCreneau).collect(Collectors.toList());
    }

    private static Creneau copierCreneau(Creneau original) {
        Creneau copie = new Creneau(original.getJour(), original.getHeureDebut(), original.getHeureFin(), original.getCoursId());
        copie.setId(original.getId());
        copie.setSalle(original.getSalle());
        copie.setDescription(original.getDescription());
        copie.setFichiersSelectionnesIds(new ArrayList<>(original.getFichiersSelectionnesIds()));
        copie.setTypeSemaine(original.getTypeSemaine());
        return copie;
    }

    private void mettreAJourEntetes() {
        ligneEntetes.getChildren().clear();
        ligneEntetes.getChildren().add(espace(LARGEUR_COLONNE_HEURES));
        LocalDate lundiCourant = LocalDate.now().with(DayOfWeek.MONDAY);
        boolean aujourdhuiAffiche = indexDuJour(LocalDate.now().getDayOfWeek()) >= 0;
        for (DayOfWeek jour : joursAffiches) {
            boolean estAujourdhui = aujourdhuiAffiche && jour == LocalDate.now().getDayOfWeek();

            Label nomJour = new Label(NomsJours.nom(jour));
            nomJour.setStyle("-fx-font-weight: bold; -fx-font-size: 13;"
                    + (estAujourdhui ? " -fx-text-fill: -color-accent-emphasis;" : ""));

            Label date = new Label(lundiCourant.plusDays(jour.getValue() - 1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("d/M")));
            date.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11;");

            VBox enTete = new VBox(2, nomJour, date);
            enTete.setAlignment(Pos.CENTER);
            enTete.setPrefWidth(largeurColonneJour);
            enTete.setPadding(new Insets(8, 4, 8, 4));
            enTete.setStyle("-fx-background-color: " + (estAujourdhui ? "-color-accent-subtle" : "-color-bg-subtle")
                    + "; -fx-background-radius: 6 6 0 0; -fx-border-color: -color-border-default;"
                    + " -fx-border-width: 1 1 0 1; -fx-border-radius: 6 6 0 0;");
            ligneEntetes.getChildren().add(enTete);
        }
    }

    private static Region espace(double largeur) {
        Region region = new Region();
        region.setPrefWidth(largeur);
        return region;
    }

    private static Label labelSection(String texte) {
        Label label = new Label(texte);
        label.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10; -fx-font-weight: bold;");
        return label;
    }

    private static Region espaceurExtensible() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    /**
     * Icône de type de fichier pour une ligne de la liste du dialogue de créneau : simple
     * indication visuelle par extension (pas d'aperçu image ici, contrairement à l'Accueil,
     * pour rester compact dans cette liste filtrable).
     */
    // TODO UX/qualité : logique dupliquée avec AccueilPane.iconeFichier (règles légèrement
    // différentes, pas de vignette image ici). Factoriser dans une classe utilitaire partagée.
    private static Node iconePourFichier(String chemin) {
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

    // TODO UX : contrairement à AccueilPane.rafraichir() qui resynchronise dateAffichee sur
    // aujourd'hui, cette méthode ne réinitialise pas semaineAffichee. Un utilisateur qui a
    // basculé manuellement sur "Semaine B" puis revient sur cet onglet un autre jour reste sur
    // "Semaine B" au lieu de refléter la semaine réelle -> risque d'éditer la mauvaise semaine
    // sans s'en rendre compte.
    public void rafraichir() {
        Parametres parametres = emploiDuTemps.getParametres();
        List<DayOfWeek> jours = parametres.getJoursAffiches();
        if (!jours.isEmpty()) {
            joursAffiches = jours.toArray(new DayOfWeek[0]);
        }
        pasMinutes = Math.max(1, parametres.getPasMinutes());
        heureDebutGrille = parametres.getHeureDebutGrille();
        heureFinGrille = parametres.getHeureFinGrille();

        largeurColonneJour = calculerLargeurColonneJour();
        mettreAJourEntetes();

        ligneColonnes.getChildren().clear();

        double hauteurGrille = minutesGrille() * PIXELS_PAR_MINUTE + 2 * MARGE_VERTICALE;

        ligneColonnes.getChildren().add(construireColonneHeures(hauteurGrille));
        ligneColonnes.getChildren().add(construireGrilleUnique(hauteurGrille));
    }

    private double calculerLargeurColonneJour() {
        Bounds viewport = defilement.getViewportBounds();
        double largeurDisponible = viewport != null ? viewport.getWidth() : 0;
        if (largeurDisponible <= 0) {
            return LARGEUR_COLONNE_JOUR_DEFAUT;
        }
        double largeurJours = largeurDisponible - LARGEUR_COLONNE_HEURES;
        return Math.max(LARGEUR_COLONNE_JOUR_MIN, largeurJours / joursAffiches.length);
    }

    private Pane construireColonneHeures(double hauteur) {
        Pane pane = new Pane();
        pane.setPrefSize(LARGEUR_COLONNE_HEURES, hauteur);
        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            LocalTime heure = heureDebutGrille.plusMinutes(minute);
            if (heure.getMinute() == 0) {
                Label label = new Label(heure.toString());
                label.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10;");
                label.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE - 6);
                label.setLayoutX(4);
                pane.getChildren().add(label);
            }
        }
        return pane;
    }

   
    private Pane construireGrilleUnique(double hauteur) {
        double largeurTotale = joursAffiches.length * largeurColonneJour;
        Pane pane = new Pane();
        pane.setPrefSize(largeurTotale, hauteur);
        pane.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 0 1;");

        int indexAujourdhui = indexDuJour(LocalDate.now().getDayOfWeek());
        if (indexAujourdhui >= 0) {
            Region surbrillance = new Region();
            surbrillance.setPrefSize(largeurColonneJour, hauteur);
            surbrillance.setLayoutX(indexAujourdhui * largeurColonneJour);
            surbrillance.setStyle("-fx-background-color: -color-bg-subtle;");
            surbrillance.setMouseTransparent(true);
            pane.getChildren().add(surbrillance);
        }

        for (int minute = 0; minute <= minutesGrille(); minute += PAS_AFFICHAGE_MINUTES) {
            Region ligne = new Region();
            ligne.setPrefSize(largeurTotale, 1);
            ligne.setLayoutY(MARGE_VERTICALE + minute * PIXELS_PAR_MINUTE);
            ligne.setStyle("-fx-background-color: -color-border-muted;");
            ligne.setMouseTransparent(true);
            pane.getChildren().add(ligne);
        }
        for (int i = 1; i < joursAffiches.length; i++) {
            Region separateur = new Region();
            separateur.setPrefSize(1, hauteur);
            separateur.setLayoutX(i * largeurColonneJour);
            separateur.setStyle("-fx-background-color: -color-border-default;");
            separateur.setMouseTransparent(true);
            pane.getChildren().add(separateur);
        }

        ajouterCellulesCliquables(pane);

        List<Creneau> creneauxVisibles = emploiDuTemps.getCreneaux().stream()
                .filter(c -> indexDuJour(c.getJour()) >= 0)
                .filter(c -> chevaucheGrille(c.getHeureDebut(), c.getHeureFin()))
                .filter(c -> c.correspondA(semaineAffichee))
                .collect(Collectors.toList());
        creneauxVisibles.forEach(c -> pane.getChildren().add(new BlocCreneau(c)));

        for (int jourIndex = 0; jourIndex < joursAffiches.length; jourIndex++) {
            DayOfWeek jour = joursAffiches[jourIndex];
            boolean journeeVide = creneauxVisibles.stream().noneMatch(c -> c.getJour() == jour);
            if (journeeVide) {
                Label vide = new Label("Journée libre");
                vide.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-font-size: 12;");
                vide.setPrefWidth(largeurColonneJour);
                vide.setAlignment(Pos.CENTER);
                vide.setMouseTransparent(true);
                vide.setLayoutX(jourIndex * largeurColonneJour);
                vide.setLayoutY(hauteur / 2 - 8);
                pane.getChildren().add(vide);
            }
        }

        return pane;
    }

    private void ajouterCellulesCliquables(Pane pane) {
        List<PlageHoraire> blocs = new ArrayList<>(emploiDuTemps.getParametres().getBlocs());
        blocs.sort(Comparator.comparing(PlageHoraire::getDebut));
        int debutGrille = toMinutes(heureDebutGrille);
        boolean guidesVisibles = emploiDuTemps.getParametres().isAfficherGuidesBlocs();
        String pourcentageAlternance = emploiDuTemps.getParametres().isThemeSombre() ? "-25%" : "25%";
        double opaciteRepos = guidesVisibles ? 0.6 : 0;
        double opaciteSurvol = guidesVisibles ? 0.9 : 0.35;
        for (int jourIndex = 0; jourIndex < joursAffiches.length; jourIndex++) {
            DayOfWeek jour = joursAffiches[jourIndex];
            for (int i = 0; i < blocs.size(); i++) {
                PlageHoraire bloc = blocs.get(i);
                int minuteDebut = Math.max(0, toMinutes(bloc.getDebut()) - debutGrille);
                int minuteFin = Math.min(minutesGrille(), toMinutes(bloc.getFin()) - debutGrille);
                if (minuteFin <= minuteDebut) {
                    continue;
                }

                Region cellule = new Region();
                cellule.setLayoutX(jourIndex * largeurColonneJour);
                cellule.setLayoutY(MARGE_VERTICALE + minuteDebut * PIXELS_PAR_MINUTE);
                cellule.setPrefSize(largeurColonneJour, (minuteFin - minuteDebut) * PIXELS_PAR_MINUTE);
                cellule.setCursor(Cursor.HAND);
                cellule.setStyle("-fx-background-color: " + (i % 2 == 1
                        ? "derive(-color-accent-subtle, " + pourcentageAlternance + ");" : "-color-accent-subtle;"));
                cellule.setOpacity(opaciteRepos);
                cellule.setOnMouseEntered(e -> cellule.setOpacity(opaciteSurvol));
                cellule.setOnMouseExited(e -> cellule.setOpacity(opaciteRepos));
                cellule.setOnMouseClicked(e -> ouvrirDialogueCreneau(null, jour, bloc.getDebut(), bloc.getFin()));
                pane.getChildren().add(cellule);
            }
        }
    }

    private int minutesGrille() {
        return (int) Duration.between(heureDebutGrille, heureFinGrille).toMinutes();
    }

    private boolean chevaucheGrille(LocalTime debut, LocalTime fin) {
        return fin.isAfter(heureDebutGrille) && debut.isBefore(heureFinGrille);
    }

    private int indexDuJour(DayOfWeek jour) {
        for (int i = 0; i < joursAffiches.length; i++) {
            if (joursAffiches[i] == jour) {
                return i;
            }
        }
        return -1;
    }

    // TODO UX : déplacer/redimensionner un créneau ne se fait qu'à la souris (glisser le bloc /
    // ses bords), sans raccourci clavier alternatif ni poignée visible (seul le curseur change
    // au survol). Peu découvrable pour un nouvel utilisateur et inaccessible au clavier/trackpad
    // peu précis.
    private final class BlocCreneau extends StackPane {

        private final Creneau creneau;
        private final Label titre = new Label();
        private final Label meta = new Label();
        private final VBox contenu = new VBox(2, titre, meta);

        private int dayIndexInitial;
        private int minutesDebutInitial;
        private int minutesFinInitial;
        private int dayIndexCourant;
        private double pressSceneX;
        private double pressSceneY;
        private boolean enTrainDeBouger;
        private ModeInteraction mode;

        BlocCreneau(Creneau creneau) {
            this.creneau = creneau;

            titre.setWrapText(true);
            titre.setMouseTransparent(true);
            titre.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            meta.setWrapText(true);
            meta.setMouseTransparent(true);
            meta.setStyle("-fx-text-fill: rgba(255,255,255,0.88); -fx-font-size: 10.5;");
            contenu.setMouseTransparent(true);
            getChildren().add(contenu);
            setAlignment(Pos.TOP_LEFT);
            setPadding(new Insets(6, 6, 6, 8));
            setPrefWidth(largeurColonneJour - 6);
            setStyle(styleFond());

            actualiser(Math.max(0, indexDuJour(creneau.getJour())),
                    toMinutes(creneau.getHeureDebut()), toMinutes(creneau.getHeureFin()));

            setOnMousePressed(this::surAppui);
            setOnMouseDragged(this::surGlissement);
            setOnMouseReleased(this::surRelachement);
            setOnMouseMoved(this::surSurvol);
            setOnMouseClicked(Event::consume);
        }

        private String styleFond() {
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String couleur = cours != null && cours.getCouleur() != null ? cours.getCouleur() : Couleurs.COURS_SANS_COULEUR;
            return "-fx-background-color: " + couleur + "; -fx-background-radius: 6;"
                    + " -fx-border-color: derive(" + couleur + ", -15%); -fx-border-width: 1; -fx-border-radius: 6;";
        }

        private void actualiser(int dayIndex, int debutMinutes, int finMinutes) {
            dayIndexCourant = dayIndex;
            int grilleDebut = toMinutes(heureDebutGrille);
            int grilleFin = toMinutes(heureFinGrille);
            int debutAffiche = clamp(debutMinutes, grilleDebut, grilleFin);
            int finAffiche = clamp(finMinutes, grilleDebut, grilleFin);
            setLayoutX(dayIndex * largeurColonneJour + 3);
            setLayoutY(MARGE_VERTICALE + (debutAffiche - grilleDebut) * PIXELS_PAR_MINUTE);
            setPrefHeight(Math.max(DUREE_MIN_MINUTES, finAffiche - debutAffiche) * PIXELS_PAR_MINUTE);
            Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
            String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";
            if (creneau.getSalle() != null && !creneau.getSalle().isBlank()) {
                nomCours += " · " + creneau.getSalle();
            }
            titre.setText(nomCours);

            StringBuilder texte = new StringBuilder();
            texte.append(minutesVersHeure(debutMinutes)).append(" - ").append(minutesVersHeure(finMinutes));
            if (creneau.getTypeSemaine() != TypeSemaine.TOUTES) {
                texte.append(" (").append(creneau.getTypeSemaine()).append(')');
            }
            if (creneau.getDescription() != null && !creneau.getDescription().isBlank()) {
                texte.append('\n').append(creneau.getDescription());
            }
            meta.setText(texte.toString());
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
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            dayIndexInitial = Math.max(0, indexDuJour(creneau.getJour()));
            minutesDebutInitial = toMinutes(creneau.getHeureDebut());
            minutesFinInitial = toMinutes(creneau.getHeureFin());
            mode = determinerMode(e.getY());
            enTrainDeBouger = false;
            e.consume();
        }

        private void surGlissement(MouseEvent e) {
            double deltaX = e.getSceneX() - pressSceneX;
            double deltaY = e.getSceneY() - pressSceneY;
            if (Math.abs(deltaX) > SEUIL_CLIC_PIXELS || Math.abs(deltaY) > SEUIL_CLIC_PIXELS) {
                enTrainDeBouger = true;
            }
            if (!enTrainDeBouger) {
                e.consume();
                return;
            }

            long pas = Math.round((deltaY / PIXELS_PAR_MINUTE) / pasMinutes);
            int deltaMinutes = (int) (pas * pasMinutes);
            int minGrille = toMinutes(heureDebutGrille);
            int maxGrille = toMinutes(heureFinGrille);

            int nouveauDayIndex = dayIndexInitial;
            int nouveauDebut = minutesDebutInitial;
            int nouveauFin = minutesFinInitial;
            switch (mode) {
                case DEPLACER -> {
                    int duree = minutesFinInitial - minutesDebutInitial;
                    nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, maxGrille - duree);
                    nouveauFin = nouveauDebut + duree;
                    int deltaJours = (int) Math.round(deltaX / largeurColonneJour);
                    nouveauDayIndex = clamp(dayIndexInitial + deltaJours, 0, joursAffiches.length - 1);
                }
                case REDIMENSIONNER_HAUT ->
                        nouveauDebut = clamp(minutesDebutInitial + deltaMinutes, minGrille, minutesFinInitial - DUREE_MIN_MINUTES);
                case REDIMENSIONNER_BAS ->
                        nouveauFin = clamp(minutesFinInitial + deltaMinutes, minutesDebutInitial + DUREE_MIN_MINUTES, maxGrille);
            }

            actualiser(nouveauDayIndex, nouveauDebut, nouveauFin);
            e.consume();
        }

        private void surRelachement(MouseEvent e) {
            if (!enTrainDeBouger) {
                ouvrirDialogueCreneau(creneau, creneau.getJour(), creneau.getHeureDebut(), creneau.getHeureFin());
                e.consume();
                return;
            }

            int debutFinal = toMinutes(heureDebutGrille) + (int) Math.round((getLayoutY() - MARGE_VERTICALE) / PIXELS_PAR_MINUTE);
            int finFinal = debutFinal + (int) Math.round(getPrefHeight() / PIXELS_PAR_MINUTE);
            enregistrerAvantModification();
            creneau.setJour(joursAffiches[dayIndexCourant]);
            creneau.setHeureDebut(minutesVersHeure(debutFinal));
            creneau.setHeureFin(minutesVersHeure(finFinal));
            notifierChangement();
            // Reconstruit toute la grille (pas seulement ce bloc) : le jour de départ peut être
            // devenu vide (ou le jour d'arrivée ne plus l'être), ce qui doit mettre à jour le
            // libellé "Journée libre"/"Pas de cours" affiché pour ce jour.
            rafraichir();
            e.consume();
        }
    }

    private void ouvrirDialogueCreneau(Creneau creneauExistant, DayOfWeek jour, LocalTime heureDebutParDefaut,
            LocalTime heureFinParDefaut) {
        if (emploiDuTemps.getCours().isEmpty()) {
            Alert alerte = new Alert(Alert.AlertType.INFORMATION,
                    "Crée d'abord un cours dans l'onglet \"Cours\" avant de remplir l'emploi du temps.");
            alerte.setTitle("Aucun cours disponible");
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }

        List<LocalTime> limites = genererLimites();
        List<LocalTime> optionsDebut = avecValeur(limites.subList(0, limites.size() - 1),
                creneauExistant != null ? creneauExistant.getHeureDebut() : heureDebutParDefaut);
        List<LocalTime> optionsFin = avecValeur(limites.subList(1, limites.size()),
                creneauExistant != null ? creneauExistant.getHeureFin() : heureFinParDefaut);

        ComboBox<Cours> choixCours = new ComboBox<>();
        choixCours.getItems().addAll(emploiDuTemps.getCours());
        ComboBox<LocalTime> choixDebut = new ComboBox<>();
        choixDebut.getItems().addAll(optionsDebut);
        ComboBox<LocalTime> choixFin = new ComboBox<>();
        choixFin.getItems().addAll(optionsFin);
        TextField champSalle = new TextField();
        champSalle.setPromptText("Salle (optionnel)");
        TextField champDescription = new TextField();
        champDescription.setPromptText("Description (optionnel)");

        ComboBox<TypeSemaine> choixSemaine = new ComboBox<>();
        choixSemaine.getItems().addAll(TypeSemaine.TOUTES, TypeSemaine.A, TypeSemaine.B);
        choixSemaine.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeSemaine typeSemaine) {
                return switch (typeSemaine) {
                    case TOUTES -> "Toutes les semaines";
                    case A -> "Semaine A";
                    case B -> "Semaine B";
                };
            }

            @Override
            public TypeSemaine fromString(String texte) {
                return null;
            }
        });
        choixSemaine.setValue(creneauExistant != null ? creneauExistant.getTypeSemaine() : TypeSemaine.TOUTES);

        Set<UUID> fichiersCoches = new LinkedHashSet<>();
        Cours coursInitial;
        if (creneauExistant != null) {
            coursInitial = emploiDuTemps.trouverCours(creneauExistant.getCoursId())
                    .orElse(emploiDuTemps.getCours().get(0));
            fichiersCoches.addAll(creneauExistant.getFichiersSelectionnesIds());
            choixDebut.setValue(creneauExistant.getHeureDebut());
            choixFin.setValue(creneauExistant.getHeureFin());
            champSalle.setText(creneauExistant.getSalle());
            champDescription.setText(creneauExistant.getDescription());
        } else {
            coursInitial = emploiDuTemps.getCours().get(0);
            emploiDuTemps.fichiersVisibles(coursInitial).forEach(f -> fichiersCoches.add(f.getId()));
            choixDebut.setValue(heureDebutParDefaut);
            choixFin.setValue(heureFinParDefaut);
        }
        choixCours.setValue(coursInitial);

        // --- Bandeau d'en-tête coloré (couleur du cours), affiché sous la barre de titre native
        // de la fenêtre via dialogPane.setHeader(...) : jour + heure de début-fin, choix du cours
        // (intégré directement dans le bandeau) et salle. Se met à jour en direct quand ces
        // champs changent, plutôt que d'être un texte figé.
        Label labelJourHeure = new Label();
        labelJourHeure.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 11; -fx-font-weight: bold;");
        choixCours.setStyle("-fx-font-size: 20; -fx-font-weight: bold;"
                + " -fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 6;");
        choixCours.setMaxWidth(Double.MAX_VALUE);
        Label labelSalleBanniere = new Label();
        labelSalleBanniere.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13;");
        VBox banniere = new VBox(6, labelJourHeure, choixCours, labelSalleBanniere);
        banniere.setPadding(new Insets(20, 24, 20, 24));

        Runnable actualiserBanniere = () -> {
            Cours coursChoisi = choixCours.getValue();
            String couleur = coursChoisi != null && coursChoisi.getCouleur() != null
                    ? coursChoisi.getCouleur() : Couleurs.COURS_SANS_COULEUR;
            banniere.setStyle("-fx-background-color: " + couleur + ";");
            LocalTime debutAffiche = choixDebut.getValue() != null ? choixDebut.getValue() : heureDebutParDefaut;
            LocalTime finAffichee = choixFin.getValue() != null ? choixFin.getValue() : heureFinParDefaut;
            labelJourHeure.setText(NomsJours.nom(jour).toUpperCase(Locale.FRENCH)
                    + " · " + debutAffiche + " - " + finAffichee);
            String salle = champSalle.getText();
            labelSalleBanniere.setText(salle != null && !salle.isBlank() ? "Salle " + salle : "");
            labelSalleBanniere.setManaged(!labelSalleBanniere.getText().isEmpty());
            labelSalleBanniere.setVisible(!labelSalleBanniere.getText().isEmpty());
        };
        choixDebut.valueProperty().addListener((obs, ancien, nouveau) -> actualiserBanniere.run());
        choixFin.valueProperty().addListener((obs, ancien, nouveau) -> actualiserBanniere.run());
        champSalle.textProperty().addListener((obs, ancien, nouveau) -> actualiserBanniere.run());

        ObservableList<Fichier> tousLesFichiersDialogue = FXCollections.observableArrayList();
        FilteredList<Fichier> fichiersFiltresDialogue = new FilteredList<>(tousLesFichiersDialogue);

        Label libelleCompteSelection = new Label();
        libelleCompteSelection.setStyle(
                "-fx-text-fill: -color-accent-emphasis; -fx-font-weight: bold; -fx-font-size: 11;");
        Runnable actualiserCompteSelection = () -> libelleCompteSelection.setText(
                fichiersCoches.size() + (fichiersCoches.size() > 1 ? " sélectionnés" : " sélectionné"));

        ListView<Fichier> listeFichiers = new ListView<>();
        listeFichiers.setPrefHeight(220);
        listeFichiers.setCellFactory(vue -> new ListCell<>() {
            private final CheckBox caseCoche = new CheckBox();
            private final Label libelleNom = new Label();
            private final Label libelleChemin = new Label();
            private final VBox texteFichier = new VBox(1, libelleNom, libelleChemin);
            private final HBox conteneurTags = new HBox(4);
            private final Label libelleExtension = new Label();
            private final Region espaceurCellule = new Region();
            private final HBox ligneCellule = new HBox(8,
                    caseCoche, Icons.document(), texteFichier, espaceurCellule, conteneurTags, libelleExtension);

            {
                libelleNom.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
                libelleChemin.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10;");
                libelleExtension.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 10;");
                conteneurTags.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(espaceurCellule, Priority.ALWAYS);
                ligneCellule.setAlignment(Pos.CENTER_LEFT);
                ligneCellule.setPadding(new Insets(8, 10, 8, 10));
                caseCoche.setOnAction(e -> {
                    Fichier fichier = getItem();
                    if (fichier == null) {
                        return;
                    }
                    if (caseCoche.isSelected()) {
                        fichiersCoches.add(fichier.getId());
                    } else {
                        fichiersCoches.remove(fichier.getId());
                    }
                    appliquerStyleSelection(caseCoche.isSelected());
                    actualiserCompteSelection.run();
                });
            }

            private void appliquerStyleSelection(boolean selectionne) {
                ligneCellule.setStyle(selectionne
                        ? "-fx-background-color: -color-accent-subtle; -fx-background-radius: 6;"
                                + " -fx-border-color: -color-accent-emphasis; -fx-border-radius: 6; -fx-border-width: 1;"
                        : "-fx-background-color: -color-bg-subtle; -fx-background-radius: 6;"
                                + " -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-border-width: 1;");
            }

            @Override
            protected void updateItem(Fichier fichier, boolean vide) {
                super.updateItem(fichier, vide);
                if (vide || fichier == null) {
                    setGraphic(null);
                    return;
                }
                libelleNom.setText(fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                        ? fichier.getNomAffichage() : fichier.getChemin());
                libelleChemin.setText(fichier.getChemin() != null ? fichier.getChemin() : "");
                libelleChemin.setManaged(!libelleChemin.getText().isBlank());
                libelleChemin.setVisible(!libelleChemin.getText().isBlank());

                boolean selectionne = fichiersCoches.contains(fichier.getId());
                caseCoche.setSelected(selectionne);
                appliquerStyleSelection(selectionne);

                conteneurTags.getChildren().clear();
                for (String tag : fichier.getTags()) {
                    conteneurTags.getChildren().add(
                            TagPills.pastille(tag, emploiDuTemps.getParametres().couleurTag(tag)));
                }

                String extension = TypeFichier.extension(fichier.getChemin());
                libelleExtension.setText(extension != null ? extension : "");
                libelleExtension.setManaged(extension != null);
                libelleExtension.setVisible(extension != null);

                ligneCellule.getChildren().set(1, iconePourFichier(fichier.getChemin()));
                setGraphic(ligneCellule);
            }
        });
        listeFichiers.setItems(fichiersFiltresDialogue);
        tousLesFichiersDialogue.setAll(emploiDuTemps.fichiersVisibles(coursInitial));
        actualiserCompteSelection.run();

        TextField rechercheFichiers = new TextField();
        rechercheFichiers.setPromptText("Rechercher un fichier...");
        HBox.setHgrow(rechercheFichiers, Priority.ALWAYS);

        ComboBox<String> filtreTag = new ComboBox<>();
        filtreTag.getItems().add("Tous les tags");
        filtreTag.getItems().addAll(emploiDuTemps.getParametres().getTagsDisponibles());
        filtreTag.setValue("Tous les tags");

        Runnable actualiserFiltreFichiers = () -> fichiersFiltresDialogue.setPredicate(fichier -> {
            String recherche = rechercheFichiers.getText();
            boolean correspondRecherche = recherche == null || recherche.isBlank()
                    || fichier.toString().toLowerCase().contains(recherche.toLowerCase());
            String tagChoisi = filtreTag.getValue();
            boolean correspondTag = tagChoisi == null || tagChoisi.equals("Tous les tags")
                    || fichier.getTags().contains(tagChoisi);
            return correspondRecherche && correspondTag;
        });
        rechercheFichiers.textProperty().addListener((obs, ancien, nouveau) -> actualiserFiltreFichiers.run());
        filtreTag.setOnAction(e -> actualiserFiltreFichiers.run());
        actualiserFiltreFichiers.run();

        HBox ligneFiltresFichiers = new HBox(8, rechercheFichiers, filtreTag);

        choixCours.setOnAction(e -> {
            Cours coursChoisi = choixCours.getValue();
            fichiersCoches.clear();
            if (coursChoisi != null) {
                emploiDuTemps.fichiersVisibles(coursChoisi).forEach(f -> fichiersCoches.add(f.getId()));
            }
            tousLesFichiersDialogue.setAll(coursChoisi != null ? emploiDuTemps.fichiersVisibles(coursChoisi) : List.of());
            actualiserCompteSelection.run();
            actualiserBanniere.run();
        });

        Button boutonToutCocher = new Button("Tout cocher");
        boutonToutCocher.setOnAction(e -> {
            fichiersCoches.clear();
            listeFichiers.getItems().forEach(f -> fichiersCoches.add(f.getId()));
            listeFichiers.refresh();
            actualiserCompteSelection.run();
        });
        Button boutonToutDecocher = new Button("Tout décocher");
        boutonToutDecocher.setOnAction(e -> {
            fichiersCoches.clear();
            listeFichiers.refresh();
            actualiserCompteSelection.run();
        });
        HBox boutonsFichiers = new HBox(8, boutonToutCocher, boutonToutDecocher);

        // Ligne compacte d'édition (horaire/semaine/salle) sous la bannière : le cours se choisit
        // directement dans le bandeau coloré désormais, l'heure de début/fin y est aussi
        // reflétée mais reste éditable ici.
        HBox.setHgrow(champSalle, Priority.ALWAYS);
        champSalle.setMaxWidth(Double.MAX_VALUE);
        HBox ligneHoraireSemaine = new HBox(8, new Label("De"), choixDebut, new Label("à"), choixFin, choixSemaine);
        ligneHoraireSemaine.setAlignment(Pos.CENTER_LEFT);

        HBox ligneSalle = new HBox(8, new Label("Salle"), champSalle);
        ligneSalle.setAlignment(Pos.CENTER_LEFT);

        VBox ligneEditionCompacte = new VBox(6, ligneHoraireSemaine, ligneSalle);

        champDescription.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 6;"
                + " -fx-border-color: -color-border-default; -fx-border-radius: 6;");

        HBox ligneTitreFichiers = new HBox(labelSection("FICHIERS DU COURS"), espaceurExtensible(), libelleCompteSelection);
        ligneTitreFichiers.setAlignment(Pos.CENTER_LEFT);

        VBox formulaire = new VBox(14,
                ligneEditionCompacte,
                new Separator(),
                labelSection("NOTE DE SÉANCE"), champDescription,
                new Separator(),
                ligneTitreFichiers, ligneFiltresFichiers, listeFichiers, boutonsFichiers);
        formulaire.setPadding(new Insets(20, 24, 16, 24));

        ButtonType boutonValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonDupliquer = new ButtonType("Dupliquer", ButtonBar.ButtonData.LEFT);
        ButtonType boutonOuvrir = new ButtonType("▶ Ouvrir maintenant", ButtonBar.ButtonData.APPLY);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle(creneauExistant == null ? "Nouveau créneau" : "Modifier le créneau");
        dialogue.setResizable(true);
        dialogue.getDialogPane().setHeader(banniere);
        dialogue.getDialogPane().setContent(formulaire);
        dialogue.getDialogPane().setPrefWidth(520);
        if (creneauExistant != null) {
            dialogue.getDialogPane().getButtonTypes().addAll(boutonSupprimer, boutonDupliquer);
        }
        dialogue.getDialogPane().getButtonTypes().addAll(boutonOuvrir, boutonValider, ButtonType.CANCEL);

        Button noeudOuvrir = (Button) dialogue.getDialogPane().lookupButton(boutonOuvrir);
        if (noeudOuvrir != null) {
            noeudOuvrir.getStyleClass().add(Styles.ACCENT);
        }
        if (creneauExistant != null) {
            Button noeudSupprimer = (Button) dialogue.getDialogPane().lookupButton(boutonSupprimer);
            if (noeudSupprimer != null) {
                noeudSupprimer.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            }
        }
        // Pas de bouton "Annuler" visible (fermer la fenêtre a le même effet) : on garde quand
        // même un ButtonType.CANCEL dans le dialogue et on masque juste son bouton, car JavaFX ne
        // câble la croix native (fermeture de fenêtre) que s'il existe un bouton de type
        // CANCEL_CLOSE — sans lui, la croix ne fait plus rien du tout.
        Button noeudAnnuler = (Button) dialogue.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (noeudAnnuler != null) {
            noeudAnnuler.setVisible(false);
            noeudAnnuler.setManaged(false);
        }

        actualiserBanniere.run();

        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() == ButtonType.CANCEL) {
            return;
        }

        if (resultat.get() == boutonSupprimer) {
            // TODO UX : suppression immédiate. Rattrapable via Ctrl+Z (pileAnnuler) mais ce
            // raccourci n'est visible nulle part dans l'UI. Ajouter un retour visuel après
            // suppression (ex. message "Créneau supprimé - Ctrl+Z pour annuler").
            enregistrerAvantModification();
            emploiDuTemps.supprimerCreneau(creneauExistant.getId());
            notifierChangement();
            rafraichir();
            return;
        }

        Cours coursChoisi = choixCours.getValue();
        LocalTime debut = choixDebut.getValue();
        LocalTime fin = choixFin.getValue();
        // TODO UX : validation seulement à la soumission (clic sur "Valider"). Désactiver
        // préventivement le bouton Valider tant que cours/horaire ne sont pas valides plutôt
        // que d'afficher l'erreur après coup.
        if (coursChoisi == null || debut == null || fin == null || !fin.isAfter(debut)) {
            Alert erreur = new Alert(Alert.AlertType.ERROR,
                    "Choisis un cours et une plage horaire valide (fin après le début).");
            erreur.setTitle("Créneau invalide");
            erreur.setHeaderText(null);
            erreur.showAndWait();
            return;
        }
        enregistrerAvantModification();

        List<UUID> idsSelectionnes = emploiDuTemps.fichiersVisibles(coursChoisi).stream()
                .map(Fichier::getId)
                .filter(fichiersCoches::contains)
                .collect(Collectors.toList());

        Creneau creneau;
        if (creneauExistant == null || resultat.get() == boutonDupliquer) {
            creneau = emploiDuTemps.ajouterCreneau(jour, debut, fin, coursChoisi.getId());
        } else {
            creneau = creneauExistant;
            creneau.setJour(jour);
            creneau.setHeureDebut(debut);
            creneau.setHeureFin(fin);
            creneau.setCoursId(coursChoisi.getId());
        }
        creneau.setFichiersSelectionnesIds(idsSelectionnes);
        creneau.setSalle(videVersNull(champSalle.getText()));
        creneau.setDescription(videVersNull(champDescription.getText()));
        creneau.setTypeSemaine(choixSemaine.getValue());

        notifierChangement();
        rafraichir();

        if (resultat.get() == boutonOuvrir) {
            List<Fichier> fichiersAOuvrir = emploiDuTemps.fichiersPourCreneau(creneau);
            ouvrirFichiers(fichiersAOuvrir);
        }
    }

    // TODO UX : aucun feedback visuel en cas de succès (seul un échec partiel affiche une
    // alerte). L'utilisateur doit remarquer que l'application externe s'est ouverte de son côté.
    private void ouvrirFichiers(List<Fichier> fichiers) {
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

    private List<LocalTime> genererLimites() {
        List<LocalTime> limites = new ArrayList<>();
        LocalTime t = heureDebutGrille;
        while (!t.isAfter(heureFinGrille)) {
            limites.add(t);
            t = t.plusMinutes(pasMinutes);
        }
        return limites;
    }

    /**
     * @return {@code options} augmentée de {@code valeur} si elle n'y figure pas déjà (triée),
     * pour garantir qu'un horaire de bloc non aligné sur {@link #pasMinutes} reste sélectionnable.
     */
    private static List<LocalTime> avecValeur(List<LocalTime> options, LocalTime valeur) {
        if (valeur == null || options.contains(valeur)) {
            return options;
        }
        List<LocalTime> resultat = new ArrayList<>(options);
        resultat.add(valeur);
        resultat.sort(LocalTime::compareTo);
        return resultat;
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

    private static String videVersNull(String texte) {
        return texte == null || texte.isBlank() ? null : texte;
    }

    private static int clamp(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }
}
