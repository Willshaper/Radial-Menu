# Radial Menu

A configurable radial menu for Minecraft 1.7.10 and GT New Horizons. It's client-side only, so it works on any server.

Requires GTNHLib and ModularUI2, which both ship with GTNH. The ring is drawn with the Tessellator and plain GL calls,
so it works with Angelica.

## Download

Get the jar from the [Releases](../../releases) page and put it in your `mods` folder. The `-dev` and `-sources` jars
are for developers.

| GTNH version | Use |
|---|---|
| 2.9 | `radialmenu-<version>.jar` from the newest release without a suffix |
| 2.8.4 | `radialmenu-<version>-gtnh2.8.4.jar` from a release tagged `-gtnh2.8.4`. It's built from the `gtnh-2.8.4` branch and doesn't have the 1.5 features. |

## Using it

- Hold R to open the menu, point at a section and left click to run it. Letting go of R closes the menu.
- Scroll, or press Tab (Shift+Tab goes back), to switch between menus.
- A section can open a submenu. Right click, or click the center, to go back.
- Shift + click a section to edit it. While Shift is held, the hovered section gets a yellow edge. Clicking an empty
  section edits it too.
- Shift + click the center, or press the *Open Menu Manager* key (unbound by default), to manage your menus.

The three keys are under *Radial Menu* in Controls. Two options in the config change how the menu behaves:

- *Release to Select*: letting go of R over a section runs it, as well as clicking.
- *Inline Submenus*: a submenu opens as a ring outside the one its section is in, instead of replacing the menu.
  Rest the mouse on the section or click it. Up to three of these rings can be open, and the menu shrinks to make
  room for them. Right click folds away the outermost one.

## Sections

Each section has a name, an icon and an action.

### Icons

The button next to the editor's icon slot opens the icon picker: a tab for each type, a search box and a grid. You
can also drag an item from NEI straight onto the icon slot. Right click the slot, or click ×, to remove the icon.

- **Items**: every item in the game and its variants, searchable by name or ID. The tab also has a slot for NEI
  drags and a *Held Item* button.
- **Symbols**: about 1,400 symbols from [Phosphor](https://phosphoricons.com), in the color of the swatch next to
  the search box.
- **Effects**: the icon of any potion effect, from vanilla or a mod.
- **Images**: your own PNGs, up to 512 × 512 pixels, from `config/radialmenu/icons`. *Open Folder* opens it and
  *Reload* picks up new files.
- **Heads**: a player's face. The tab lists the players on the server, and you can type any other name in the search
  box. Skins are looked up from Mojang the same way the game loads them, and remembered in
  `config/radialmenu/heads.json` so heads show straight away on the next launch. Each is checked again at most once
  a day. Names Mojang doesn't know show Steve.
- **Text**: a few characters in any color. With Angelica 2.2.19 or newer, text icons and section names can use its
  color codes, such as `&#FF8800TP`, or `&q` for rainbow text.

### Actions

- **Command**: runs like typing it in chat. Client commands are tried first, then the server.
- **Chat Message**: sends a message.
- **Press Keybind**: presses any vanilla or mod keybinding, bound to a key or not. *Tap* presses it once. *Toggle*
  holds it down, which is handy for sneak, until you use the section again, press the real key, or leave the world.
  Opening and closing screens doesn't release it.
- **Select Hotbar Slot**: switches to slot 1 to 9.
- **Open Submenu**: shows another menu. *New* creates one named after the section when you save.

*Clear* empties the section after a second click.

## Menus

The menu manager adds, renames, reorders and deletes menus and sets how many sections each has (2 to 24).

- *Submenu only* menus open only from a section in another menu. Tab and the scroll wheel skip them, and the list
  marks them with ↳.
- Hover a section to show its checkbox and the arrows that move it around the ring.
- To move sections to another menu, tick them, click *Move to...* and pick a menu or *+ New submenu*. They go into
  the first empty slots there. Hold Shift while picking to copy them instead.
- Lowering the section count hides sections without deleting them. If a hidden section isn't empty, the manager says
  which one, so it isn't forgotten.
- The swatches next to *Colors* set the ring's colors. The arrow next to them resets them.

Menus are saved in `config/radialmenu/menus.json`. If that file can't be read, it's renamed to
`menus.json.broken-<time>` and a new default menu is created, so nothing is overwritten.

### Backups

Before the first save in each game session, `menus.json` is copied to `config/radialmenu/backups`. The ten newest
backups are kept. *Backups* in the menu manager's title bar restores one, and backs up the current menus before it does, so a
restore can be undone the same way.

## Command

`/radialmenu` runs on your side only, so it works on any server.

- `/radialmenu edit` opens the menu manager.
- `/radialmenu reload` reads `menus.json` and your icon images again, and rechecks player heads.
- `/radialmenu backup` makes a backup now.
- `/radialmenu restore` lists the backups. Click one to restore it.

## Config

Mods → Radial Menu → Config, or `config/radialmenu.cfg`, has the ring size, center size, colors, open animation,
labels, whether the last menu used is remembered, *Release to Select* and *Inline Submenus*. Colors are written like
`#3C78D8`. The `#` is optional and `#38D` works too. Opacity is a separate setting from 0 to 100%.

The editor screens use a dark ModularUI theme stored in the jar as `assets/radialmenu/themes/dark.json`. A resource
pack can replace it.

## Building

Run `./gradlew build` with JDK 17 or newer. Gradle runs the build on JDK 25, using an installed copy or downloading
one, and the mod still targets Java 8. The build also runs the unit tests. The jar ends up in `build/libs`, and its
version comes from the git tag.

`tools/SymbolSheet.java` makes the symbol sheet from Phosphor's icon font. The file explains how to run it.

GitHub Actions builds every push. Pushing a version tag, such as `1.5.7`, builds that version and publishes it on
the Releases page with the jars attached.

## License

LGPL-3.0. See [LICENSE](LICENSE).

The symbols are [Phosphor Icons](https://phosphoricons.com), under the MIT license. Their license is in
`src/main/resources/assets/radialmenu/symbols/LICENSE-phosphor.txt` and in the jar.
