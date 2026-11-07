package org.vvs.valimbavaka.feature.shooting.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingSummaryRDto;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingArticleQuantity;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingRepository;
import org.vvs.valimbavaka.feature.shooting.service.ShootingSummaryService;

@Service
@Transactional(readOnly = true)
public class ShootingSummaryServiceImpl implements ShootingSummaryService {

    private static final int MONEY_SCALE = 2;

    private final ShootingRepository shootingRepository;
    private final ArticleService articleService;
    private final Clock clock;

    ShootingSummaryServiceImpl(ShootingRepository shootingRepository, ArticleService articleService, Clock clock) {
        this.shootingRepository = shootingRepository;
        this.articleService = articleService;
        this.clock = clock;
    }

    /** Same late rule as {@code Shooting.isLateAt}: still out and expected return strictly before now. */
    @Override
    public ShootingSummaryRDto getSummary() {
        var now = clock.instant();
        var outCount = shootingRepository.countByStatus(ShootingStatus.OUT);
        var lateCount = shootingRepository.countByStatusAndExpectedReturnAtBefore(ShootingStatus.OUT, now);
        var quantities = shootingRepository.quantitiesByArticle(ShootingStatus.OUT);
        var costs = costsOf(quantities);
        var value = BigDecimal.ZERO;
        var unvalued = BigDecimal.ZERO;
        for (var line : quantities) {
            var cost = costs.get(line.articleId());
            if (cost == null) {
                unvalued = unvalued.add(line.quantity());
            } else {
                value = value.add(line.quantity().multiply(cost));
            }
        }
        return new ShootingSummaryRDto(
                outCount, lateCount, value.setScale(MONEY_SCALE, RoundingMode.HALF_UP), unvalued);
    }

    /** Purchase cost of the articles still out; an article without cost is absent (not valued). */
    private Map<UUID, BigDecimal> costsOf(List<ShootingArticleQuantity> quantities) {
        var articleIds =
                quantities.stream().map(ShootingArticleQuantity::articleId).collect(Collectors.toSet());
        return articleIds.isEmpty() ? Map.of() : articleService.getCosts(articleIds);
    }
}
