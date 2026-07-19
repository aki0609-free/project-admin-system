package com.project.backend.features.system.notice.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeDynamicSchedulerService {

    private final NoticeRuleRepository noticeRuleRepository;
    private final NoticeAutoGenerateService noticeAutoGenerateService;
    private final ThreadPoolTaskScheduler noticeTaskScheduler;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        reloadSchedules();
    }

    @PreDestroy
    public void shutdown() {
        cancelAll();
    }

    /**
     * DBの notice_rule を読み直して、スケジュールを再登録する。
     * 画面でルールを保存・削除したあとにも呼ぶ。
     */
    @Transactional(readOnly = true)
    public synchronized void reloadSchedules() {
        cancelAll();

        List<NoticeRule> rules =
                noticeRuleRepository.findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

        for (NoticeRule rule : rules) {
            register(rule);
        }

        log.info("Notice dynamic schedules reloaded. count={}", scheduledTasks.size());
    }

    /**
     * 指定ルールだけ再登録したい時用。
     */
    @Transactional(readOnly = true)
    public synchronized void reloadRule(Long ruleId) {
        cancel(ruleId);

        NoticeRule rule = noticeRuleRepository.findByIdAndDeletedAtIsNull(ruleId)
                .orElse(null);

        if (rule == null || !rule.isActiveFlag()) {
            return;
        }

        register(rule);
    }

    public synchronized void cancel(Long ruleId) {
        ScheduledFuture<?> future = scheduledTasks.remove(ruleId);

        if (future != null) {
            future.cancel(false);
        }
    }

    public synchronized void cancelAll() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }

        scheduledTasks.clear();
    }

    @SuppressWarnings("null")
    private void register(NoticeRule rule) {
        if (rule.getId() == null) {
            return;
        }

        if (!StringUtils.hasText(rule.getCronExpression())) {
            log.info(
                    "NoticeRule skipped because cronExpression is empty. ruleCode={}",
                    rule.getRuleCode()
            );
            return;
        }

        try {
            CronTrigger trigger = new CronTrigger(rule.getCronExpression());

            ScheduledFuture<?> future = noticeTaskScheduler.schedule(
                    () -> executeRule(rule.getId(), rule.getRuleCode()),
                    trigger
            );

            if (future != null) {
                scheduledTasks.put(rule.getId(), future);
            }

            log.info(
                    "NoticeRule scheduled. ruleId={}, ruleCode={}, cron={}",
                    rule.getId(),
                    rule.getRuleCode(),
                    rule.getCronExpression()
            );

        } catch (Exception e) {
            log.error(
                    "NoticeRule schedule registration failed. ruleId={}, ruleCode={}, cron={}",
                    rule.getId(),
                    rule.getRuleCode(),
                    rule.getCronExpression(),
                    e
            );
        }
    }

    private void executeRule(Long ruleId, String ruleCode) {
        try {
            log.info("NoticeRule scheduled execution started. ruleId={}, ruleCode={}", ruleId, ruleCode);

            noticeAutoGenerateService.generateByRuleId(ruleId);

            log.info("NoticeRule scheduled execution finished. ruleId={}, ruleCode={}", ruleId, ruleCode);

        } catch (Exception e) {
            log.error(
                    "NoticeRule scheduled execution failed. ruleId={}, ruleCode={}",
                    ruleId,
                    ruleCode,
                    e
            );
        }
    }
}