package niv.flowstone.config;

import static niv.flowstone.Flowstone.LOGGER;
import static niv.flowstone.Flowstone.MOD_ID;
import static niv.flowstone.Flowstone.MOD_NAME;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import net.fabricmc.loader.api.FabricLoader;

@NullMarked
public final class Loader<C> {

    private final Gson gson;

    private final File file;

    private final Class<C> configurationClass;

    private final Runnable invoker;

    private @NonNull C configuration;

    private long lastLoaded;

    @SuppressWarnings("null")
    Loader(Class<C> configurationClass, Runnable invoker, Supplier<@NonNull C> constructor) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setStrictness(Strictness.LENIENT)
                .create();
        this.file = FabricLoader.getInstance().getGameDir()
                .resolve("config")
                .resolve(MOD_ID + ".json")
                .toFile();
        this.configurationClass = configurationClass;
        this.invoker = invoker;
        this.configuration = constructor.get();
        this.lastLoaded = 0L;
    }

    public synchronized C getConfiguration() {
        var lastModified = this.file.lastModified();
        if (lastModified == 0L && tryWrite()) {
            this.lastLoaded = System.currentTimeMillis();
        } else if (lastModified > this.lastLoaded) {
            if (tryRead()) {
                this.lastLoaded = System.currentTimeMillis();
                this.invoker.run();
            } else if (tryWrite())
                this.lastLoaded = System.currentTimeMillis();
        }
        return configuration;
    }

    private boolean tryWrite() {
        try (var channel = FileChannel.open(file.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            var json = gson.toJson(this.configuration);
            var buffer = StandardCharsets.UTF_8.encode(json);
            channel.write(buffer);
            return true;
        } catch (Exception ex) {
            LOGGER.warn("({}) Failed to write configuration file", MOD_NAME, ex);
            return false;
        }
    }

    private boolean tryRead() {
        try (var channel = FileChannel.open(file.toPath(),
                StandardOpenOption.READ)) {
            var buffer = ByteBuffer.allocate(1024);
            var out = new ByteArrayOutputStream();
            while (channel.read(buffer) > 0) {
                out.write(buffer.array(), 0, buffer.position());
                buffer.clear();
            }
            var newConfiguration = gson.fromJson(out.toString(StandardCharsets.UTF_8), this.configurationClass);
            if (newConfiguration == null || this.configuration.equals(newConfiguration)) {
                return false;
            } else {
                this.configuration = newConfiguration;
                return true;
            }
        } catch (Exception ex) {
            LOGGER.warn("({}) Failed to read configuration file", MOD_NAME, ex);
            return false;
        }
    }
}
