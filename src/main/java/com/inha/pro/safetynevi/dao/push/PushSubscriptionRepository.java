package com.inha.pro.safetynevi.dao.push;

import com.inha.pro.safetynevi.entity.push.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);
}
