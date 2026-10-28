package org.vvs.valimbavaka.feature.article.domain;

import java.util.Set;

/**
 * Role an article can play in a production batch (ART-04). Derived from the article type only: no parallel flag.
 */
public enum ProductionUsage {
    /** Consumed by a batch: raw materials (MP) and semi-finished products (SF). */
    INPUT(Set.of(ArticleType.RAW_MATERIAL, ArticleType.SEMI_FINISHED)),
    /** Produced by a batch: finished products. */
    OUTPUT(Set.of(ArticleType.FINISHED));

    private final Set<ArticleType> types;

    ProductionUsage(Set<ArticleType> types) {
        this.types = types;
    }

    /** Immutable set of the article types allowed for this usage. */
    public Set<ArticleType> types() {
        return types;
    }

    public boolean accepts(ArticleType type) {
        return types.contains(type);
    }
}
