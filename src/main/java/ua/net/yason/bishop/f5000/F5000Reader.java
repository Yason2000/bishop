package ua.net.yason.bishop.f5000;

import com.fazecast.jSerialComm.SerialPort;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ua.net.yason.bishop.common.reader.DefaultReader;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;
import ua.net.yason.bishop.common.reader.ReaderValueConverterSupplier;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static ua.net.yason.bishop.common.Utils.bytesToHexPrintString;
import static ua.net.yason.bishop.common.Utils.findDouble;

@Slf4j
public class F5000Reader extends DefaultReader implements Closeable, ReaderValueConverterSupplier {
    private static final byte[] INIT_COMMAND = new byte[]{(byte) 0xAA, 0x04, 0x06, (byte) 0xB4, 0x55};
    private static final byte[] START_COMMAND = new byte[]{(byte) 0xAA, 0x05, 0x0E, 0x01, (byte) 0xBE, 0x55};
    private static final byte[] STOP_COMMAND = new byte[]{(byte) 0xAA, 0x05, 0x0E, 0x00, (byte) 0xBD, 0x55};

    private static final String CONFIG_NODE = "f5000.";
    private final SerialPort serial;
    private final F5000PacketReader packetReader;
    private final Pattern lastValueRegexPattern;
    private final Pattern averageValueRegexPattern;
    private final Pattern totalValueRegexPattern;
    private final ReaderValueConverter valueConverter;
    private final AtomicReference<F5000Value> lastValue = new AtomicReference<>();
    private final AtomicBoolean stopReaderThread = new AtomicBoolean();;
    private final Thread readerThread;

    public F5000Reader(Config config) {
        super(config, CONFIG_NODE);
        if (isEnable()) {
            log.debug("Initialize serial");
            String serialPort = config.getString(CONFIG_NODE + "serial");
            serial = SerialPort.getCommPort(serialPort);
            serial.setBaudRate(config.getInt(CONFIG_NODE + "baudRate"));
            serial.openPort();
            log.debug("Waiting till serial port {} is open", serialPort);
            int isOpenTryCount = Integer.decode(config.getString(CONFIG_NODE + "isOpenTryCount"));
            while (!serial.isOpen()) {
                pauseRead();
                if (isOpenTryCount-- < 0) {
                    throw new RuntimeException("Fail to open serial port: " + serialPort);
                }
            }
            int readBufferSize = config.getInt(CONFIG_NODE + "readBufferSize");
            int readTimeoutMillis = config.getInt(CONFIG_NODE + "readTimeoutMillis");
            int readPacketDelayMillis = config.getInt(CONFIG_NODE + "readPacketDelayMillis");
            int endOfPacketValue = Integer.decode(config.getString(CONFIG_NODE + "endOfPacketValue"));
            packetReader = new F5000PacketReader(serial, readBufferSize, endOfPacketValue,
                    readTimeoutMillis, readPacketDelayMillis);
            readerThread = initReaderThread();
        } else {
            serial = null;
            packetReader = null;
            readerThread = null;
        }
        lastValueRegexPattern = Pattern.compile(config.getString(CONFIG_NODE + "lastValueRegex"));
        averageValueRegexPattern = Pattern.compile(config.getString(CONFIG_NODE + "averageValueRegex"));
        totalValueRegexPattern = Pattern.compile(config.getString(CONFIG_NODE + "totalValueRegex"));
        valueConverter = new F5000ValueConverter(getValueTag());
    }

    private Thread initReaderThread() {
        log.debug("Write INIT command");
        serial.writeBytes(INIT_COMMAND, INIT_COMMAND.length);
        try {
            log.debug("Read INIT packet");
            byte[] initPacket = packetReader.readPacket(false);
            log.debug("Init packet received:\n{}", bytesToHexPrintString(initPacket));
        } catch (IOException ex) {
            log.error("Fail to read init packet", ex);
        }

        log.debug("Write START command");
        serial.writeBytes(START_COMMAND, START_COMMAND.length);
        serial.writeBytes(START_COMMAND, START_COMMAND.length);
        serial.writeBytes(START_COMMAND, START_COMMAND.length);
        Thread thread = new Thread(() -> {
            while (!stopReaderThread.get()) {
                try {
                    byte[] dataPacket = packetReader.readPacket();
                    log.debug("Data packet received:\n{}", bytesToHexPrintString(dataPacket));
                    if (dataPacket.length > 10) { // ignore small packets without data values
                        byte[] valueData = Arrays.copyOfRange(dataPacket, 3, dataPacket.length - 2);
                        String value = new String(valueData, StandardCharsets.UTF_8);
                        lastValue.set(F5000Value.builder()
                                .lastSievert(findDouble(value, lastValueRegexPattern)
                                        .orElseThrow(() -> new IllegalArgumentException("Could not parse last sievert value")))
                                .averageSievert(findDouble(value, averageValueRegexPattern)
                                        .orElseThrow(() -> new IllegalArgumentException("Could not parse average sievert value")))
                                .totalSievert(findDouble(value, totalValueRegexPattern)
                                        .orElseThrow(() -> new IllegalArgumentException("Could not parse total sievert value")))
                                .build());
                    }
                } catch (Exception ex) {
                    log.error("Error read data", ex);
                }
            }
        });
        thread.start();
        return thread;
    }

    @Override
    public ReaderValueConverter getReaderValueConverter() {
        return valueConverter;
    }

    @Override
    public void close() {
        if (readerThread != null) {
            stopReaderThread.set(true);
            readerThread.interrupt();
        }
        if (serial != null) {
            serial.writeBytes(STOP_COMMAND, STOP_COMMAND.length);
            serial.closePort();
        }
    }

    @Override
    protected ReaderValue readValue() {
        F5000Value f5000Value = lastValue.get();
        if (f5000Value == null) {
            throw new RuntimeException("Value isn't ready");
        }
        return f5000Value;
    }
}
