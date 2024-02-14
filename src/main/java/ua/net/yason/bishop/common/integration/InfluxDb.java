package ua.net.yason.bishop.common.integration;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

@Slf4j
public class InfluxDb implements Closeable {
    private static final String CONFIG_NODE = "influxDb.";
    private final InfluxDBClient dbClient;
    private final WriteApiBlocking writeApi;
    private final String bucket;
    private final String organization;

    public InfluxDb(Config config) {
        log.debug("Initialize db client");
        bucket = config.getString(CONFIG_NODE + "bucket");
        organization = config.getString(CONFIG_NODE + "organization");
        dbClient = InfluxDBClientFactory.create(config.getString(CONFIG_NODE + "url"),
                config.getString(CONFIG_NODE + "token").toCharArray());
        writeApi = dbClient.getWriteApiBlocking();
    }

    @Override
    public void close() {
        if (dbClient != null) {
            log.debug("Close db client");
            dbClient.close();
        }
    }

    public void write(Point point) {
        log.trace("Write {} point into {} bucket of {} organization", point, bucket, organization);
        writeApi.writePoint(bucket, organization, point);
    }
}
