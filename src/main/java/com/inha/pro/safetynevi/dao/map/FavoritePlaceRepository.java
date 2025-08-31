package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.map.FavoritePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoritePlaceRepository extends JpaRepository<FavoritePlace, Long> {

    // 특정 유저의 특정 타입(집/회사) 장소 조회
    Optional<FavoritePlace> findByUserIdAndPlaceType(String userId, String placeType);

    // 특정 유저의 모든 장소 목록 조회
    List<FavoritePlace> findAllByUserId(String userId);
}