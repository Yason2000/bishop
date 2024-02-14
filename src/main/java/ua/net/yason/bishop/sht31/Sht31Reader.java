package ua.net.yason.bishop.sht31;

import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ua.net.yason.bishop.common.integration.RaspberryPi;
import ua.net.yason.bishop.common.reader.DefaultReader;
import ua.net.yason.bishop.common.reader.JobIntervalSupplier;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;
import ua.net.yason.bishop.common.reader.ReaderValueConverterSupplier;

import java.io.Closeable;

@Slf4j
public class Sht31Reader extends DefaultReader implements Closeable, ReaderValueConverterSupplier, JobIntervalSupplier {
    private static final String CONFIG_NODE = "sht31.";
    private final I2C i2c;
    private final ReaderValueConverter valueConverter;
    private final int writeRegister;
    private final int writeRegisterCode;
    private final int readRegister;

    public Sht31Reader(Config config, RaspberryPi pi) {
        super(config, CONFIG_NODE);
        if (isEnable()) {
            log.debug("Initialize i2c");
            I2CConfig i2cConfig = I2C.newConfigBuilder(pi.getPi4j())
                    .id(getValueTag().getDevice())
                    .bus(config.getInt(CONFIG_NODE + "busNumber"))
                    .device(Integer.decode(config.getString(CONFIG_NODE + "deviceAddr")))
                    .build();
            i2c = pi.getI2CProvider().create(i2cConfig);
        } else {
            i2c = null;
        }
        valueConverter = new Sht31ValueConverter(MeasurementValueTag.builder()
                .measurement(config.getString(CONFIG_NODE + "measurement"))
                .hostName(config.getString(CONFIG_NODE + "hostName"))
                .device(getValueTag().getDevice())
                .build());
        writeRegister = Integer.decode(config.getString(CONFIG_NODE + "writeRegister"));
        writeRegisterCode = Integer.decode(config.getString(CONFIG_NODE + "writeRegisterCode"));
        readRegister = Integer.decode(config.getString(CONFIG_NODE + "readRegister"));
    }

    @Override
    public void close() {
        if (i2c != null) {
            i2c.close();
        }
    }

    @Override
    protected ReaderValue readValue() {
        log.debug("Write device register");
        i2c.writeRegister(writeRegister, writeRegisterCode);
        pauseRead();

        log.debug("Read device register");
        final byte[] sensorsData = new byte[6];
        i2c.readRegister(readRegister, sensorsData);
        return Sht31Value.builder()
                .temperature(-45 + (175 * (((sensorsData[0] & 0xFF) << 8) | (sensorsData[1] & 0xFF)) / 65535.0))
                .humidity(100 * (((sensorsData[3] & 0xFF) << 8) | (sensorsData[4] & 0xFF)) / 65535.0)
                .build();
    }

    @Override
    public ReaderValueConverter getReaderValueConverter() {
        return valueConverter;
    }
}
