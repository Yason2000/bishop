package ua.net.yason.bishop.mhz19c;

import com.pi4j.context.Context;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.StopBits;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ua.net.yason.bishop.common.integration.RaspberryPi;
import ua.net.yason.bishop.common.reader.DefaultReader;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;
import ua.net.yason.bishop.common.reader.ReaderValueConverterSupplier;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.IntStream;

@Slf4j
public class Mhz19cReader extends DefaultReader implements Closeable, ReaderValueConverterSupplier {
    private static final String CONFIG_NODE = "mhz19c.";
    private final ReaderValueConverter valueConverter;
    private final Serial serial;

    public Mhz19cReader(Config config, RaspberryPi pi) {
        super(config, CONFIG_NODE);
        if (isEnable()) {
            log.debug("Initialize serial");
            Context pi4j = pi.getPi4j();
            String serialPort = config.getString(CONFIG_NODE + "serial");
            serial = pi4j.create(Serial.newConfigBuilder(pi4j)
                    .use_9600_N81()
                    .dataBits_8()
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE)
                    .id(getValueTag().getDevice())
                    .device(serialPort)
                    .provider("pigpio-serial")
                    .build());
            serial.open();
            log.debug("Waiting till serial port {} is open", serialPort);
            int isOpenTryCount = Integer.decode(config.getString(CONFIG_NODE + "isOpenTryCount"));
            while (!serial.isOpen()) {
                pauseRead();
                if (isOpenTryCount-- < 0) {
                    throw new RuntimeException("Fail to open serial port: " + serialPort);
                }
            }
        } else {
            serial = null;
        }
        valueConverter = new Mhz19cValueConverter(getValueTag());
    }

    @Override
    public ReaderValueConverter getReaderValueConverter() {
        return valueConverter;
    }

    @Override
    public void close() {
        if (serial != null) {
            serial.close();
        }
    }

    @Override
    protected ReaderValue readValue() throws IOException {
        log.debug("Write data to serial");
        serial.write(new byte[]{(byte) 0xFF, 0x01, (byte) 0x86, 0x00, 0x00, 0x00, 0x00, 0x00, 0x79});
        pauseRead();

        log.debug("Read data from serial");
        byte[] response = new byte[9];
        serial.read(response);
        if (getCheckSum(response) != response[8]) {
            throw new IOException("Checksum fail");
        }
        return Mhz19cValue.builder()
                .temperature((response[4] & 0xFF) - 40)
                .co2(((response[2] & 0xFF) << 8) + (response[3] & 0xFF))
                .build();
    }

    private byte getCheckSum(byte[] packet) {
        final int checksum = IntStream.rangeClosed(1, 7)
                .map(i -> packet[i] & 0xFF)
                .sum();
        return (byte) (0xFF - (checksum & 0xFF) + 1);
    }
}
