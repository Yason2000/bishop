package ua.net.yason.bishop.common.reader;

import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import ua.net.yason.bishop.common.integration.InfluxDb;

import static ua.net.yason.bishop.common.reader.ReaderJobScheduler.CONVERTER_KEY;
import static ua.net.yason.bishop.common.reader.ReaderJobScheduler.DB_KEY;
import static ua.net.yason.bishop.common.reader.ReaderJobScheduler.READER_KEY;

@Slf4j
public class ReaderJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        log.debug("Collecting data");
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Reader reader = (Reader) dataMap.get(READER_KEY);
        try {
            ReaderValue value = reader.read();
            log.info("Collected data: {}", value);
            ReaderValueConverter converter = (ReaderValueConverter) dataMap.get(CONVERTER_KEY);
            Point point = converter.convert(value);
            InfluxDb db = (InfluxDb) dataMap.get(DB_KEY);
            db.write(point);
        } catch (Exception ex) {
            log.error("Fail to collect data", ex);
        }
    }
}
