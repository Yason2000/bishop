package ua.net.yason.bishop.f5000;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

@Slf4j
class F5000PacketReader {
    private final SerialPort serialPort;
    private final long readTimeoutMillis;
    private final long readPacketDelayMillis;
    private final int endOfPacketValue;
    private final byte[] buffer;
    private int alreadyRead = 0;

    public F5000PacketReader(SerialPort serialPort, int bufferSize, int endOfPacketValue,
                             long readTimeoutMillis, long readPacketDelayMillis) {
        buffer = new byte[bufferSize];
        this.serialPort = serialPort;
        this.endOfPacketValue = endOfPacketValue;
        this.readTimeoutMillis = readTimeoutMillis;
        this.readPacketDelayMillis = readPacketDelayMillis;
    }

    public byte[] readPacket() throws IOException {
        return readPacket(true);
    }

    public byte[] readPacket(boolean validate) throws IOException {
        byte[] packetData = readPacketData();
        if (validate) validatePacket(packetData);
        return packetData;
    }

    private void validatePacket(byte[] packetData) throws IOException {
        byte checksum = (byte) (IntStream.range(0, packetData.length - 2)
                .map(i -> packetData[i] & 0xFF)
                .sum() & 0xFF);
        if (checksum != packetData[packetData.length - 2]) {
            throw new IOException("Packet checksum is incorrect");
        }
    }

    private byte[] readPacketData() throws IOException {
        long startTime = System.currentTimeMillis();
        do {
            int bytesToRead = serialPort.bytesAvailable();
            if (bytesToRead > 0) {
                if (bytesToRead > buffer.length - alreadyRead) {
                    throw new IOException("Can't read bytes from serial port - buffer overflow");
                }
                int reallyRead = serialPort.readBytes(buffer, bytesToRead, alreadyRead);
                log.debug("reallyRead: {}", reallyRead);
                if (reallyRead == -1) {
                    throw new IOException("Fail to read bytes from serial port");
                }
                int alreadyReadCurrent = alreadyRead + reallyRead;
                int endOfPacketPosition = findEndOfPacketPosition(alreadyReadCurrent);
                if (endOfPacketPosition >= 0) {
                    byte[] result = Arrays.copyOf(buffer, endOfPacketPosition + 1);
                    storeTailIfAlreadyReadNextPacket(alreadyReadCurrent, endOfPacketPosition);
                    return result;
                }
                alreadyRead = alreadyReadCurrent;
            }
            try {
                Thread.sleep(readPacketDelayMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Read packet interrupted", e);
            }
        } while (System.currentTimeMillis() - startTime < readTimeoutMillis);
        throw new IOException("Read timeout");
    }

    private void storeTailIfAlreadyReadNextPacket(int alreadyReadCurrent, int endOfPacketPosition) {
        if (endOfPacketPosition != alreadyReadCurrent - 1) {
            int tailLength = alreadyReadCurrent - endOfPacketPosition - 1;
            System.arraycopy(buffer, endOfPacketPosition + 1,
                    buffer, 0, tailLength);
            alreadyRead = tailLength;
        } else {
            alreadyRead = 0;
        }
    }

    private int findEndOfPacketPosition(int alreadyReadCurrent) {
        int endOfPacketPosition = -1;
        for (int i = alreadyRead; i < alreadyReadCurrent; i++) {
            if (i > 1 && (buffer[i] & 0xFF) == endOfPacketValue) { // ignore 0xAA [0x55] starting bytes
                endOfPacketPosition = i;
            }
        }
        return endOfPacketPosition;
    }
}
