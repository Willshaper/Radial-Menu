# Radial Menu

A configurable radial menu for Minecraft 1.7.10 / GT New Horizons. Client-side only; works on any server.

Requires GTNHLib and ModularUI2 (both ship with GTNH). Angelica-safe: the ring is drawn with the Tessellator and plain
GL state calls.

## Using it

- **Hold R** to open the menu. Point at a section and **left click** to run it. Letting go of R closes the menu.
- **Scroll** or press **Tab** (Shift+Tab to go back) to switch between menus.
- Sections can open **submenus**. Right click, or click the center, to go back.
- **Shift + click** a section to edit it. Clicking an empty section also edits it.
- **Shift + click** the center, or use the (unbound by default) *Open Menu Manager* key, to manage menus.

All three keys are under *Radial Menu* in Controls.

## Sections

Each section has a name, an icon and an action.

- **Icons**: an item (drag it in from NEI, or use the item in your hand) or a short piece of colored text.
- **Actions**:
  - *Command*: runs like typing it in chat (client commands first, then the server).
  - *Chat Message*: sends a message.
  - *Press Keybind*: presses any vanilla or mod keybinding. *Tap* presses it once; *Toggle* holds it until you
    press the real key (useful for sneak). Mods that read the keyboard directly instead of through a keybinding
    can't be triggered this way.
  - *Select Hotbar Slot*: switches to slot 1 to 9.
  - *Open Submenu*: shows another menu inside the ring.

## Menus

The menu manager adds, renames, reorders and deletes menus, sets each menu's section count (2 to 24) and whether the
switch key reaches it (*Switchable*). Menus that aren't switchable are only reachable as submenus. Lowering the
section count hides sections without deleting them.

Menus are stored in `config/radialmenu/menus.json`. A file that can't be read is renamed to
`menus.json.broken-<time>` and a fresh default menu is created, so nothing is silently overwritten.

## Settings

Ring size, center size, colors, the open animation, labels and whether the last used menu is remembered are in the
mod's config screen (Mods → Radial Menu → Config) or `config/radialmenu.cfg`.

## Building

Gradle needs JDK 25 to run (the mod itself targets Java 8):

```
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25..." ./gradlew build
```

The version comes from the git tag.
