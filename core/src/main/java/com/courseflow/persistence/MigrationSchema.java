package com.courseflow.persistence;

import com.courseflow.model.EmploiDuTemps;

/**
 * Enchaîne les migrations nécessaires pour amener un {@link EmploiDuTemps} tout juste
 * désérialisé à {@link EmploiDuTemps#VERSION_ACTUELLE}, une étape à la fois.
 *
 * <p>Chaque cas de {@link #migrerUneEtape} a la charge de transformer les données à sa version
 * d'arrivée <em>et</em> d'incrémenter {@code version} ; {@link #appliquer} boucle dessus jusqu'à
 * la version courante, pour qu'un fichier de n'importe quelle version antérieure se charge sans
 * code de migration cumulatif à réécrire à chaque nouvelle version du schéma.
 *
 * <p>La version 0 désigne les données antérieures à l'introduction de ce champ (aucune clé
 * "version" dans le JSON) ; la migration 0 -> 1 ne transforme aucune donnée, elle ne fait
 * qu'assigner la version explicite.
 */
final class MigrationSchema {

    private MigrationSchema() {
    }

    static void appliquer(EmploiDuTemps emploiDuTemps) {
        while (emploiDuTemps.getVersion() < EmploiDuTemps.VERSION_ACTUELLE) {
            migrerUneEtape(emploiDuTemps);
        }
    }

    private static void migrerUneEtape(EmploiDuTemps emploiDuTemps) {
        switch (emploiDuTemps.getVersion()) {
            case 0 -> emploiDuTemps.setVersion(1);
            default -> throw new IllegalStateException(
                    "Version de schéma inconnue ou déjà à jour : " + emploiDuTemps.getVersion());
        }
    }
}
