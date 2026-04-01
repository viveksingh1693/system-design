package com.viv.ticketmaster.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viv.ticketmaster.catalog.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            select e from Event e
            where (:city is null or lower(e.city) = lower(:city))
              and (
                    :query is null
                    or lower(e.title) like lower(concat('%', :query, '%'))
                    or lower(e.performer) like lower(concat('%', :query, '%'))
                  )
            order by e.city asc, e.title asc
            """)
    List<Event> search(@Param("city") String city, @Param("query") String query);
}
