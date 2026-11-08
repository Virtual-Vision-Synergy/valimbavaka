package org.vvs.valimbavaka.shared.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sur une méthode GET : enregistre les routes jumelles {@code <chemin>/export} (xlsx) et {@code <chemin>/export/pdf}
 * (PDF tabulaire) qui renvoient le même résultat, avec les mêmes paramètres et la même autorisation
 * ({@code @HasPermission}) que la route d'origine.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {

    /** Libellé libre de l'objet exporté, transmis tel quel dans {@code ExcelExportedEvent}. */
    String target() default "";

    /** Nom du fichier téléchargé, sans extension (.xlsx ou .pdf est ajouté). Vide = nom du DTO. */
    String filename() default "";

    /** Nom de l'onglet Excel (31 caractères max). Vide = nom du DTO. */
    String sheet() default "";

    /** Titre imprimé en tête du PDF. Vide = nom de l'onglet, sinon nom du DTO. */
    String title() default "";

    /** {@code false} : pas de route {@code <chemin>/export/pdf}, seul le xlsx est proposé. */
    boolean pdf() default true;
}
