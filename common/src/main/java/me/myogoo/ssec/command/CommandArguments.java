package me.myogoo.ssec.command;

import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.command.argument.SSCBooleanArgument;
import me.myogoo.ssec.command.argument.SSCDoubleArgument;
import me.myogoo.ssec.command.argument.SSCFloatArgument;
import me.myogoo.ssec.command.argument.SSCIntArgument;
import me.myogoo.ssec.command.argument.SSCStringArgument;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CommandArguments {
    private static final Map<Class<?>, SSCArgumentAdapter<?>> ADAPTERS = new ConcurrentHashMap<>();

    static {
        registerAdapter(int.class, new SSCIntArgument());
        registerAdapter(Integer.class, new SSCIntArgument());
        registerAdapter(double.class, new SSCDoubleArgument());
        registerAdapter(Double.class, new SSCDoubleArgument());
        registerAdapter(float.class, new SSCFloatArgument());
        registerAdapter(Float.class, new SSCFloatArgument());
        registerAdapter(boolean.class, new SSCBooleanArgument());
        registerAdapter(Boolean.class, new SSCBooleanArgument());
        registerAdapter(String.class, new SSCStringArgument());
        DefaultArguments.register();
    }

    private CommandArguments() {
    }

    static void registerAdapter(Class<?> type, SSCArgumentAdapter<?> adapter) {
        ADAPTERS.put(type, adapter);
    }

    static SSCArgumentAdapter<?> getAdapter(Class<?> type) {
        return ADAPTERS.get(type);
    }

    record ParameterMapping(int index, String name, Class<?> contextType, SSCArgumentAdapter<?> adapter) {
        static ParameterMapping context(int index, Class<?> type) {
            return new ParameterMapping(index, null, type, null);
        }

        static ParameterMapping argument(int index, String name, SSCArgumentAdapter<?> adapter) {
            return new ParameterMapping(index, name, null, adapter);
        }

        Object value(CommandContext<CommandSourceStack> context) throws Exception {
            if (contextType == CommandContext.class) {
                return context;
            }
            if (contextType == CommandSourceStack.class) {
                return context.getSource();
            }
            return adapter.value(context, name);
        }
    }
}
