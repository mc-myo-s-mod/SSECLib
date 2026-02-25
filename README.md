# SSECLib (Super Sexy Event & Command Library)

An **annotation-driven command & event framework** for Minecraft Fabric.  
Register commands and events with annotations only — no Brigadier boilerplate required.

> 🇰🇷 [한국어 문서 (Korean)](readme/kor.md)

---

## ✨ Features

| Feature | Description |
|---|---|
| **Annotation-based Commands** | Auto-register commands with `@SSCommand` + `@SSCExecute` |
| **Subcommand Support** | Nested subcommands via `parent` (inner class / external class) |
| **Alias System** | `@SSCAlias` with absolute (`/`) and relative path support |
| **Permission System** | `@SSCPermission` — OP Level, LuckPerms, Custom Checker |
| **Auto Argument Mapping** | `@SSCArgument` for automatic parameter mapping (int, String, Vec3, Entity, etc.) |
| **Event Registration** | `@SSEvent` for automatic Fabric event listener registration |
| **Debug Mode** | `@SSECDebug` for dev-environment-only commands/events |
| **Classpath Scanning** | `SSECScanner` auto-discovers and registers annotated classes |

---

## 📦 Quick Start

### 1. Entry Point Setup

Register your initializer classes in `fabric.mod.json`:

```json
{
  "entrypoints": {
    "ssec": [
      "com.example.mymod.event.MyEventInitializer",
      "com.example.mymod.command.MyCommandInitializer"
    ]
  }
}
```

Implement `SSECInitializer` to define packages to scan and perform additional setup:

```java
public class MyCommandInitializer implements SSECInitializer {
    @Override
    public void onInitializeSSEC() {
        // Register custom argument adapters, additional setup, etc.
        // CommandRegistrar.registerAdapter(MyType.class, new MyTypeAdapter());
    }

    @Override
    public String[] getPackagesToScan() {
        // Specify packages containing @SSCommand / @SSEvent annotated classes
        return new String[] { "com.example.mymod.command" };
    }
}
```

- `getPackagesToScan()` — Returns packages that `SSECScanner` will scan for annotated classes
- `onInitializeSSEC()` — Called during initialization; use it to register custom `SSCArgumentAdapter`s or perform any additional setup

### 2. Defining a Command

```java
@SSCommand("greeting")
@SSCAlias({ "hi", "hello" })
@SSCPermission(permission = PermissionLevel.GAME_MASTER)
public class GreetingCommand {

    @SSCExecute
    public static void execute(CommandContext<CommandSourceStack> ctx,
                               @SSCArgument("name") String name) {
        ctx.getSource().sendSuccess(
            () -> Component.literal("Hello, " + name + "!"), false);
    }

    @SSCommand(value = "shout", parent = GreetingCommand.class)
    public static class ShoutCommand {
        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx,
                                   @SSCArgument("msg") String msg) {
            ctx.getSource().sendSuccess(
                () -> Component.literal("§l" + msg.toUpperCase()), false);
        }
    }
}
```

This registers the following commands automatically:
- `/greeting <name>` (aliases: `/hi`, `/hello`)
- `/greeting shout <msg>`

### 3. Defining an Event Listener

```java
public class MyEvents {
    @SSEvent(ServerLifecycleEvents.ServerStarting.class)
    public static void onServerStart(MinecraftServer server) {
        // Server starting logic
    }
}
```

---

## 🔧 API Annotations

### Command

| Annotation | Target | Description |
|---|---|---|
| `@SSCommand` | Class | Define a command/subcommand. `value` = name, `parent` = parent class |
| `@SSCExecute` | Method | Mark the execution method (must be `static`) |
| `@SSCArgument` | Parameter | Map a parameter to a Brigadier argument |
| `@SSCAlias` | Class | Register command aliases (via redirect) |
| `@SSCPermission` | Class/Method | Configure permissions |

### Event & Utility

| Annotation | Target | Description |
|---|---|---|
| `@SSEvent` | Method | Register a Fabric event listener. Control order with `priority` |
| `@SSECDebug` | Class/Method | Active only in dev environment (`isDevelopmentEnvironment`) |

---

## 🛡️ Permission System

`@SSCPermission` supports **one of** three modes:

```java
// 1. Vanilla OP Level
@SSCPermission(permission = PermissionLevel.GAME_MASTER)

// 2. LuckPerms / Fabric Permissions API
@SSCPermission(value = "mymod.admin")

// 3. Custom Checker
@SSCPermission(custom = MyPermissionChecker.class)
```

### Permission Levels

| Level | Description |
|---|---|
| `NONE` | Unspecified (default) |
| `MODERATOR` | Moderator |
| `GAME_MASTER` | Game Master |
| `ADMIN` | Admin |
| `OWNER` | Owner |

### `propagate` Option

- `propagate = false` (default): Permission applies to the current command only
- `propagate = true`: Permission cascades to all subcommands

---

## 🔀 Alias System

`@SSCAlias` uses a `/`-based path system.

| Rule | Example | Result |
|---|---|---|
| **Absolute** (starts with `/`) | `@SSCAlias("/s")` | `/s` |
| **Absolute multi-segment** | `@SSCAlias("/other/sub")` | `/other sub` (requires `other` command) |
| **Relative** (no leading `/`) | `@SSCAlias("alt")` on child of "test" | `/test alt` |
| **Relative multi-segment** | `@SSCAlias("a/b")` on child of "test" | `/test a b` |

> ⚠️ An `IllegalStateException` is thrown if the root command referenced by an absolute path does not exist.

---

## 🎯 Supported Argument Types

| Java Type | Brigadier Mapping |
|---|---|
| `int` / `Integer` | `IntegerArgumentType` |
| `double` / `Double` | `DoubleArgumentType` |
| `float` / `Float` | `FloatArgumentType` |
| `boolean` / `Boolean` | `BoolArgumentType` |
| `String` | `StringArgumentType` |
| `Vec3` | `Vec3Argument` |
| `Entity` | `EntityArgument.entity()` |
| `Entity[]` | `EntityArgument.entities()` |
| `ServerPlayer` | `EntityArgument.player()` |
| `ServerPlayer[]` | `EntityArgument.players()` |

Register custom types via `SSCArgumentAdapter` and `CommandRegistrar.registerAdapter()`.

---

## 📁 Project Structure

```
me.myogoo.ssec
├── api/                          # Public API
│   ├── SSECDebug                 # Debug-only annotation
│   ├── SSECInitializer           # Initializer interface
│   ├── command/                  # Command annotations
│   │   ├── SSCommand             # Command definition
│   │   ├── SSCExecute            # Execution method marker
│   │   ├── SSCArgument           # Argument binding
│   │   ├── SSCAlias              # Alias definition
│   │   ├── SSCPermission         # Permission control
│   │   ├── argument/             # Argument adapter API
│   │   └── permission/           # Permission enums & interfaces
│   └── event/
│       └── SSEvent               # Event listener annotation
├── command/                      # Internal implementation
│   ├── CommandRegistrar          # Command registration engine
│   └── argument/                 # Built-in argument adapters
├── event/
│   └── EventRegistrar            # Event registration engine
└── util/
    └── SSECScanner               # Classpath scanner
```

---

## License

This project is licensed under the [LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html).
