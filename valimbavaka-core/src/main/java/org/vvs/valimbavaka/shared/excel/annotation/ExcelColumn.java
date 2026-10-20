package org.vvs.valimbavaka.shared.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Colonne exportée ; seuls les champs annotés (y compris ceux des classes parentes) sont exportés. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelColumn {

    String header();

    int order() default Integer.MAX_VALUE;

    /** Expression SpEL évaluée sur l'élément, ex. {@code "role?.name"}. Vide = valeur du champ. */
    String expression() default "";

    /** Format de cellule Excel, ex. {@code "#,##0.00"} ou {@code "dd/mm/yyyy"}. Vide = format par défaut du type. */
    String format() default "";

    /** Largeur minimale, en caractères. */
    int width() default 20;
}
