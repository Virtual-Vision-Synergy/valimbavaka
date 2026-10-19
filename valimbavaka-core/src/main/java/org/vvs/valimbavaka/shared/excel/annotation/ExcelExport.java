package org.vvs.valimbavaka.shared.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sur une méthode GET : enregistre la route jumelle {@code <chemin>/export} qui renvoie le même résultat en xlsx,
 * avec les mêmes paramètres et la même autorisation ({@code @HasPermission}) que la route d'origine.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {

    /** Libellé libre de l'objet exporté, transmis tel quel dans {@code ExcelExportedEvent}. */
    String target() default "";

    /** Nom du fichier téléchargé (avec ou sans .xlsx). Vide = nom du DTO. */
    String filename() default "";

    /** Nom de l'onglet Excel (31 caractères max). Vide = nom du DTO. */
    String sheet() default "";
}
