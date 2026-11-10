package com.autoservice.job;

import com.autoservice.service.CatalogSnapshotService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class CatalogSnapshotJob implements Job {
    private final CatalogSnapshotService snapshots;

    public CatalogSnapshotJob(CatalogSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            snapshots.capture(JobParameters.required(context, "reportType"), JobParameters.scheduledDate(context));
        } catch (RuntimeException error) {
            throw new JobExecutionException("Не вдалося зберегти знімок каталогу", error, false);
        }
    }
}
