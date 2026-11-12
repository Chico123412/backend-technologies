package com.autoservice.job;

import com.autoservice.service.CatalogSnapshotService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class SnapshotCleanupJob implements Job {
    private final CatalogSnapshotService snapshots;

    public SnapshotCleanupJob(CatalogSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            snapshots.cleanup(JobParameters.scheduledDate(context),
                    Integer.parseInt(JobParameters.required(context, "retentionDays")));
        } catch (RuntimeException error) {
            throw new JobExecutionException("Не вдалося очистити старі знімки", error, false);
        }
    }
}
