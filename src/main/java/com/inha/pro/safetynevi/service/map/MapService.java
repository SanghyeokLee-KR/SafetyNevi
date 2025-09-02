package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.FavoritePlaceRepository;
import com.inha.pro.safetynevi.dao.member.FamilyRepository;
import com.inha.pro.safetynevi.entity.map.FavoritePlace;
import com.inha.pro.safetynevi.entity.member.Family;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MapService {

    private final FavoritePlaceRepository favoritePlaceRepository;
    private final FamilyRepository familyRepository; // 🌟 추가

    // 1. 집/회사 위치 저장 또는 업데이트
    public void saveSpecialPlace(String userId, String type, String address, Double lat, Double lon) {
        // 이미 등록된 것이 있는지 확인
        FavoritePlace place = favoritePlaceRepository.findByUserIdAndPlaceType(userId, type)
                .orElse(null);

        if (place != null) {
            // 있으면 업데이트
            place.updateLocation(address, lat, lon);
        } else {
            // 없으면 새로 생성
            String name = type.equals("HOME") ? "집" : "회사";
            favoritePlaceRepository.save(FavoritePlace.builder()
                    .userId(userId).placeType(type).name(name)
                    .address(address).latitude(lat).longitude(lon)
                    .build());
        }
    }

    // 2. 일반 즐겨찾기 추가
    public void addFavorite(String userId, String name, String address, Double lat, Double lon) {
        favoritePlaceRepository.save(FavoritePlace.builder()
                .userId(userId).placeType("FAVORITE").name(name)
                .address(address).latitude(lat).longitude(lon)
                .build());
    }

    // 3. 내 모든 장소 조회 (집, 회사, 즐겨찾기 전부)
    @Transactional(readOnly = true)
    public List<FavoritePlace> getMyAllPlaces(String userId) {
        return favoritePlaceRepository.findAllByUserId(userId);
    }

    // 4. 장소 삭제 (본인 소유만 삭제 가능)
    public void deletePlace(String userId, Long placeId) {
        FavoritePlace place = favoritePlaceRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));
        if (!place.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        favoritePlaceRepository.delete(place);
    }

    // 🌟 [가족] 목록 조회
    @Transactional(readOnly = true)
    public List<Family> getFamilyList(String userId) {
        return familyRepository.findAllByUserId(userId);
    }

    // 🌟 [가족] 추가
    public void addFamily(String userId, String name, String phone) {
        familyRepository.save(Family.builder()
                .userId(userId).name(name).phone(phone).build());
    }

    // 🌟 [가족] 삭제 (본인 소유만 삭제 가능)
    public void deleteFamily(String userId, Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("연락처를 찾을 수 없습니다."));
        if (!family.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        familyRepository.delete(family);
    }

}