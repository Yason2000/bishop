package ua.net.yason.bishop.common.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import ua.net.yason.bishop.common.integration.InfluxDb;
import ua.net.yason.bishop.common.integration.Quartz;

@Slf4j
@RequiredArgsConstructor
public class ReaderJobScheduler {
    static final String READER_KEY = "reader";
    static final String CONVERTER_KEY = "converter";
    static final String DB_KEY = "db";

    private final Quartz quartz;
    private final InfluxDb db;

    public void schedule(Reader reader,
                         ReaderValueConverterSupplier valueConverterSupplier,
                         JobIntervalSupplier jobIntervalSupplier) throws SchedulerException {
        String readerClassName = reader.getClass().getSimpleName();
        if (reader.isEnable()) {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(READER_KEY, reader);
            jobDataMap.put(CONVERTER_KEY, valueConverterSupplier.getReaderValueConverter());
            jobDataMap.put(DB_KEY, db);
            JobDetail jobDetail = JobBuilder.newJob(ReaderJob.class)
                    .withIdentity(readerClassName + "Job")
                    .usingJobData(jobDataMap)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(readerClassName + "Trigger")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(jobIntervalSupplier.getIntervalInSeconds())
                            .repeatForever())
                    .build();
            quartz.schedule(jobDetail, trigger);
        } else {
            log.warn("Skip reader job schedule for disabled reader: {}", readerClassName);
        }
    }
}
