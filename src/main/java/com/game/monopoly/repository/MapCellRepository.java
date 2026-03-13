package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.model.metaData.MapCellId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapCellRepository extends JpaRepository<MapCell, MapCellId> {

    List<MapCell> findByMap_MapIdOrderById_PositionAsc(Integer mapId);

}