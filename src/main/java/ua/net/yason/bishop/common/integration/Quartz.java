package ua.net.yason.bishop.common.integration;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.io.Closeable;
import java.io.IOException;

@Slf4j
public class Quartz implements Closeable {
    private final Scheduler scheduler;

    public Quartz() throws SchedulerException {
        log.debug("Initialize scheduler");
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler = schedulerFactory.getScheduler();
        scheduler.start();
    }

    @Override
    public void close() throws IOException {
        if (scheduler != null) {
            try {
                log.debug("Shutdown scheduler");
                scheduler.shutdown();
            } catch (SchedulerException ex) {
                throw new IOException("Fail to shutdown scheduler", ex);
            }
        }
    }

    public void schedule(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        log.debug("Schedule the {} job on {} trigger", jobDetail, trigger);
        scheduler.scheduleJob(jobDetail, trigger);
    }
}
