package org.vvs.valimbavaka.shared.excel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/** Colonne lue sur un champ {@link ExcelColumn} ; l'expression SpEL est analysée une seule fois par type. */
record ExcelColumnDefinition(String header, int width, String format, Expression expression) {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    // Lecture seule + méthodes d'instance : aucun accès aux types, constructeurs ni beans
    private static final EvaluationContext CONTEXT = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withInstanceMethods()
            .build();

    // Chemin interrompu par un null (ex. "role.name" avec role null) : cellule vide, pas d'erreur
    private static final Set<SpelMessage> NULL_IN_PATH = Set.of(
            SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED);

    /** Champs annotés, ceux des classes parentes d'abord, puis triés (de façon stable) par {@code order}. */
    static List<ExcelColumnDefinition> scan(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            fields.addAll(0, Arrays.asList(current.getDeclaredFields()));
        }
        return fields.stream()
                .filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                .sorted(Comparator.comparingInt(
                        field -> field.getAnnotation(ExcelColumn.class).order()))
                .map(ExcelColumnDefinition::of)
                .toList();
    }

    @Nullable
    Object valueOf(Object item) {
        try {
            return expression.getValue(CONTEXT, item);
        } catch (SpelEvaluationException e) {
            if (NULL_IN_PATH.contains(e.getMessageCode())) {
                return null;
            }
            throw e;
        }
    }

    private static ExcelColumnDefinition of(Field field) {
        ExcelColumn column = field.getAnnotation(ExcelColumn.class);
        String expression = column.expression().isBlank() ? field.getName() : column.expression();
        return new ExcelColumnDefinition(
                column.header(), column.width(), column.format(), PARSER.parseExpression(expression));
    }
}
