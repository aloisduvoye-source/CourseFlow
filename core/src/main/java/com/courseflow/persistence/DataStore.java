package com.courseflow.persistence;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.courseflow.model.EmploiDuTemps;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Chargement et sauvegarde de l'{@link EmploiDuTemps} dans un fichier JSON local.
 * Emplacement par défaut : {@code ~/.courseflow/data.json}.
 *
 * <p>L'écriture est atomique (fichier temporaire puis renommage) pour qu'une interruption en
 * cours d'écriture ne laisse jamais un {@code data.json} tronqué, et chaque sauvegarde fait
 * tourner quelques copies horodatées ({@code data.json.bak1}..{@code bak3}). Un fichier présent
 * mais illisible est mis de côté plutôt qu'ignoré silencieusement (voir
 * {@link DonneesIllisiblesException}). Un fichier d'une version de schéma antérieure
 * ({@link EmploiDuTemps#getVersion()}) est migré en mémoire à la lecture (voir
 * {@link MigrationSchema}) ; la version courante n'est réécrite sur disque qu'à la prochaine
 * {@link #sauvegarder(EmploiDuTemps)}.
 */
public class DataStore {

    public static final Path EMPLACEMENT_PAR_DEFAUT =
            Path.of(System.getProperty("user.home"), ".courseflow", "data.json");

    /** Nombre de copies de secours conservées ({@code .bak1} = la plus récente). */
    private static final int NOMBRE_SAUVEGARDES = 3;

    private static final DateTimeFormatter HORODATAGE =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path fichierDonnees;
    private final ObjectMapper mapper;

    public DataStore() {
        this(EMPLACEMENT_PAR_DEFAUT);
    }

    public DataStore(Path fichierDonnees) {
        this.fichierDonnees = fichierDonnees;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                // Tolère qu'un fichier écrit par une version plus récente contienne des champs
                // encore inconnus ici : mieux vaut les ignorer que refuser d'ouvrir le fichier.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Charge l'emploi du temps depuis le disque, ou renvoie un emploi du temps vide si aucune
     * donnée n'a encore été sauvegardée.
     *
     * @throws DonneesIllisiblesException si le fichier existe mais n'a pas pu être désérialisé ;
     *                                    il a alors été renommé (mis en quarantaine) pour ne pas
     *                                    être écrasé par une sauvegarde ultérieure
     * @throws IOException                en cas d'erreur d'accès disque (le fichier, s'il existe,
     *                                    est laissé intact)
     */
    public EmploiDuTemps charger() throws IOException {
        if (!Files.exists(fichierDonnees)) {
            return new EmploiDuTemps();
        }
        try {
            JsonNode racine = mapper.readTree(fichierDonnees.toFile());
            EmploiDuTemps emploiDuTemps = mapper.treeToValue(racine, EmploiDuTemps.class);
            if (!racine.has("version")) {
                // Fichier écrit avant l'introduction du champ : forcer la version 0 plutôt que de
                // laisser la valeur par défaut de la classe (VERSION_ACTUELLE) masquer le fait
                // qu'aucune migration n'a encore été appliquée à ces données.
                emploiDuTemps.setVersion(0);
            }
            MigrationSchema.appliquer(emploiDuTemps);
            return emploiDuTemps;
        } catch (JacksonException e) {
            Path quarantaine = fichierDonnees.resolveSibling(
                    fichierDonnees.getFileName() + ".corrompu-" + LocalDateTime.now().format(HORODATAGE));
            Files.move(fichierDonnees, quarantaine, StandardCopyOption.REPLACE_EXISTING);
            throw new DonneesIllisiblesException(quarantaine, e);
        }
    }

    public void sauvegarder(EmploiDuTemps emploiDuTemps) throws IOException {
        Files.createDirectories(fichierDonnees.getParent());
        rotationSauvegardes();

        Path temporaire = fichierDonnees.resolveSibling(fichierDonnees.getFileName() + ".tmp");
        mapper.writeValue(temporaire.toFile(), emploiDuTemps);
        try {
            Files.move(temporaire, fichierDonnees,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Certains systèmes de fichiers (ou un déplacement inter-volumes) ne le permettent
            // pas : on retombe sur un renommage simple, moins sûr mais toujours mieux qu'écrire
            // directement dans le fichier cible.
            Files.move(temporaire, fichierDonnees, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Décale {@code data.json} et ses copies : {@code data.json -> .bak1 -> .bak2 -> .bak3}. */
    private void rotationSauvegardes() throws IOException {
        if (!Files.exists(fichierDonnees)) {
            return;
        }
        for (int i = NOMBRE_SAUVEGARDES; i >= 2; i--) {
            Path source = fichierSauvegarde(i - 1);
            if (Files.exists(source)) {
                Files.copy(source, fichierSauvegarde(i), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.copy(fichierDonnees, fichierSauvegarde(1), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path fichierSauvegarde(int index) {
        return fichierDonnees.resolveSibling(fichierDonnees.getFileName() + ".bak" + index);
    }

    public Path getFichierDonnees() {
        return fichierDonnees;
    }
}
