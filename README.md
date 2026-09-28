# Radial Menu

A configurable radial menu for Minecraft 1.7.10 / GT New Horizons. Client-side only; works on any server.

Requires GTNHLib and ModularUI2 (both ship with GTNH). Angelica-safe: the ring is drawn with the Tessellator and plain
GL state calls.

## Using it

- **Hold R** to open the menu. Point at a section and **left click** to run it. Letting go of R closes the menu.
- **Scroll** or press **Tab** (Shift+Tab to go back) to switch between menus.
- Sections can open **submenus**. Right click, or click the center, to go back.
- **Shift + click** a section to edit it. While Shift is held the hovered section gets a yellow edge and the text
  under the ring says the click will edit. Clicking an empty section also edits it.
- **Shift + click** the center, or use the (unbound by default) *Open Menu Manager* key, to manage menus.

All three keys are under *Radial Menu* in Controls.

## Sections

Each section has a name, an icon and an action.

- **Icons**: an item (drag it in from NEI, or use the item in your hand) or a short piece of colored text. With
  Angelica 2.2.19 or newer, text icons and section names can use its color codes, such as `&#FF8800TP` or `&q` for
  rainbow text.
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

The ring's colors are easiest to change in the menu manager: click a swatch next to *Colors* to open a color picker,
or click *Reset* to go back to the default colors.

Ring size, center size, colors, the open animation, labels and whether the last used menu is remembered are also in
the mod's config screen (Mods → Radial Menu → Config) or `config/radialmenu.cfg`. Colors there are written like
`#3C78D8` (the `#` is optional, and `#38D` works too), with a separate opacity from 0 to 100%.

## Building

Gradle needs JDK 25 to run (the mod itself targets Java 8):

```
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25..." ./gradlew build
```

The version comes from the git tag.
