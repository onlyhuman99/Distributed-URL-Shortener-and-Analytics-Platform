package com.example.shortener.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository
        extends JpaRepository<ClickEvent, Long> {

    long countByCode(String code);

    @Query(
        value = """
            SELECT to_char(clicked_at, 'YYYY-MM-DD') AS day,
                   COUNT(*) AS clicks
            FROM click_event
            WHERE code = :code
            GROUP BY 1
            ORDER BY 1
            """,
        nativeQuery = true
    )
    List<DailyClicks> dailyClicks(@Param("code") String code);

    interface DailyClicks {
        String getDay();
        long getClicks();
    }
}