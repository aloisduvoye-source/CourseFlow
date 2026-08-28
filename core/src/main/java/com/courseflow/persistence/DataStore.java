package com.courseflow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.courseflow.model.EmploiDuTemps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Chargement et sauvegarde de l'{@link EmploiDuTemps} dans un fichier JSON local.
 * Emplacement par défaut : {@code ~/.courseflow/data.json}.
 */
public class DataStore {

    public static final Path EMPLACEMENT_PAR_DEFAUT =
            Path.of(System.getProperty("user.home"), ".courseflow", "data.json");

    private final Path fichierDonnees;
    private final ObjectMapper mapper;

    public DataStore() {
        this(EMPLACEMENT_PAR_DEFAUT);
    }

    public DataStore(Path fichierDonnees) {
        this.fichierDonnees = fichierDonnees;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Charge l'emploi du temps depuis le disque, ou renvoie un emploi du temps vide
     * si aucune donnée n'a encore été sauvegardée.
     */
    public EmploiDuTemps charger() throws IOException {
        if (!Files.exists(fichierDonnees)) {
            return new EmploiDuTemps();
        }
        return mapper.readValue(fichierDonnees.toFile(), EmploiDuTemps.class);
    }

    public void sauvegarder(EmploiDuTemps emploiDuTemps) throws IOException {
        Files.createDirectories(fichierDonnees.getParent());
        mapper.writeValue(fichierDonnees.toFile(), emploiDuTemps);
    }

    public Path getFichierDonnees() {
        return fichierDonnees;
    }
}
