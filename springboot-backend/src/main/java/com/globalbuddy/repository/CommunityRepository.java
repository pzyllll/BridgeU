package com.globalbuddy.repository;

import com.globalbuddy.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityRepository extends JpaRepository<Community, String> {
    List<Community> findByCountryOrderByCreatedAtDesc(String country);
    List<Community> findByCountryAndLanguageOrderByCreatedAtDesc(String country, String language);
    List<Community> findAllByOrderByCreatedAtDesc();
}

