package ua.net.yason.bishop.common.integration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class Configuration {
    public static final String APP_NAME = "bishop";
    private static final String CONFIG_FILE_NAME = APP_NAME + ".conf";
    private static final String CONFIG_PATH = File.separator + "conf" + File.separator + CONFIG_FILE_NAME;

    public static Config getConfig() {
        List<Supplier<Optional<Config>>> configSuppliers = List.of(
                Configuration::getUserHomeConfig,
                Configuration::getAppHomeConfig,
                Configuration::getLocalConfig,
                Configuration::getParentDirConfig,
                Configuration::getCurrentDirConfig,
                Configuration::getJarFileDirsConfig
        );
        return configSuppliers.stream()
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseGet(Configuration::getDefaultConfig);
    }

    private static Config getDefaultConfig() {
        log.info("Default config will be taken");
        return ConfigFactory.load();
    }

    private static Optional<Config> parseConfig(String configFileName) {
        if (configFileName != null) {
            File file = new File(configFileName);
            if (file.exists()) {
                try {
                    log.debug("Trying to parse {}", configFileName);
                    Optional<Config> config = Optional.of(ConfigFactory.parseFile(file));
                    log.info("Loaded configuration from file: {}", configFileName);
                    return config;
                } catch (ConfigException ex) {
                    log.error("Failed to parse {}", file, ex);
                }
            } else {
                log.debug("Config file {} does not exits", configFileName);
            }
        }
        return Optional.empty();
    }

    private static Optional<Config> getUserHomeConfig() {
        log.debug("Looking for user home config");
        return parseConfig(System.getProperty("user.home")
                + File.separator + ".config" + File.separator + APP_NAME + File.separator + CONFIG_FILE_NAME);
    }

    private static Optional<Config> getAppHomeConfig() {
        log.debug("Looking for app home config");
        String appHome = System.getenv("APP_HOME");
        if (appHome == null) {
            log.debug("app home dir not defined");
            return Optional.empty();
        }
        return parseConfig(appHome + CONFIG_PATH);
    }

    private static Optional<Config> getLocalConfig() {
        log.debug("Looking for local dir config");
        return parseConfig("." + CONFIG_PATH);
    }

    private static Optional<Config> getParentDirConfig() {
        log.debug("Looking for parent dir config");
        return parseConfig(".." + CONFIG_PATH);
    }

    private static Optional<Config> getCurrentDirConfig() {
        log.debug("Looking for current dir config");
        return parseConfig(CONFIG_FILE_NAME);
    }

    private static Optional<String> getJarFileDir() {
        log.debug("Looking for jar file dir");
        try {
            File jarFile = new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.getName().endsWith(".jar")) {
                String parentDir = jarFile.getParent();
                log.debug("Jar file dir {}", parentDir);
                return Optional.ofNullable(parentDir);
            } else {
                log.debug("jar file path {} not found", jarFile);
            }
        } catch (URISyntaxException ex) {
            log.error("Error to get application jar file path", ex);
        }
        return Optional.empty();
    }

    private static Optional<Config> getJarFileCurrentDirConfig(String jarFileDir) {
        log.debug("Looking for jar file current dir config");
        return parseConfig(jarFileDir + File.separator + CONFIG_FILE_NAME);
    }

    private static Optional<Config> getJarFileConfDirConfig(String jarFileDir) {
        log.debug("Looking for jar file conf dir config");
        return parseConfig(new File(jarFileDir).getParent() + CONFIG_PATH);
    }

    private static Optional<Config> getJarFileDirsConfig() {
        return getJarFileDir()
                .map(jarDir -> getJarFileCurrentDirConfig(jarDir)
                        .orElseGet(() -> getJarFileConfDirConfig(jarDir)
                                .orElse(null))
                );
    }
}
