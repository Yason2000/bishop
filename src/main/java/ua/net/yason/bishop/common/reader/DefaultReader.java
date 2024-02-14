package ua.net.yason.bishop.common.reader;

import com.typesafe.config.Config;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class DefaultReader implements Reader, JobIntervalSupplier {

    private final boolean enable;
    private final int intervalInSeconds;
    private final long readDelayMillis;

    @Getter(AccessLevel.PROTECTED)
    private final MeasurementValueTag valueTag;

    protected DefaultReader(Config config, String configNodeName) {
        enable = config.getBoolean(configNodeName + "enable");
        readDelayMillis = Long.decode(config.getString(configNodeName + "readDelayMillis"));
        intervalInSeconds = config.getInt(configNodeName + "intervalInSeconds");
        valueTag = MeasurementValueTag.builder()
                .measurement(config.getString(configNodeName + "measurement"))
                .hostName(config.getString(configNodeName + "hostName"))
                .device(config.getString(configNodeName + "device"))
                .build();
    }

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    @Override
    public ReaderValue read() throws IOException {
        if (isEnable()) {
            return readValue();
        } else {
            throw new IllegalStateException("Fail to read value in disable state");
        }
    }

    protected abstract ReaderValue readValue() throws IOException;

    protected void pauseRead() {
        try {
            Thread.sleep(readDelayMillis);
        } catch (InterruptedException ex) {
            log.error("Pause interrupted", ex);
        }
    }
}
