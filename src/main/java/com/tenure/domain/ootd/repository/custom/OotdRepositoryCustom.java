package com.tenure.domain.ootd.repository.custom;

import com.tenure.domain.ootd.entity.Ootd;
import com.tenure.domain.search.dto.request.OotdSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface OotdRepositoryCustom {

    Slice<Ootd> searchOotd(String keyword, OotdSearchCondition condition, Pageable pageable);

    Long searchOotdsTotalCount(String keyword, OotdSearchCondition condition, Pageable pageable);
}
