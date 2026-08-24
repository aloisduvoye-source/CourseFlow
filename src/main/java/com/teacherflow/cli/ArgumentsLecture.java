package com.teacherflow.cli;

import com.teacherflow.util.NomsJours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Analyse des arguments de la commande {@code lecture} : une sous-commande optionnelle
 * ({@code slot}, {@code slots}, {@code schedule}, {@code courses}, {@code course},
 * {@code open-file} ; son absence signifie "ouvrir les fichiers du créneau ciblé"), suivie
 * d'options communes {@code --next}/{@code --previous}, {@code --date}/{@code --day} (jour
 * ciblé, exclusifs entre eux), {@code --time}, et des options propres à certaines
 * sous-commandes ({@code --course}, {@code --file}, {@code --missing-info}).
 */
public final class ArgumentsLecture {

    public enum Commande { OUVRIR, SLOT, SLOTS, SCHEDULE, COURSES, COURSE, OPEN_FILE }

    private final Commande commande;
    private final LocalDate date;
    private final LocalTime heure;
    private final boolean suivant;
    private final boolean precedent;
    private final boolean missingInfo;
    private final String nomCours;
    private final String nomFichier;

    private ArgumentsLecture(Commande commande, LocalDate date, LocalTime heure, boolean suivant,
            boolean precedent, boolean missingInfo, String nomCours, String nomFichier) {
        this.commande = commande;
        this.date = date;
        this.heure = heure;
        this.suivant = suivant;
        this.precedent = precedent;
        this.missingInfo = missingInfo;
        this.nomCours = nomCours;
        this.nomFichier = nomFichier;
    }

    public Commande getCommande() {
        return commande;
    }

    public DayOfWeek getJour() {
        return date.getDayOfWeek();
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public boolean isSuivant() {
        return suivant;
    }

    public boolean isPrecedent() {
        return precedent;
    }

    public boolean isMissingInfo() {
        return missingInfo;
    }

    public String getNomCours() {
        return nomCours;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public static ArgumentsLecture analyser(String[] args, LocalDate dateParDefaut, LocalTime heureParDefaut) {
        int index = 0;
        Commande commande = Commande.OUVRIR;
        String nomCours = null;

        if (args.length > 0 && !args[0].startsWith("-")) {
            commande = switch (args[0]) {
                case "slot" -> Commande.SLOT;
                case "slots" -> Commande.SLOTS;
                case "schedule" -> Commande.SCHEDULE;
                case "courses" -> Commande.COURSES;
                case "course" -> Commande.COURSE;
                case "open-file" -> Commande.OPEN_FILE;
                default -> throw new IllegalArgumentException("Commande inconnue : \"" + args[0] + "\".");
            };
            index = 1;
            if (commande == Commande.COURSE) {
                if (index >= args.length || args[index].startsWith("-")) {
                    throw new IllegalArgumentException(
                            "La commande \"course\" attend le nom du cours en argument.");
                }
                nomCours = args[index];
                index++;
            }
        }

        LocalDate date = dateParDefaut;
        LocalTime heure = heureParDefaut;
        boolean jourSpecifie = false;
        boolean dateSpecifiee = false;
        boolean heureSpecifiee = false;
        boolean suivant = false;
        boolean precedent = false;
        boolean missingInfo = false;
        String nomCoursOption = null;
        String nomFichier = null;

        while (index < args.length) {
            String option = args[index];
            switch (option) {
                case "--next" -> {
                    suivant = true;
                    index += 1;
                }
                case "--previous" -> {
                    precedent = true;
                    index += 1;
                }
                case "--day" -> {
                    String valeur = valeurSuivante(args, index, option);
                    DayOfWeek jourDemande = NomsJours.depuisNom(valeur).orElseThrow(() -> new IllegalArgumentException(
                            "Jour inconnu : \"" + valeur + "\" (attendu : lundi, mardi, mercredi, jeudi, "
                                    + "vendredi, samedi ou dimanche)."));
                    date = date.with(jourDemande);
                    jourSpecifie = true;
                    index += 2;
                }
                case "--date" -> {
                    String valeur = valeurSuivante(args, index, option);
                    try {
                        date = LocalDate.parse(valeur);
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException(
                                "Date invalide : \"" + valeur + "\" (format attendu : AAAA-MM-JJ).");
                    }
                    dateSpecifiee = true;
                    index += 2;
                }
                case "--time" -> {
                    String valeur = valeurSuivante(args, index, option);
                    try {
                        heure = LocalTime.parse(valeur);
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException(
                                "Heure invalide : \"" + valeur + "\" (format attendu : HH:mm).");
                    }
                    heureSpecifiee = true;
                    index += 2;
                }
                case "--course" -> {
                    nomCoursOption = valeurSuivante(args, index, option);
                    index += 2;
                }
                case "--file" -> {
                    nomFichier = valeurSuivante(args, index, option);
                    index += 2;
                }
                case "--missing-info" -> {
                    missingInfo = true;
                    index += 1;
                }
                default -> throw new IllegalArgumentException("Option inconnue : \"" + option + "\".");
            }
        }

        if (jourSpecifie && dateSpecifiee) {
            throw new IllegalArgumentException("Les options --day et --date sont incompatibles.");
        }
        if (suivant && precedent) {
            throw new IllegalArgumentException("Les options --next et --previous sont incompatibles.");
        }
        boolean jourDonne = jourSpecifie || dateSpecifiee;

        switch (commande) {
            case OUVRIR, SLOT -> {
                interdireSiPresent(missingInfo, "--missing-info");
                interdireSiPresent(nomCoursOption != null, "--course");
                interdireSiPresent(nomFichier != null, "--file");
                if (jourDonne && !heureSpecifiee && !suivant && !precedent) {
                    throw new IllegalArgumentException(
                            "--day/--date nécessite --time (ou utilisez \"lecture slots\" pour lister "
                                    + "les créneaux d'un jour).");
                }
            }
            case SLOTS -> {
                interdireSiPresent(heureSpecifiee, "--time");
                interdireSiPresent(suivant, "--next");
                interdireSiPresent(precedent, "--previous");
                interdireSiPresent(missingInfo, "--missing-info");
                interdireSiPresent(nomCoursOption != null, "--course");
                interdireSiPresent(nomFichier != null, "--file");
            }
            case SCHEDULE -> {
                interdireSiPresent(jourDonne, "--day/--date");
                interdireSiPresent(heureSpecifiee, "--time");
                interdireSiPresent(suivant, "--next");
                interdireSiPresent(precedent, "--previous");
                interdireSiPresent(missingInfo, "--missing-info");
                interdireSiPresent(nomCoursOption != null, "--course");
                interdireSiPresent(nomFichier != null, "--file");
            }
            case COURSES -> {
                interdireSiPresent(jourDonne, "--day/--date");
                interdireSiPresent(heureSpecifiee, "--time");
                interdireSiPresent(suivant, "--next");
                interdireSiPresent(precedent, "--previous");
                interdireSiPresent(nomCoursOption != null, "--course");
                interdireSiPresent(nomFichier != null, "--file");
            }
            case COURSE -> {
                interdireSiPresent(jourDonne, "--day/--date");
                interdireSiPresent(heureSpecifiee, "--time");
                interdireSiPresent(suivant, "--next");
                interdireSiPresent(precedent, "--previous");
                interdireSiPresent(missingInfo, "--missing-info");
                interdireSiPresent(nomCoursOption != null, "--course");
                interdireSiPresent(nomFichier != null, "--file");
            }
            case OPEN_FILE -> {
                interdireSiPresent(suivant, "--next");
                interdireSiPresent(precedent, "--previous");
                interdireSiPresent(missingInfo, "--missing-info");
                if (nomFichier == null) {
                    throw new IllegalArgumentException("La commande \"open-file\" nécessite --file.");
                }
                if (nomCoursOption != null && jourDonne) {
                    throw new IllegalArgumentException("--course et --day/--date sont incompatibles.");
                }
                if (nomCoursOption == null && !jourDonne) {
                    throw new IllegalArgumentException(
                            "La commande \"open-file\" nécessite --course, ou --day/--date accompagné de --time.");
                }
                if (jourDonne && !heureSpecifiee) {
                    throw new IllegalArgumentException("--day/--date nécessite --time pour \"open-file\".");
                }
                nomCours = nomCoursOption;
            }
        }

        return new ArgumentsLecture(commande, date, heure, suivant, precedent, missingInfo, nomCours, nomFichier);
    }

    private static void interdireSiPresent(boolean present, String option) {
        if (present) {
            throw new IllegalArgumentException(
                    "L'option " + option + " n'est pas applicable à cette commande.");
        }
    }

    private static String valeurSuivante(String[] args, int index, String option) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException("L'option " + option + " nécessite une valeur.");
        }
        return args[index + 1];
    }
}
