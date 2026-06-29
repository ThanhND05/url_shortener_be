package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.LinkRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRuleRepository extends JpaRepository<LinkRule, Long> {

    /** Lấy tất cả rule của link, sắp theo priority ASC (ưu tiên cao → thấp). */
    List<LinkRule> findByLinkIdOrderByPriorityAsc(Long linkId);
}
