package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Hero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Integer> {
}
