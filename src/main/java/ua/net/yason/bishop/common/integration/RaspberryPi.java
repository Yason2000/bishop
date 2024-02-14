package ua.net.yason.bishop.common.integration;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2CProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

@Slf4j
@Getter
public class RaspberryPi implements Closeable {

    private static final String I2C_PROVIDER = "linuxfs-i2c";

    private final Context pi4j;
    private final I2CProvider i2CProvider;

    public RaspberryPi() {
        log.debug("Initialize pi4j context");
        pi4j = Pi4J.newAutoContext();
        log.debug("Initialize i2C provider");
        i2CProvider = pi4j.provider(I2C_PROVIDER);
    }

    @Override
    public void close() {
        if (pi4j != null) {
            pi4j.shutdown();
        }
    }
}
