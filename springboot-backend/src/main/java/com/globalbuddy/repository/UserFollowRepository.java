package com.globalbuddy.repository;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, String> {
    Optional<UserFollow> findByFollowerAndFollowing(AppUser follower, AppUser following);
    boolean existsByFollowerAndFollowing(AppUser follower, AppUser following);
    long countByFollower(AppUser follower); // Following count
    long countByFollowing(AppUser following); // Followers count
}

