package com.viv.nearby.service;

import com.viv.nearby.repository.FriendRepository;
import com.viv.nearby.repository.UserGeoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.viv.nearby.model.NearbyFriend;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j 
@RequiredArgsConstructor 
@Service
public class NearbyFriendsService {

    private final FriendRepository friendRepository;
    private final UserGeoRepository userGeoRepository;


    public List<NearbyFriend> findNearbyFriends(
            String userId,
            double radiusKm) {

        /*
         * 1. Get user's latest location from Redis GEO
         */
        Point location =  userGeoRepository.getUserLocation(userId);

        if (location == null) {
            return List.of();
        }

        /*
         * Redis Point:
         *
         * x = longitude
         * y = latitude
         */

        /*
         * 2. Find all users within radius
         */
        var nearbyUsers =  userGeoRepository.findNearby( location.getY(), location.getX(),radiusKm);

        /*
         * 3. Get user's friends from PostgreSQL
         */
        Set<String> friends =
                friendRepository.findFriends(userId)
                        .stream()
                        .collect(Collectors.toSet());

        /*
         * 4. Geo candidates ∩ friends
         */
        return nearbyUsers.stream()
                .map(result -> {

                    String nearbyUserId =
                            result.getContent().getName();

                    double distanceKm =
                            result.getDistance().getValue();

                    return new NearbyFriend(
                            nearbyUserId,
                            distanceKm
                    );
                })
                .filter(friend ->
                        friends.contains(friend.userId())
                )
                .toList();
    }
}