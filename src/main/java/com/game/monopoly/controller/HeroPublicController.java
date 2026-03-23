package com.game.monopoly.controller;

import com.game.monopoly.dto.HeroOptionResponse;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.service.HeroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/heroes")
@RequiredArgsConstructor
public class HeroPublicController {

    private final HeroService heroService;

    @GetMapping
    public List<HeroOptionResponse> getHeroes() {
        return heroService.getAllHeroes().stream()
                .map(this::toHeroOption)
                .toList();
    }

    private HeroOptionResponse toHeroOption(Hero hero) {
        return HeroOptionResponse.builder()
                .heroId(hero.getCharacterId())
                .name(hero.getName())
                .imageUrl("/images/heroes/" + hero.getName().toLowerCase().replace(" ", "-") + ".png")
                .build();
    }
}
