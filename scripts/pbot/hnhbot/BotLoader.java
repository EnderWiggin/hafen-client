package haven.purus.pbot.hnhbot;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.util.GroovyScriptEngine;
import haven.purus.pbot.external.HnhBot;
import haven.purus.pbot.hnhbot.dto.HnhBotConfiguration;
import haven.purus.pbot.hnhbot.dto.ScriptManifest;

public class BotLoader {

    public static final BotLoader INSTANCE = new BotLoader();

    private static final String SCRIPTS_PATH = "gscripts/";
    private static final String SCRIPT_CONFIGURATION_FILE = "manifest.json";

    private final GroovyScriptEngine engine = createScriptEngine();
    private final ObjectMapper objectMapper;

    public BotLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public List<HnhBotConfiguration> loadConfigurations() {
        File scriptDirectory = new File(SCRIPTS_PATH);
        File[] scriptDirectories = scriptDirectory.listFiles();
        if (scriptDirectories == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(scriptDirectories)
                     .map(this::loadConfiguration)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .toList();
    }

    public Optional<HnhBot> loadBot(HnhBotConfiguration configuration) {
        try {
            Class<?> clazz = engine.loadScriptByName(configuration.path() + "/" + configuration.scriptClassName());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof HnhBot) {
                return Optional.of((HnhBot) instance);
            }
        } catch (Exception e) {
            System.out.println("Failed to load bot class");
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private Optional<HnhBotConfiguration> loadConfiguration(File directory) {
        try {
            File configFile = new File(directory, SCRIPT_CONFIGURATION_FILE);
            if (!configFile.exists()) {
                return Optional.empty();
            }

            var manifest = objectMapper.readValue(configFile, ScriptManifest.class);
            var configuration = new HnhBotConfiguration(manifest.name(), configFile.getParentFile().getName(),
                                                        manifest.botClassName());
            return Optional.of(configuration);
        } catch (Throwable e) {
            System.out.println("Failed to load bot configuration");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static GroovyScriptEngine createScriptEngine() {
        try {
            URL scriptUrl = new File(SCRIPTS_PATH).toURI().toURL();
            return new GroovyScriptEngine(new URL[]{scriptUrl});
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
