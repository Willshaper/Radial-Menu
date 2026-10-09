# Radial Menu

A configurable radial menu for Minecraft 1.7.10 / GT New Horizons. Client-side only; works on any server.

This branch is the build for **GTNH 2.8.4**, made against the ModularUI2 (2.2.18) and GTNHLib (0.7.10) that 2.8.4
ships. Its releases are tagged with a `-gtnh2.8.4` suffix. For GTNH 2.9, use the `master` branch and the releases
without that suffix.

Requires GTNHLib and ModularUI2 (both ship with GTNH). The ring is drawn with the Tessellator and plain GL state calls,
so it works with Angelica.

## Download

Get the jar from the [Releases](../../releases) page and put it in your `mods` folder. For GTNH 2.8.4, use
`radialmenu-<version>-gtnh2.8.4.jar`; the `-dev` and `-sources` jars are for developers.

## Using it

- Hold R to open the menu. Point at a section and left click to run it. Letting go of R closes the menu.
- Scroll or press Tab (Shift+Tab to go back) to switch between menus.
- Sections can open submenus. Right click, or click the center, to go back.
- Shift + click a section to edit it. While Shift is held the hovered section gets a yellow edge and the text under
  the ring says the click will edit. Clicking an empty section also edits it.
- Shift + click the center, or use the *Open Menu Manager* key (unbound by default), to manage menus.

All three keys are under *Radial Menu* in Controls.

## Sections

Each section has a name, an icon and an action.

- **Icons**: an item (drag it in from NEI, or use the item in your hand) or a short piece of colored text. The
  Angelica in GTNH 2.8.4 predates Angelica's color codes, so text icons use the one color picked for them.
- **Actions**:
  - *Command*: runs like typing it in chat (client commands first, then the server).
  - *Chat Message*: sends a message.
  - *Press Keybind*: presses any vanilla or mod keybinding. *Tap* presses it once; *Toggle* holds it until you
    press the real key (useful for sneak). Mods that read the keyboard directly instead of through a keybinding
    can't be triggered this way.
  - *Select Hotbar Slot*: switches to slot 1 to 9.
  - *Open Submenu*: shows another menu inside the ring. *New* creates a submenu named after the section when you
    save.

*Clear* empties the section. It asks for a second click first.

## Menus

The menu manager adds, renames, reorders and deletes menus, sets each menu's section count (2 to 24) and whether the
switch key reaches it (*Switchable*). Menus that aren't switchable are only reachable as submenus and are marked with
↳ in the list. The arrows next to each section move it around the ring.

Lowering the section count hides sections without deleting them. If a hidden section still has something in it, the
menu manager says which ones, so they aren't forgotten.

Menus are stored in `config/radialmenu/menus.json`. A file that can't be read is renamed to
`menus.json.broken-<time>` and a fresh default menu is created, so nothing is silently overwritten.

## Settings

Click a swatch next to *Colors* in the menu manager to pick the ring's colors, or click *Reset* to go back to the
defaults.

Ring size, center size, colors, the open animation, labels and whether the last used menu is remembered are also in
the mod's config screen (Mods → Radial Menu → Config) or `config/radialmenu.cfg`. Colors there are written like
`#3C78D8` (the `#` is optional, and `#38D` works too), with a separate opacity from 0 to 100%.

## Building

Run `./gradlew build` with JDK 17 or newer. Gradle runs the build itself on JDK 25, using an installed copy or
downloading one; the mod still targets Java 8. The jar ends up in `build/libs`, and its version comes from the git tag.

GitHub Actions builds every push to `master`. Pushing a version tag, such as `1.4.0-gtnh2.8.4` on this branch,
builds that version and publishes it on the Releases page with the jars attached.

## License

LGPL-3.0. See [LICENSE](LICENSE).
