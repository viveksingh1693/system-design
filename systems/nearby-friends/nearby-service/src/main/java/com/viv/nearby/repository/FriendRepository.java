package com.viv.nearby.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor 
@Repository
public class FriendRepository {

    private final JdbcTemplate jdbcTemplate;

    public Set<String> findFriends(String userId) {

        String sql = """
                SELECT friend_id
                FROM friendship
                WHERE user_id = ?
                  AND status = 'ACTIVE'
                """;

        return new HashSet<>(
                jdbcTemplate.query(
                        sql,
                        (rs, rowNum) ->
                                rs.getString("friend_id"),
                        userId
                )
        );
    }
}
