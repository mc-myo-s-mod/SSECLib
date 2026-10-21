# SSECLib (Super Sexy Event & Command Library)

## 독립 프로젝트와 내부 명령어 라이브러리

SSEC는 `Minecraft/SSECLib`의 별도 저장소로 분리되어 있습니다. 기존 루트 빌드는
Fabric 26.1.2 이벤트 모드(`me.myogoo:ssec:26.1.2+0.1`)를 유지합니다. `common/src/main/java`의 명령어 엔진은
`neoforge-1-21-1`(Java 21), `forge-1-20-1`(Java 17), `neoforge-26-1-2`(Java 25)에서
각각 라이브러리로 빌드합니다. 각 폴더에서 `gradlew.bat build`를 실행합니다.

Myotus는 옆 폴더의 SSEC 소스를 Gradle composite build로 참조하고 Jar-in-Jar로
내장합니다. 사용자가 SSEC를 따로 설치할 필요는 없습니다. 내부 라이브러리는
자동 스캔이나 모드 초기화를 실행하지 않으며, 호스트가 명령어 등록 이벤트에서
`CommandRegistrar.registerAll`에 디스패처·빌드 컨텍스트·명령어 클래스·개발 모드
여부를 전달합니다. 아래 Fabric entrypoint 안내는 독립 이벤트 모드용입니다.

루트 Fabric 모드도 같은 26.1.2 명령 라이브러리를 Fabric Jar-in-Jar로 내장합니다.
라이브러리는 Minecraft·Brigadier·SLF4J API만 사용하며 NeoForge 런타임에는
의존하지 않습니다. 폴더 이름은 컴파일 대상을 나타냅니다. 이벤트 기능과 자동
스캔은 Fabric 루트에만 있으며 Forge/NeoForge 라이브러리에는 포함하지 않습니다.
명령어는 SSEC 어노테이션을 씁니다.

Forge는 개발용 Mojang 매핑 JAR과 내장 배포용 SRG 매핑 JAR을 구분합니다.
Myotus의 Jar-in-Jar만 `production` capability를 선택합니다. 1.20.1에 없는
Minecraft 타입을 쓰는 display-slot·score-holder·slot-range·style 어댑터 4종은
해당 버전에서 제외합니다.
명령어 라이브러리 좌표는 현재 로컬/composite용 SNAPSHOT입니다. 이를 의존하는
Maven 릴리스 전에 라이브러리도 발행하고, Myotus CI의 `SSEC_REF`를 같은 소스
리비전으로 고정해야 합니다.

Minecraft Fabric 기반의 **어노테이션 드리븐 커맨드 & 이벤트 프레임워크**입니다.
Brigadier 보일러플레이트 없이 어노테이션만으로 커맨드와 이벤트를 등록할 수 있습니다.

> 🇺🇸 [English](../README.md)

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| **어노테이션 기반 커맨드** | `@SSCommand` + `@SSCExecute`로 커맨드 자동 등록 |
| **서브커맨드** | `parent` 지정으로 중첩 서브커맨드 (inner class / external class) |
| **Alias 시스템** | `@SSCAlias`로 절대경로(`/`) / 상대경로 alias 지원 |
| **권한 시스템** | `@SSCPermission` — OP Level, LuckPerms, Custom Checker 지원 |
| **자동 Argument 매핑** | `@SSCArgument`로 파라미터 자동 매핑 (int, String, Vec3, Entity 등) |
| **이벤트 등록** | `@SSEvent`로 Fabric 이벤트 리스너 자동 등록 |
| **디버그 모드** | `@SSECDebug`로 개발 환경에서만 동작하는 커맨드/이벤트 |
| **클래스패스 스캐닝** | `SSECScanner`가 어노테이션을 자동 스캔하여 등록 |

---

## 📦 빠른 시작

### 1. Entry Point 설정

`fabric.mod.json`에 초기화 클래스를 등록합니다:

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

`SSECInitializer`를 구현하여 스캔할 패키지와 추가 설정을 정의합니다:

```java
public class MyCommandInitializer implements SSECInitializer {
    @Override
    public void onInitializeSSEC() {
        // 커스텀 Argument 어댑터 등록, 추가 설정 등
        // CommandRegistrar.registerAdapter(MyType.class, new MyTypeAdapter());
    }

    @Override
    public String[] getPackagesToScan() {
        // @SSCommand / @SSEvent 어노테이션이 있는 클래스가 위치한 패키지 지정
        return new String[] { "com.example.mymod.command" };
    }
}
```

- `getPackagesToScan()` — `SSECScanner`가 스캔할 패키지 목록 반환
- `onInitializeSSEC()` — 초기화 시 호출. 커스텀 `SSCArgumentAdapter` 등록이나 추가 설정에 사용

### 2. 커맨드 정의

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

위 코드로 다음 커맨드가 자동 등록됩니다:
- `/greeting <name>` (alias: `/hi`, `/hello`)
- `/greeting shout <msg>`

### 3. 이벤트 정의

```java
public class MyEvents {
    @SSEvent(ServerLifecycleEvents.ServerStarting.class)
    public static void onServerStart(MinecraftServer server) {
        // 서버 시작 로직
    }
}
```

---

## 🔧 API 어노테이션

### 커맨드

| 어노테이션 | 대상 | 설명 |
|---|---|---|
| `@SSCommand` | Class | 커맨드/서브커맨드 정의. `value`=이름, `parent`=부모 커맨드 클래스 |
| `@SSCExecute` | Method | 커맨드 실행 메서드 지정 (반드시 `static`) |
| `@SSCArgument` | Parameter | 파라미터를 Brigadier Argument로 매핑 |
| `@SSCAlias` | Class | 커맨드 alias 등록 (redirect 방식) |
| `@SSCPermission` | Class/Method | 권한 설정 |

### 이벤트 & 유틸리티

| 어노테이션 | 대상 | 설명 |
|---|---|---|
| `@SSEvent` | Method | Fabric 이벤트 리스너 등록. `priority`로 실행 순서 제어 |
| `@SSECDebug` | Class/Method | 개발 환경(`isDevelopmentEnvironment`)에서만 활성화 |

---

## 🛡️ 권한 시스템

`@SSCPermission`은 3가지 모드 중 **하나만** 사용 가능합니다:

```java
// 1. 바닐라 OP 레벨
@SSCPermission(permission = PermissionLevel.GAME_MASTER)

// 2. LuckPerms / Fabric Permissions API
@SSCPermission(value = "mymod.admin")

// 3. 커스텀 체커
@SSCPermission(custom = MyPermissionChecker.class)
```

### 권한 레벨

| 레벨 | 설명 |
|---|---|
| `NONE` | 미지정 (기본값) |
| `MODERATOR` | 모더레이터 권한 |
| `GAME_MASTER` | 게임 마스터 권한 |
| `ADMIN` | 관리자 권한 |
| `OWNER` | 오너 권한 |

### `propagate` 옵션

- `propagate = false` (기본값): 현재 커맨드에만 권한 적용
- `propagate = true`: 하위 서브커맨드까지 권한 전파

---

## 🔀 Alias 시스템

`@SSCAlias`는 `/` 기반 경로 시스템을 사용합니다.

| 규칙 | 예시 | 결과 |
|---|---|---|
| **절대 경로** (`/`로 시작) | `@SSCAlias("/s")` | `/s` |
| **절대 경로 (다중)** | `@SSCAlias("/other/sub")` | `/other sub` |
| **상대 경로** (`/` 없이) | `@SSCAlias("alt")` (parent: "test") | `/test alt` |
| **상대 경로 (다중)** | `@SSCAlias("a/b")` (parent: "test") | `/test a b` |

> 별칭의 루트가 없으면 생성합니다. 기존 명령어 경로를 별칭으로 덮어쓰지 않으며 상위 권한 검사를 유지합니다.

---

## 🎯 지원 Argument 타입

| Java 타입 | Brigadier 매핑 |
|---|---|
| `int` / `Integer` | `IntegerArgumentType` |
| `double` / `Double` | `DoubleArgumentType` |
| `float` / `Float` | `FloatArgumentType` |
| `boolean` / `Boolean` | `BoolArgumentType` |
| `String` | `StringArgumentType` |
| `UUID` | `UuidArgument` |
| `ChatFormatting` | `ColorArgument` |
| `Component` | `ComponentArgument` |
| `Style` | `StyleArgument` |
| `CompoundTag` | `CompoundTagArgument` |
| `NbtPathArgument.NbtPath` | `NbtPathArgument` |
| `BlockInput` | `BlockStateArgument` |
| `BlockPos` | `BlockPosArgument` |
| `ColumnPos` | `ColumnPosArgument` |
| `Vec2` | `Vec2Argument` |
| `Vec3` | `Vec3Argument` |
| `AngleArgument.SingleAngle` | `AngleArgument` |
| `EntityAnchorArgument.Anchor` | `EntityAnchorArgument` |
| `Entity` | `EntityArgument.entity()` |
| `Entity[]` | `EntityArgument.entities()` |
| `ServerPlayer` | `EntityArgument.player()` |
| `ServerPlayer[]` | `EntityArgument.players()` |
| `ServerLevel` | `DimensionArgument` |
| `GameType` | `GameModeArgument` |
| `Heightmap.Types` | `HeightmapTypeArgument` |
| `DisplaySlot` | `ScoreboardSlotArgument` |
| `ScoreHolder` | `ScoreHolderArgument` |
| `PlayerTeam` | `TeamArgument` |
| `SlotRange` | `SlotsArgument` |
| `Mirror` | `TemplateMirrorArgument` |
| `Rotation` | `TemplateRotationArgument` |

커스텀 타입은 `SSCArgumentAdapter`를 구현하여 `CommandRegistrar.registerAdapter()`로 등록할 수 있습니다.

---

## 📁 프로젝트 구조

```
me.myogoo.ssec
├── api/                          # 공개 API
│   ├── SSECDebug                 # 디버그 전용 어노테이션
│   ├── SSECInitializer           # 초기화 인터페이스
│   ├── command/                  # 커맨드 어노테이션
│   │   ├── SSCommand             # 커맨드 정의
│   │   ├── SSCExecute            # 실행 메서드 마커
│   │   ├── SSCArgument           # Argument 바인딩
│   │   ├── SSCAlias              # Alias 정의
│   │   ├── SSCPermission         # 권한 제어
│   │   ├── argument/             # Argument 어댑터 API
│   │   └── permission/           # 권한 Enum & 인터페이스
│   └── event/
│       └── SSEvent               # 이벤트 리스너 어노테이션
├── command/                      # 내부 구현
│   ├── CommandRegistrar          # 커맨드 등록 엔진
│   └── argument/                 # 내장 Argument 어댑터
├── event/
│   └── EventRegistrar            # 이벤트 등록 엔진
└── util/
    └── SSECScanner               # 클래스패스 스캐너
```

---

## License

이 프로젝트는 [LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) 라이선스를 따릅니다.
