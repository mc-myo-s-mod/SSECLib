package me.myogoo.ssec.mixin.argument;

import net.minecraft.commands.arguments.AngleArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AngleArgument.SingleAngle.class)
public interface AngleArgumentAccessor {
    @Accessor("angle")
    float getAngle();

    @Invoker("<init>")
    static AngleArgument.SingleAngle create(float angle, boolean isRelative) {
        throw new UnsupportedOperationException();
    }
}
