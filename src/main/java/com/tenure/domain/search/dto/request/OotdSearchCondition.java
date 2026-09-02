package com.tenure.domain.search.dto.request;

import com.tenure.domain.search.enums.ItemStatusFilter;
import com.tenure.domain.search.enums.SearchSortType;
import com.tenure.domain.user.enums.UserGender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OotdSearchCondition {

    private UserGender gender;
    private Integer heightMin;
    private Integer heightMax;
    private Integer weightMin;
    private Integer weightMax;
    private List<Long> categoryIds;
    private ItemStatusFilter itemStatusFilter;
    private SearchSortType sort = SearchSortType.LATEST;

    // 커서 페이징용 파라미터
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime cursor;
    private Long cursorId;
    private Integer cursorValue;
    private Double cursorHotScore;
}
