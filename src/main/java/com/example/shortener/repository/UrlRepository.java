package com.example.shortener.repository;

import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.entity.UrlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    Optional<UrlEntity> findByShortCodeAndOwnerUid(String shortCode, String ownerUid);

    boolean existsByShortCode(String shortCode);

    List<UrlEntity> findByShortCodeIn(Collection<String> shortCodes);

    /**
     * One page of the owner's links with their click counts, in a single query.
     * {@code pattern} is a lower-case LIKE pattern ("%" matches everything).
     */
    @Query(value = """
            select new com.example.shortener.dto.UrlItemDto(u.shortCode, u.originalUrl, u.createdAt, count(a.id), u.disabled)
            from UrlEntity u left join UrlAnalytics a on a.url = u
            where u.ownerUid = :owner
              and (lower(u.shortCode) like :pattern escape '!' or lower(u.originalUrl) like :pattern escape '!')
            group by u.id, u.shortCode, u.originalUrl, u.createdAt, u.disabled
            order by u.id desc
            """,
            countQuery = """
            select count(u) from UrlEntity u
            where u.ownerUid = :owner
              and (lower(u.shortCode) like :pattern escape '!' or lower(u.originalUrl) like :pattern escape '!')
            """)
    Page<UrlItemDto> findPageWithClicks(@Param("owner") String owner, @Param("pattern") String pattern, Pageable pageable);

    long countByOwnerUid(String ownerUid);

    /** Bulk delete; the database cascades to the link's analytics rows (fk_analytics_url ON DELETE CASCADE). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UrlEntity u where u.id = :id")
    int deleteByIdBulk(@Param("id") Long id);
}
