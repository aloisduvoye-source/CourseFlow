package com.teacherflow.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeFichierTest {

    @Test
    void reconnaitLesExtensionsImageUsuellesQuelleQueSoitLaCasse() {
        assertTrue(TypeFichier.estImage("/docs/maths/schema.png"));
        assertTrue(TypeFichier.estImage("photo.JPG"));
        assertTrue(TypeFichier.estImage("https://example.com/diagramme.webp"));
        assertTrue(TypeFichier.estImage("scan.jpeg"));
    }

    @Test
    void rejetteLesNonImagesEtLesCheminsSansExtension() {
        assertFalse(TypeFichier.estImage("/docs/maths/cours.pdf"));
        assertFalse(TypeFichier.estImage("README"));
        assertFalse(TypeFichier.estImage("archive."));
        assertFalse(TypeFichier.estImage(null));
    }
}
