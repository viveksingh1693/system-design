package com.viv.nearby.controller;

import com.viv.nearby.model.NearbyFriend;
import com.viv.nearby.service.NearbyFriendsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor 
@Slf4j 
@RestController
@RequestMapping("/api/v1/nearby-friends")
public class NearbyFriendsController {

        private final NearbyFriendsService nearbyFriendsService;

        @GetMapping
        public List<NearbyFriend> getNearbyFriends(
                        @RequestParam String userId,
                        @RequestParam(defaultValue = "5") double radiusKm) {

                log.info("Finding nearby friends for userId={} within radius={} km", userId, radiusKm);
                return nearbyFriendsService.findNearbyFriends(
                                userId,
                                radiusKm);
        }
}