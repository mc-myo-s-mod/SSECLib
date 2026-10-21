package me.myogoo.ssec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CommandRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistrar.class);

    /** Native SSEC registration intentionally replaces an existing mapping. */
    public static void registerAdapter(Class<?> type, SSCArgumentAdapter<?> adapter) {
        CommandArguments.registerAdapter(Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(adapter, "adapter"));
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher,
                                   CommandBuildContext buildContext, Collection<Class<?>> commandClasses,
                                   boolean developmentMode) {
        List<Class<?>> filtered = discover(commandClasses, developmentMode);
        List<Registration> registrations = new ArrayList<>();
        for (Class<?> clazz : filtered) {
            try {
                CommandMetadata.CommandInfo command = CommandMetadata.getCommandInfo(clazz);
                if (command != null && command.parent() == void.class) {
                    Registration registration = buildAndRegister(dispatcher, buildContext, clazz, filtered);
                    if (registration != null) {
                        registrations.add(registration);
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                logSkippedCommand(clazz, e);
            }
        }
        // Alias parent paths must not create unrestricted placeholders before real roots are registered.
        for (Registration registration : registrations) {
            registerAliases(dispatcher, registration);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Class<?> clazz) {
        register(dispatcher, null, clazz, List.of());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext,
                                Class<?> clazz) {
        register(dispatcher, buildContext, clazz, List.of());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext,
                                Class<?> clazz, List<Class<?>> allCommandClasses) {
        List<Class<?>> candidates = new ArrayList<>(allCommandClasses);
        candidates.add(clazz);
        Registration registration = buildAndRegister(dispatcher, buildContext, clazz,
                discover(candidates, true));
        if (registration != null) {
            registerAliases(dispatcher, registration);
        }
    }

    private static Registration buildAndRegister(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext buildContext, Class<?> clazz,
                                         List<Class<?>> commandClasses) {
        try {
            Set<Class<?>> builtClasses = new LinkedHashSet<>();
            LiteralArgumentBuilder<CommandSourceStack> builder = CommandNodeBuilder.build(
                    buildContext, clazz, commandClasses, builtClasses, new LinkedHashSet<>());
            if (builder == null) {
                return null;
            }
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(builder);
            LOGGER.info("Registered SSEC command node: {}", node.getName());
            return new Registration(clazz, node, builtClasses);
        } catch (RuntimeException | LinkageError e) {
            logSkippedCommand(clazz, e);
            return null;
        }
    }

    private static void registerAliases(CommandDispatcher<CommandSourceStack> dispatcher,
                                        Registration registration) {
        CommandAliasRegistrar.registerAliases(dispatcher, registration.owner(), registration.node(),
                registration.classes());
    }

    private record Registration(Class<?> owner, LiteralCommandNode<CommandSourceStack> node, Set<Class<?>> classes) {
    }

    private static List<Class<?>> discover(Collection<Class<?>> commandClasses, boolean developmentMode) {
        Set<Class<?>> visited = new LinkedHashSet<>();
        Set<Class<?>> filtered = new LinkedHashSet<>();
        for (Class<?> clazz : commandClasses) {
            discover(clazz, developmentMode, visited, filtered);
        }
        return new ArrayList<>(filtered);
    }

    private static void discover(Class<?> clazz, boolean developmentMode,
                                 Set<Class<?>> visited, Set<Class<?>> filtered) {
        if (clazz == null || !visited.add(clazz)) {
            return;
        }
        try {
            if (CommandMetadata.getCommandInfo(clazz) == null || (!developmentMode && CommandMetadata.isDebugOnly(clazz))) {
                return;
            }
            filtered.add(clazz);
            for (Class<?> inner : clazz.getDeclaredClasses()) {
                discover(inner, developmentMode, visited, filtered);
            }
        } catch (RuntimeException | LinkageError e) {
            filtered.remove(clazz);
            logSkippedCommand(clazz, e);
        }
    }

    private static void logSkippedCommand(Class<?> clazz, Throwable cause) {
        LOGGER.error("Skipping SSEC command class {} because it could not be registered.", clazz.getName(), cause);
    }
}
