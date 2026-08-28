package com.courseflow.util;

import java.util.Locale;
import java.util.Set;

/**
 * Détection du type d'un fichier à partir de son chemin/URL, par extension. Reste dans le
 * module {@code core} (zéro dépendance graphique) : ne fait que de la détection, le rendu
 * d'aperçu/icône (JavaFX) vit dans {@code app}.
 */
public final class TypeFichier {

    private static final Set<String> EXTENSIONS_IMAGE =
            Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final Set<String> EXTENSIONS_TEXTE =
            Set.of("doc", "docx", "odt", "txt", "rtf");
    private static final Set<String> EXTENSIONS_PRESENTATION =
            Set.of("ppt", "pptx", "odp");

    private TypeFichier() {
    }

    /**
     * @return l'extension du chemin/URL en minuscules (sans le point), ou {@code null} s'il n'y
     * en a pas.
     */
    public static String extension(String chemin) {
        if (chemin == null) {
            return null;
        }
        int point = chemin.lastIndexOf('.');
        if (point < 0 || point == chemin.length() - 1) {
            return null;
        }
        return chemin.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * @return true si le chemin/URL se termine par une extension d'image usuelle, pour laquelle
     * un aperçu miniature peut être affiché sans dépendance supplémentaire (JavaFX sait charger
     * ces formats nativement).
     */
    public static boolean estImage(String chemin) {
        String extension = extension(chemin);
        return extension != null && EXTENSIONS_IMAGE.contains(extension);
    }

    /**
     * @return true pour un document texte usuel (Word/LibreOffice Writer/texte brut).
     */
    public static boolean estDocumentTexte(String chemin) {
        String extension = extension(chemin);
        return extension != null && EXTENSIONS_TEXTE.contains(extension);
    }

    /**
     * @return true pour une présentation usuelle (PowerPoint/LibreOffice Impress).
     */
    public static boolean estPresentation(String chemin) {
        String extension = extension(chemin);
        return extension != null && EXTENSIONS_PRESENTATION.contains(extension);
    }
}
