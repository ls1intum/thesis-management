package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.NotificationSetting;
import de.tum.cit.aet.thesis.entity.key.NotificationSettingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, NotificationSettingId> {
	@Modifying
	@Transactional
	@Query("DELETE FROM NotificationSetting ns WHERE ns.id.userId = :userId")
	void deleteByUserId(UUID userId);
}
