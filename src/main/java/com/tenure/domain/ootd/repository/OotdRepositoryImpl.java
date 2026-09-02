package com.tenure.domain.ootd.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tenure.domain.item.enums.ItemStatus;
import com.tenure.domain.ootd.entity.Ootd;
import com.tenure.domain.ootd.enums.OotdPublicationStatus;
import com.tenure.domain.search.dto.request.OotdSearchCondition;
import com.tenure.domain.search.enums.ItemStatusFilter;
import com.tenure.domain.search.enums.SearchSortType;
import com.tenure.domain.tag.entity.QOotdTag;
import com.tenure.domain.tag.enums.TagStatus;
import com.tenure.domain.user.enums.UserGender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.tenure.domain.ootd.entity.QOotd.ootd;
import static com.tenure.domain.tag.entity.QOotdTag.ootdTag;
import static com.tenure.domain.user.entity.QUser.user;


@RequiredArgsConstructor
public class OotdRepositoryImpl implements OotdRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Ootd> searchOotd(String keyword, OotdSearchCondition condition, Pageable pageable) {

        int pageSize = pageable.getPageSize();

        List<Ootd> content = queryFactory
                .select(ootd)
                .from(ootd)
                .leftJoin(ootd.owner, user).fetchJoin()
                .where(
                        publicationStatus(OotdPublicationStatus.ACTIVE),
                        genderEq(condition.getGender()),
                        heightGoe(condition.getHeightMin()),
                        heightLoe(condition.getHeightMax()),
                        weightGoe(condition.getWeightMin()),
                        weightLoe(condition.getWeightMax()),
                        categoryAndKeywordIn(keyword, condition.getCategoryIds()),
                        itemStatusEq(condition.getItemStatusFilter()),
                        cursorConditon(condition) // 동적 커서 조건
                )
                .orderBy(getOrderSpecifiers(condition.getSort()))
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageSize) {
            content.remove(pageSize);
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Long searchOotdsTotalCount(String keyword, OotdSearchCondition condition, Pageable pageable) {
        return queryFactory
                .select(ootd.count())
                .from(ootd)
                .where(
                        publicationStatus(OotdPublicationStatus.ACTIVE),
                        genderEq(condition.getGender()),
                        heightGoe(condition.getHeightMin()),
                        heightLoe(condition.getHeightMax()),
                        weightGoe(condition.getWeightMin()),
                        weightLoe(condition.getWeightMax()),
                        categoryAndKeywordIn(keyword, condition.getCategoryIds()),
                        itemStatusEq(condition.getItemStatusFilter())

                )
                .fetchOne();
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(SearchSortType sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        SearchSortType sortType = (sort != null) ? sort : SearchSortType.LATEST;

        switch (sortType) {
            case HEART -> orders.add(ootd.heartCount.desc());
            case SAVE  -> orders.add(ootd.saveCount.desc());
            case VIEW  -> orders.add(ootd.viewCount.desc());
            case LATEST -> orders.add(ootd.createdAt.desc());
        }

        orders.add(ootd.id.desc());

        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanExpression cursorConditon(OotdSearchCondition condition) {
        SearchSortType sort = condition.getSort() != null ? condition.getSort() : SearchSortType.LATEST;

        if (sort == SearchSortType.LATEST) {
            if (condition.getCursor() == null || condition.getCursorId() == null) return null;
            return ootd.createdAt.lt(condition.getCursor())
                    .or(ootd.createdAt.eq(condition.getCursor()).and(ootd.id.lt(condition.getCursorId())));
        }

        if (condition.getCursorValue() == null || condition.getCursorId() == null) return null;

        NumberPath<Integer> targetPath = switch (sort) {
            case HEART -> ootd.heartCount;
            case SAVE -> ootd.saveCount;
            case VIEW -> ootd.viewCount;
            default -> ootd.viewCount;
        };

        return targetPath.lt(condition.getCursorValue())
                .or(targetPath.eq(condition.getCursorValue()).and(ootd.id.lt(condition.getCursorId())));
    }

    private BooleanExpression itemStatusEq(ItemStatusFilter itemStatusFilter) {
        if (itemStatusFilter == null) return null;

        if (itemStatusFilter == ItemStatusFilter.ON_SALE_ONLY) {
            QOotdTag ot2 = new QOotdTag("ot2");
            QOotdTag ot3 = new QOotdTag("ot3");


            return ootd.id.in(
                    JPAExpressions
                            .select(ot2.ootd.id)
                            .from(ot2)
                            .where(
                                    ot2.item.isNotNull(),
                                    ot2.status.eq(TagStatus.CONFIRMED),
                                    ot2.item.itemStatus.eq(ItemStatus.ON_SALE)
                            )
            ).and(
                    ootd.id.notIn(
                            JPAExpressions
                                    .select(ot3.ootd.id)
                                    .from(ot3)
                                    .where(
                                            ot3.item.isNotNull(),
                                            ot3.status.eq(TagStatus.CONFIRMED),
                                            ot3.item.itemStatus.ne(ItemStatus.ON_SALE)
                                    )
                    )
            );
        } else if (itemStatusFilter == ItemStatusFilter.ON_SALE_INCLUDED) {
            QOotdTag ot2 = new QOotdTag("ot2");
            return ootd.id.in(
                    JPAExpressions
                            .select(ot2.ootd.id)
                            .from(ot2)
                            .where(
                                    ot2.item.isNotNull(),
                                    ot2.status.eq(TagStatus.CONFIRMED),
                                    ot2.item.itemStatus.eq(ItemStatus.ON_SALE)
                            )
            );
        }

        return null;
    }

    private BooleanExpression categoryAndKeywordIn(String keyword, List<Long> categoryIds) {
        if (!StringUtils.hasText(keyword) && (categoryIds == null || categoryIds.isEmpty())) {
            return null;
        }

        return ootd.id.in(
                JPAExpressions
                        .select(ootdTag.ootd.id)
                        .from(ootdTag)
                        .where(
                                ootdTag.item.isNotNull(),
                                ootdTag.status.eq(TagStatus.CONFIRMED),
                                keywordContain(keyword),
                                categoryIn(categoryIds)
                        )
        );
    }

    private BooleanExpression categoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;

        return ootdTag.item.category.id.in(categoryIds)
                .or(ootdTag.item.category.parent.id.in(categoryIds));
    }

    private BooleanExpression keywordContain(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        return ootdTag.item.itemName.containsIgnoreCase(keyword)
                .or(ootdTag.item.brandName.containsIgnoreCase(keyword));
    }

    private BooleanExpression weightLoe(Integer weightMax) {
        return weightMax != null ? ootd.owner.weightKg.loe(weightMax) : null;
    }

    private BooleanExpression weightGoe(Integer weightMin) {
        return weightMin != null ? ootd.owner.weightKg.goe(weightMin) : null;
    }

    private BooleanExpression heightLoe(Integer heightMax) {
        return heightMax != null ? ootd.owner.heightCm.loe(heightMax) : null;
    }

    private BooleanExpression heightGoe(Integer heightMin) {
        return heightMin != null ? ootd.owner.heightCm.goe(heightMin) : null;
    }

    private BooleanExpression genderEq(UserGender gender) {
        return gender != null ? ootd.owner.gender.eq(gender) : null;
    }

    private BooleanExpression publicationStatus(OotdPublicationStatus ootdPublicationStatus) {
        return ootdPublicationStatus != null ? ootd.publicationStatus.eq(ootdPublicationStatus) : null;
    }
}