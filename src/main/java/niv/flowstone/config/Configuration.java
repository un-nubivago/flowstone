package niv.flowstone.config;

import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@NullMarked
public final class Configuration {

    public static final Event<@NonNull Runnable> LOADED = EventFactory
            .createArrayBacked(Runnable.class,
                    runnables -> () -> Stream.of(runnables).forEach(Runnable::run));

    private boolean allowDeepslateGenerators = true;
    private boolean allowWorldlyGenerators = true;
    private boolean allowCustomGenerators = false;

    private boolean enableBasaltGeneration = false;
    private boolean enableNetherrackGeneration = true;
    private boolean enableEndStoneGeneration = true;

    private @Nullable Boolean debugMode = null;

    Configuration() {
    }

    private boolean getDebugMode() {
        return this.debugMode != null && this.debugMode;
    }

    private static final Configuration getInstance() {
        return ConfigurationLoader.getConfiguration();
    }

    public static final void init() {
        getInstance();
    }

    public static final boolean allowDeepslateGenerators() {
        return getInstance().allowDeepslateGenerators;
    }

    public static final boolean allowWorldlyGenerators() {
        return getInstance().allowWorldlyGenerators;
    }

    public static final boolean allowCustomGenerators() {
        return getInstance().allowCustomGenerators;
    }

    public static final boolean enableBasaltGeneration() {
        return getInstance().enableBasaltGeneration;
    }

    public static final boolean enableNetherrackGeneration() {
        return getInstance().enableNetherrackGeneration;
    }

    public static final boolean enableEndStoneGeneration() {
        return getInstance().enableEndStoneGeneration;
    }

    public static final boolean debugMode() {
        return getInstance().getDebugMode();
    }
}
