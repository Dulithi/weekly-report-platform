package com.weeklyreport.activity.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.entity.ActivityLog;
import com.weeklyreport.activity.repository.ActivityLogRepository;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, 
        UserRepository userRepository
    ) {
        this.activityLogRepository = activityLogRepository;

        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            UUID actorUserId,
            ActivityType activityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {

        User actor = userRepository.getReferenceById(actorUserId);

        ActivityLog activity = new ActivityLog(actor, activityType, entityId, metadata);

        activityLogRepository.save(activity);
    }
}
