package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.NotificationSetting;
import de.tum.cit.aet.thesis.entity.key.NotificationSettingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, NotificationSettingId> {

}
