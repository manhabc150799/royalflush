# PHÂN TÍCH DỰ ÁN ROYAL FLUSHG

## 📋 TỔNG QUAN DỰ ÁN

**Royal FlushG** là một game bài (poker/card game) được xây dựng bằng **libGDX** framework, sử dụng kiến trúc **Autumn MVC** cho việc quản lý UI và logic. Dự án được thiết kế với khả năng mở rộng cho multiplayer thông qua **KryoNet**.

---

## 🏗️ CẤU TRÚC DỰ ÁN (Multi-Module Gradle)

Dự án sử dụng **Gradle Multi-Project** với 4 module chính:

### 1. **core** - Module Logic Chính
- **Vai trò**: Chứa toàn bộ logic game, UI controllers, services, và configuration
- **Dependencies**: Tất cả các thư viện chính (libGDX, Autumn MVC, VisUI, KryoNet, ...)
- **Cấu trúc**:
  ```
  core/src/main/java/com/mygame/client/
  ├── configuration/          # Cấu hình ứng dụng
  │   ├── Configuration.java  # Cấu hình chính (UI, i18n, sound, viewport)
  │   └── preferences/        # Quản lý preferences
  ├── controller/             # MVC Controllers
  │   ├── MenuController.java      # Controller cho menu chính
  │   ├── LoadingController.java   # Controller cho màn hình loading
  │   ├── action/                  # Global actions cho LML
  │   └── dialog/                  # Dialog controllers (Settings)
  ├── service/                # Business logic services
  │   └── ScaleService.java   # Quản lý scale UI
  └── RoyalFlushG.java        # Entry point class (scanning root)
  ```

### 2. **lwjgl3** - Desktop Launcher
- **Vai trò**: Launcher cho desktop (Windows/Mac/Linux)
- **Main Class**: `Lwjgl3Launcher.java`
- **Đặc điểm**: Sử dụng LWJGL3 backend, có thể build native executables

### 3. **server** - Server Module
- **Vai trò**: Server application riêng biệt (không phụ thuộc core)
- **Trạng thái**: Chưa implement (chỉ có TODO)
- **Mục đích**: Chạy game server cho multiplayer

### 4. **shared** - Module Dùng Chung
- **Vai trò**: Chứa code dùng chung giữa client (core) và server
- **Trạng thái**: Hiện tại trống
- **Mục đích**: Network messages, game state classes, shared models

---

## 📚 CÁC THƯ VIỆN VÀ FRAMEWORK CHÍNH

### 🎮 **libGDX** (v1.14.0)
- **Framework game engine chính**
- **Mục đích**: Rendering, input handling, audio, file I/O
- **Modules sử dụng**:
  - `gdx`: Core libGDX
  - `gdx-box2d`: Physics engine (Box2D)
  - `gdx-freetype`: Font rendering (FreeType)

### 🎨 **Autumn MVC** (v1.10.1.12.1)
- **Framework quản lý UI và dependency injection**
- **Kiến trúc**: Model-View-Controller pattern
- **Thành phần**:
  - `gdx-autumn`: Core dependency injection
  - `gdx-autumn-mvc`: MVC framework
  - `gdx-lml`: LML template parser (XML-based UI)
  - `gdx-lml-vis`: VisUI integration cho LML
  - `gdx-kiwi`: Utilities

**Cách hoạt động**:
- Sử dụng **annotation-based** scanning để tự động phát hiện components
- `@Component`: Đánh dấu class là component (service, controller)
- `@View`: Đánh dấu class là view controller, liên kết với LML template
- `@Inject`: Dependency injection tự động
- `@Initiate`: Method được gọi khi context khởi tạo
- `@Asset`: Tự động load assets
- `@ViewActionContainer`: Expose methods cho LML templates

**Ví dụ**:
```java
@View(id = "menu", value = "ui/templates/menu.lml")
public class MenuController implements ViewRenderer {
    @Asset("images/libgdx.png") private Texture logo;
    // ...
}
```

### 🖼️ **VisUI** (v1.5.7)
- **UI framework** cho libGDX
- **Tích hợp**: Với LML để tạo UI từ XML templates
- **Tính năng**: Buttons, windows, dialogs, tables, ...

### 📝 **LML (libGDX Markup Language)**
- **XML-based UI template system**
- **File location**: `assets/ui/templates/*.lml`
- **Templates hiện có**:
  - `menu.lml`: Menu chính
  - `loading.lml`: Màn hình loading
  - `dialogs/settings.lml`: Dialog cài đặt
  - `macros/global.lml`: Macros dùng chung

**Ví dụ LML**:
```xml
<textButton onChange="show:settings">@settings</textButton>
<textButton onChange="app:exit">@exit</textButton>
```

### 🌐 **KryoNet** (v2.22.7)
- **Networking library** cho multiplayer
- **Trạng thái**: Đã có dependency nhưng **chưa implement**
- **Mục đích**: Client-server communication cho multiplayer
- **Cần implement**:
  - Server trong `server` module
  - Client networking trong `core` module
  - Network messages trong `shared` module

### 🎯 **Các Thư Viện Khác**
- **TenPatch** (v5.2.3): 9-patch image support
- **libgdx-utils** (v0.13.7): Utilities cho libGDX
- **gdx-controllerutils-scene2d** (v2.3.0): Controller support cho Scene2D

---

## 🎨 ASSETS VÀ RESOURCES

### Cấu trúc Assets (`assets/`)
```
assets/
├── images/
│   ├── cards/light/          # Hình ảnh bài (52 lá + Joker + Back)
│   └── CatUI/                # UI elements (buttons, panels, icons)
├── ui/
│   ├── templates/            # LML templates
│   ├── uiskin.*             # VisUI skin files
│   └── Background_*.png     # Background images
├── music/
│   └── theme.ogg            # Background music
└── i18n/
    ├── bundle.properties     # Default locale
    ├── bundle_en.properties  # English
    └── bundle_pl.properties  # Polish
```

### Đặc điểm Assets
- **Cards**: Đầy đủ 52 lá bài + Joker + Back card
- **UI**: Bộ CatUI với nhiều elements (buttons, panels, chat boxes, ...)
- **i18n**: Hỗ trợ đa ngôn ngữ (English, Polish)
- **Auto-generation**: `assets.txt` được tự động generate bởi Gradle task

---

## 🔧 KIẾN TRÚC VÀ PATTERNS

### 1. **Dependency Injection (Autumn)**
- Tự động scan và inject dependencies
- Không cần manual wiring
- Lifecycle management tự động

### 2. **MVC Pattern**
- **Model**: Game state, data classes
- **View**: LML templates (XML)
- **Controller**: Java classes với `@View` annotation

### 3. **Service Layer**
- Business logic tách biệt trong services
- Ví dụ: `ScaleService` quản lý UI scale

### 4. **Preference Management**
- Tự động lưu/load preferences
- Annotation-based: `@Preference`, `@SoundVolume`, `@MusicVolume`, ...

---

## 🚀 CẤU HÌNH VÀ BUILD

### Java Version
- **Source/Target**: Java 17
- **Gradle**: 8.14.3

### Build Tasks
- `gradlew build`: Build tất cả modules
- `gradlew :lwjgl3:run`: Chạy desktop client
- `gradlew :server:run`: Chạy server (chưa implement)
- `gradlew :lwjgl3:jar`: Build JAR file

### Gradle Properties
- `gdxVersion=1.14.0`
- `kryoNetVersion=2.22.7`
- `lmlVersion=1.10.1.12.1`
- `visUiVersion=1.5.7`
- `projectVersion=1.0.0`

---

## 📝 CODE STRUCTURE PATTERNS

### View Controller Pattern
```java
@View(id = "viewId", value = "ui/templates/view.lml")
public class ViewController implements ViewRenderer {
    @Asset("path/to/asset") private Texture asset;
    
    @Override
    public void render(Stage stage, float delta) {
        // Custom rendering logic
    }
}
```

### Service Pattern
```java
@Component
public class MyService {
    @Inject private OtherService dependency;
    
    @Initiate
    public void initialize() {
        // Initialization logic
    }
}
```

### Action Container Pattern
```java
@ViewActionContainer("namespace")
public class GlobalActions implements ActionContainer {
    @LmlAction("actionName")
    public void doSomething() {
        // Action logic
    }
}
```

---

## 🎯 ĐIỂM MẠNH VÀ ĐIỂM CẦN PHÁT TRIỂN

### ✅ Điểm Mạnh
1. **Kiến trúc rõ ràng**: MVC pattern với DI
2. **UI linh hoạt**: LML templates dễ maintain
3. **Assets đầy đủ**: Có sẵn hình ảnh bài và UI
4. **Multi-module**: Tách biệt client/server
5. **i18n support**: Đa ngôn ngữ
6. **Modern stack**: Java 17, libGDX 1.14.0

### ⚠️ Cần Phát Triển
1. **Game Logic**: Chưa có logic game bài
2. **Multiplayer**: KryoNet chưa implement
3. **Server**: ServerLauncher chỉ có TODO
4. **Shared Module**: Trống, cần network messages
5. **Game State Management**: Chưa có state machine
6. **Card Game Logic**: Cần implement rules

---

## 🔮 HƯỚNG PHÁT TRIỂN ĐỀ XUẤT

### Phase 1: Core Game Logic
- Implement card game rules (poker/tien len)
- Game state management
- Player actions (bet, fold, call, ...)

### Phase 2: Single Player
- AI opponents
- Game flow
- Win/lose conditions

### Phase 3: Multiplayer Foundation
- Network messages trong `shared`
- Server implementation
- Client networking

### Phase 4: Multiplayer Features
- Room system
- Matchmaking
- Real-time synchronization

---

## 📖 TÀI LIỆU THAM KHẢO

- **libGDX**: https://libgdx.com/
- **Autumn MVC**: https://github.com/crashinvaders/gdx-lml
- **LML Tutorial**: https://github.com/crashinvaders/gdx-lml/wiki
- **VisUI**: https://github.com/kotcrab/vis-editor/wiki/VisUI
- **KryoNet**: https://github.com/EsotericSoftware/kryonet

---

## 💡 LƯU Ý KHI PHÁT TRIỂN

1. **Annotation Scanning**: Autumn tự động scan package của `RoyalFlushG`, đảm bảo các components nằm trong package con
2. **LML Actions**: Methods với `@LmlAction` có thể được gọi từ LML templates
3. **Asset Loading**: Sử dụng `@Asset` annotation, không cần manual loading
4. **Preferences**: Tự động save/load, sử dụng annotations
5. **View Navigation**: Sử dụng `goto:viewId` trong LML hoặc `InterfaceService`
6. **MusicFadingAction**: Đã được register trong `Configuration.java` để xử lý music transitions

---

**Tài liệu này cung cấp overview toàn diện về dự án để có thể viết master prompt cho developers mới.**

