package ua.net.yason.bishop.bme280;

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
import java.io.IOException;

@Slf4j
public class Bme280Reader extends DefaultReader implements Closeable, ReaderValueConverterSupplier, JobIntervalSupplier {
    private static final String CONFIG_NODE = "bme280.";
    private final I2C i2c;
    private final ReaderValueConverter valueConverter;

    public Bme280Reader(Config config, RaspberryPi pi) {
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
        valueConverter = new Bme280ValueConverter(MeasurementValueTag.builder()
                .measurement(config.getString(CONFIG_NODE + "measurement"))
                .hostName(config.getString(CONFIG_NODE + "hostName"))
                .device(getValueTag().getDevice())
                .build());
    }

    @Override
    public void close() {
        if (i2c != null) {
            i2c.close();
        }
    }

    /**
     * @param read 8 bits data
     * @return unsigned value
     */
    private int castOffSignByte(byte read) {
        return ((int) read & 0Xff);
    }

    /**
     * @param read 8 bits data
     * @return signed value
     */
    private int signedByte(byte[] read) {
        return ((int) read[0]);
    }

    /**
     * @param read 16bits of data stored in 8 bit array
     * @return 16bit signed
     */
    private int signedInt(byte[] read) {
        int temp = 0;
        temp = (read[0] & 0xff);
        temp += (((long) read[1]) << 8);
        return (temp);
    }

    /**
     * @param read 16 bits of data  stored in 8 bit array
     * @return 64 bit unsigned value
     */
    private long castOffSignInt(byte[] read) {
        long temp = 0;
        temp = ((long) read[0] & 0xff);
        temp += (((long) read[1] & 0xff)) << 8;
        return (temp);
    }

    /**
     * This part of code was taken from https://github.com/Pi4J/pi4j-jbang/blob/main/Pi4JTempHumPressI2C.java
     *
     * @return sensor data
     * @throws IOException when sensor is not BME280 compatible
     */
    @Override
    protected ReaderValue readValue() throws IOException {
        // resetSensor
        i2c.writeRegister(0xE0, 0xB6);
        pauseRead();

        int id = i2c.readRegister(0xD0);
        if (id != 0x60) {
            throw new IOException("Incorrect chip Id, i2c i2c is not BME280");
        }

        int ctlHum = i2c.readRegister(0xF2);
        ctlHum |= 0x01;
        byte[] humRegVal = new byte[1];
        humRegVal[0] = (byte) ctlHum;
        i2c.writeRegister(0xF2, humRegVal, humRegVal.length);
        pauseRead();

        // Set forced mode to leave sleep ode state and initiate measurements.
        // At measurement completion chip returns to sleep mode
        int ctlReg = i2c.readRegister(0xF4);
        ctlReg |= 0x01;
        ctlReg &= ~0xE0;
        ctlReg |= 0x20;
        ctlReg &= ~0x1C;
        ctlReg |= 0x04;

        int ctrlMeasCode = 0xF4;
        byte[] regVal = new byte[1];
        regVal[0] = (byte) (ctrlMeasCode);
        byte[] ctlVal = new byte[1];
        ctlVal[0] = (byte) ctlReg;
        i2c.writeRegister(regVal, ctlVal, ctlVal.length);
        pauseRead();

        // getMeasurements
        byte[] buff = new byte[6];
        i2c.readRegister(0xF7, buff);
        long adc_T = (long) ((buff[3] & 0xFF) << 12) | (long) ((buff[4] & 0xFF) << 4);
        long adc_P = (long) ((buff[0] & 0xFF) << 12) | (long) ((buff[1] & 0xFF) << 4);

        int humMsbRegister = 0xFD;
        byte[] buffHum = new byte[2];
        i2c.readRegister(humMsbRegister, buffHum);
        long adc_H = (long) ((buffHum[0] & 0xFF) << 8) | (long) (buffHum[1] & 0xFF);

        byte[] compVal = new byte[2];

        // Temperature
        i2c.readRegister(0x88, compVal);
        long dig_t1 = castOffSignInt(compVal);

        i2c.readRegister(0x8A, compVal);
        int dig_t2 = signedInt(compVal);

        i2c.readRegister(0x8C, compVal);
        int dig_t3 = signedInt(compVal);

        double var1 = (((double) adc_T) / 16384.0 - ((double) dig_t1) / 1024.0) * ((double) dig_t2);
        double var2 = ((((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0) *
                (((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0)) * ((double) dig_t3);
        double t_fine = (int) (var1 + var2);
        double temperature = (var1 + var2) / 5120.0;

        // Pressure
        i2c.readRegister(0x8E, compVal);
        long dig_p1 = castOffSignInt(compVal);

        i2c.readRegister(0x90, compVal);
        int dig_p2 = signedInt(compVal);

        i2c.readRegister(0x92, compVal);
        int dig_p3 = signedInt(compVal);

        i2c.readRegister(0x94, compVal);
        int dig_p4 = signedInt(compVal);

        i2c.readRegister(0x96, compVal);
        int dig_p5 = signedInt(compVal);

        i2c.readRegister(0x98, compVal);
        int dig_p6 = signedInt(compVal);

        i2c.readRegister(0x9A, compVal);
        int dig_p7 = signedInt(compVal);

        i2c.readRegister(0x9C, compVal);
        int dig_p8 = signedInt(compVal);

        i2c.readRegister(0x9E, compVal);
        int dig_p9 = signedInt(compVal);

        var1 = (t_fine / 2.0) - 64000.0;
        var2 = var1 * var1 * ((double) dig_p6) / 32768.0;
        var2 = var2 + var1 * ((double) dig_p5) * 2.0;
        var2 = (var2 / 4.0) + (((double) dig_p4) * 65536.0);
        var1 = (((double) dig_p3) * var1 * var1 / 524288.0 + ((double) dig_p2) * var1) / 524288.0;
        var1 = (1.0 + var1 / 32768.0) * ((double) dig_p1);
        double pressure = 0;
        if (var1 != 0.0) {
            // avoid exception caused by division by zero
            pressure = 1048576.0 - (double) adc_P;
            pressure = (pressure - (var2 / 4096.0)) * 6250.0 / var1;
            var1 = ((double) dig_p9) * pressure * pressure / 2147483648.0;
            var2 = pressure * ((double) dig_p8) / 32768.0;
            pressure = pressure + (var1 + var2 + ((double) dig_p7)) / 16.0;
        }
        pressure /= 100; // GPa

        // Humidity
        byte[] charVal = new byte[1];

        i2c.readRegister(0xA1, charVal);
        long dig_h1 = castOffSignByte(charVal[0]);

        i2c.readRegister(0xE1, compVal);
        int dig_h2 = signedInt(compVal);

        i2c.readRegister(0xE3, charVal);
        long dig_h3 = castOffSignByte(charVal[0]);

        i2c.readRegister(0xE4, compVal);
        // get the bits
        int dig_h4 = ((compVal[0] & 0xff) << 4) | (compVal[1] & 0x0f);

        i2c.readRegister(0xE5, compVal);
        // get the bits
        int dig_h5 = (compVal[0] & 0x0f) | ((compVal[1] & 0xff) << 4);

        i2c.readRegister(0xE7, charVal);
        long dig_h6 = signedByte(charVal);

        double humidity = t_fine - 76800.0;
        humidity = (adc_H - (((double) dig_h4) * 64.0 + ((double) dig_h5) / 16384.0 * humidity))
                * (((double) dig_h2) / 65536.0 * (1.0 + ((double) dig_h6) / 67108864.0 * humidity
                * (1.0 + ((double) dig_h3) / 67108864.0 * humidity)));
        humidity = humidity * (1.0 - ((double) dig_h1) * humidity / 524288.0);
        if (humidity > 100.0) {
            humidity = 100.0;
        } else if (humidity < 0.0) {
            humidity = 0.0;
        }

        return Bme280Value.builder()
                .temperature(temperature)
                .humidity(humidity)
                .pressure(pressure)
                .build();
    }

    @Override
    public ReaderValueConverter getReaderValueConverter() {
        return valueConverter;
    }
}
