package com.courseflow.io;

import com.courseflow.model.Fichier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OuvreurFichiersTest {

    @Test
    void estUrlReconnaitUnSchemaMaisPasUnCheminLocal() {
        assertTrue(OuvreurFichiers.estUrl("https://example.com/cours.pdf"));
        assertTrue(OuvreurFichiers.estUrl("http://example.com"));
        assertFalse(OuvreurFichiers.estUrl("/docs/maths/cours.pdf"));
        assertFalse(OuvreurFichiers.estUrl(null));
    }

    @Test
    void unCheminNulEstSignaleCommeInvalideSansTenterUneOuverture() {
        Fichier fichier = new Fichier(null, "Fichier sans chemin");

        List<String> echecs = OuvreurFichiers.ouvrir(List.of(fichier));

        assertEquals(1, echecs.size());
        assertTrue(echecs.get(0).contains("chemin invalide"));
    }

    @Test
    void unFichierManquantSurLeDisqueEstSignaleClairement(@TempDir Path repertoireTemp) {
        String cheminManquant = repertoireTemp.resolve("n-existe-pas.pdf").toString();
        Fichier fichier = new Fichier(cheminManquant, "Exercices");

        List<String> echecs = OuvreurFichiers.ouvrir(List.of(fichier));

        assertEquals(1, echecs.size());
        assertTrue(echecs.get(0).contains("Exercices"));
        assertTrue(echecs.get(0).contains("introuvable"));
        assertTrue(echecs.get(0).contains(cheminManquant));
    }
}
