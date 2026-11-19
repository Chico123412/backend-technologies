package com.autoservice;

import com.autoservice.config.CatalogQuartzConfig;
import com.autoservice.service.CatalogSnapshotService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingTest {
    @BeforeAll
    static void createCatalogFixture(@Autowired JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE service_items (id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO service_items (id) VALUES (1)");
    }

    @Autowired CatalogSnapshotService snapshots;
    @Autowired JdbcTemplate jdbc;
    @Autowired Scheduler scheduler;
    @Autowired MockMvc mvc;
    @Autowired javax.sql.DataSource dataSource;

    @Autowired com.autoservice.events.CatalogEventJournal journal;

    @Autowired org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;
    @Autowired org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry listeners;

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_RABBITMQ_TESTS", matches = "true")
    void realListenerProcessesQueuedJsonAndIgnoresRedelivery() throws Exception {
        var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(rabbit.getConnectionFactory());
        var queue = new org.springframework.amqp.core.Queue("reporting.catalog-events", true);
        var exchange = new org.springframework.amqp.core.DirectExchange("catalog.events", true, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(org.springframework.amqp.core.BindingBuilder.bind(queue)
                .to(exchange).with("service.changed"));
        assertThat(admin.getQueueProperties(queue.getName()).get("QUEUE_MESSAGE_COUNT")).isEqualTo(2);
        jdbc.update("DELETE FROM catalog_event_journal");
        assertThat(journal.recent()).isEmpty();
        listeners.start();
        try {
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline
                    && (journal.recent().size() < 2
                    || !Integer.valueOf(0).equals(admin.getQueueProperties(queue.getName()).get("QUEUE_MESSAGE_COUNT")))) {
                Thread.sleep(20);
            }
            assertThat(journal.recent()).hasSize(2)
                    .extracting(com.autoservice.events.ServiceEvent::action)
                    .containsOnly("CREATED");
            var event = journal.recent().get(0);
            var confirmation = new org.springframework.amqp.rabbit.connection.CorrelationData(event.eventId());
            rabbit.convertAndSend("catalog.events", "service.changed",
                    Map.of("eventId", event.eventId(), "action", event.action(),
                            "serviceId", event.serviceId(), "name", event.name()), confirmation);
            assertThat(confirmation.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS).isAck()).isTrue();
            deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (!Integer.valueOf(0).equals(admin.getQueueProperties(queue.getName()).get("QUEUE_MESSAGE_COUNT"))
                    && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
        } finally {
            listeners.stop();
        }
        assertThat(admin.getQueueProperties(queue.getName()).get("QUEUE_MESSAGE_COUNT")).isEqualTo(0);
        assertThat(journal.recent()).hasSize(2);
    }

    @Test
    void repeatedEventIsStoredOnlyOnceAndVisibleThroughApi() throws Exception {
        var event = new com.autoservice.events.ServiceEvent("repeat-event", "CREATED", 7L, "Огляд");
        assertThat(journal.accept(event)).isEqualTo(1);
        assertThat(journal.accept(event)).isZero();
        assertThat(journal.recent()).contains(event);
        mvc.perform(get("/api/catalog-events")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventId == 'repeat-event')].name").value("Огляд"));
    }

    @Test
    void statusEndpointRespondsWithServiceAndSchedulerState() throws Exception {
        mvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("reporting-service"))
                .andExpect(jsonPath("$.scheduler").value("standby"));
    }

    @Test
    void snapshotAndCleanupAreIdempotentInDatabase() {
        var day = java.time.LocalDate.of(2026, 11, 10);
        jdbc.update("DELETE FROM catalog_snapshots");
        assertThat(snapshots.capture("daily", day.minusDays(31))).isEqualTo(1);
        assertThat(snapshots.capture("daily", day.minusDays(30))).isEqualTo(1);
        assertThat(snapshots.capture("daily", day)).isEqualTo(1);
        assertThat(snapshots.capture("daily", day)).isZero();
        assertThat(snapshots.cleanup(day, 30)).isEqualTo(1);
        assertThat(snapshots.cleanup(day, 30)).isZero();
        assertThat(jdbc.queryForList("SELECT report_date FROM catalog_snapshots ORDER BY report_date",
                java.sql.Date.class)).containsExactly(java.sql.Date.valueOf(day.minusDays(30)), java.sql.Date.valueOf(day));
    }

    @Test
    void quartzExecutesRealJobUsingJdbcStoreAndSpringService() throws Exception {
        assertThat(scheduler.getMetaData().isJobStoreSupportsPersistence()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = ?",
                Integer.class, scheduler.getSchedulerName())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = ?",
                Integer.class, scheduler.getSchedulerName())).isEqualTo(3);
        jdbc.update("DELETE FROM catalog_snapshots WHERE report_type = 'weekly'");
        var finished = new java.util.concurrent.CompletableFuture<JobExecutionException>();
        var listener = new JobListenerSupport() {
            @Override public String getName() { return "snapshot-test"; }

            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException error) {
                if ("weekly".equals(context.getMergedJobDataMap().getString("reportType"))) {
                    finished.complete(error);
                }
            }
        };
        scheduler.getListenerManager().addJobListener(listener);
        try {
            scheduler.start();
            scheduler.triggerJob(CatalogQuartzConfig.SNAPSHOT, new JobDataMap(Map.of("reportType", "weekly")));
            assertThat(finished.get(5, java.util.concurrent.TimeUnit.SECONDS)).isNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_snapshots WHERE report_type = 'weekly'",
                    Integer.class)).isEqualTo(1);
        } finally {
            scheduler.standby();
            scheduler.getListenerManager().removeJobListener(listener.getName());
        }
    }

    @Test
    void triggerSurvivesSchedulerRestartUsingTheSameDatabase() throws Exception {
        var job = scheduler.getJobDetail(CatalogQuartzConfig.SNAPSHOT);
        var key = org.quartz.TriggerKey.triggerKey("restart-check", "catalog");
        var start = java.util.Date.from(Instant.parse("2099-01-01T00:00:00Z"));
        var first = restartTestScheduler();
        try {
            first.getScheduler().addJob(job, false);
            first.getScheduler().scheduleJob(TriggerBuilder.newTrigger()
                    .withIdentity(key).forJob(job.getKey()).startAt(start).build());
        } finally {
            first.destroy();
        }
        var second = restartTestScheduler();
        try {
            var restored = second.getScheduler().getTrigger(key);
            assertThat(restored).isNotNull();
            assertThat(restored.getJobKey()).isEqualTo(job.getKey());
            assertThat(restored.getNextFireTime()).isEqualTo(start);
        } finally {
            second.destroy();
        }
    }

    private SchedulerFactoryBean restartTestScheduler() throws Exception {
        var factory = new SchedulerFactoryBean();
        factory.setSchedulerName("restart-check-scheduler");
        factory.setDataSource(dataSource);
        factory.setAutoStartup(false);
        factory.afterPropertiesSet();
        return factory;
    }
}
