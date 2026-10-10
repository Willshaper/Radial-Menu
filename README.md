# Radial Menu

A configurable radial menu for Minecraft 1.7.10 / GT New Horizons. Client-side only; works on any server.

Requires GTNHLib and ModularUI2 (both ship with GTNH). The ring is drawn with the Tessellator and plain GL state calls,
so it works with Angelica.

## Download

Get the jar from the [Releases](../../releases) page and put it in your `mods` folder. The `-dev` and `-sources` jars
are for developers.

| GTNH version | Use |
|---|---|
| 2.9 | `radialmenu-<version>.jar`, from releases without a suffix |
| 2.8.4 | `radialmenu-<version>-gtnh2.8.4.jar`, from releases tagged `-gtnh2.8.4` (built from the `gtnh-2.8.4` branch) |

## Using it

- Hold R to open the menu. Point at a section and left click to run it. Letting go of R closes the menu. With
  *Release to Select* on, letting go of R over a section also runs it.
- Scroll or press Tab (Shift+Tab to go back) to switch between menus.
- Sections can open submenus. Right click, or click the center, to go back.
- With *Inline Submenus* on, a submenu never replaces the menu. Resting the mouse on a submenu section for a moment,
  or clicking it, shows the submenu as a ring outside the ring the section is in. Submenus inside that ring open as
  further rings, up to three, and the whole menu shrinks to make room for as many as it can open. Right click folds
  away the outermost ring.
- Shift + click a section to edit it. While Shift is held the hovered section gets a yellow edge and the text under
  the ring says the click will edit. Clicking an empty section also edits it.
- Shift + click the center, or use the *Open Menu Manager* key (unbound by default), to manage menus.

All three keys are under *Radial Menu* in Controls.

## Sections

Each section has a name, an icon and an action.

- **Icons**: the button next to the icon slot opens the icon picker, with a tab per type, a search box and a grid. Dragging an item from NEI
  onto the editor's icon slot also works, and right clicking the slot or clicking × removes the icon.
  - *Items*: every item in the game and its variants, searchable by name or id. Drag one in from NEI or use *Held
    Item* instead if that's quicker.
  - *Symbols*: about 1,400 symbols from [Phosphor](https://phosphoricons.com), in the color of the swatch next to
    the search box.
  - *Effects*: the icon of a potion effect, from vanilla or any mod.
  - *Images*: PNGs of your own, up to 512 × 512 pixels, from `config/radialmenu/icons` (*Open Folder* opens it).
  - *Heads*: a player's face. The tab lists the players on the server; type any other name in the search box. If
    the player is nearby their skin is used, otherwise it's looked up by name from Mojang, the same way the game loads
    skins. Names Mojang doesn't know show Steve. Looked-up skins are remembered in `config/radialmenu/heads.json`,
    so heads show straight away on the next launch; each is checked again at most once a day to pick up skin
    changes, and `/radialmenu reload` checks them all again.
  - *Text*: a short piece of colored text. With Angelica 2.2.19 or newer, text icons and section names can use its
    color codes, such as `&#FF8800TP` or `&q` for rainbow text.
- **Actions**:
  - *Command*: runs like typing it in chat (client commands first, then the server).
  - *Chat Message*: sends a message.
  - *Press Keybind*: presses any vanilla or mod keybinding, bound to a key or not. *Tap* presses it once; *Toggle*
    holds it down (useful for sneak) until you use the section again, press the real key, or leave the world.
    Opening and closing screens doesn't let go of it.
  - *Select Hotbar Slot*: switches to slot 1 to 9.
  - *Open Submenu*: shows another menu inside the ring. *New* creates a submenu named after the section when you
    save.

*Clear* empties the section. It asks for a second click first.

## Menus

The menu manager adds, renames, reorders and deletes menus, and sets each menu's section count (2 to 24). *Submenu
only* menus open only from a section in another menu: Tab and the scroll wheel skip them, and they're marked with ↳
in the list. The arrows that show when hovering a section move it around the ring.

To move sections to another menu, tick their boxes and click *Move to...*, then pick a menu or *+ New submenu*. They
go into the first empty slots there, and its section count grows if needed. Hold Shift while picking to copy them
instead.

Lowering the section count hides sections without deleting them. If a hidden section still has something in it, the
menu manager says which ones, so they aren't forgotten.

Menus are stored in `config/radialmenu/menus.json`. A file that can't be read is renamed to
`menus.json.broken-<time>` and a fresh default menu is created, so nothing is silently overwritten.

### Backups

The first time the menus are saved in a game session, the file as it was is copied to `config/radialmenu/backups`.
The three newest backups are kept. *Backups...* in the menu manager restores one. The menus from before the restore
are backed up first, so a restore can be undone the same way.

## Command

`/radialmenu` works on any server, since it runs on your side only.

- `/radialmenu edit` opens the menu manager.
- `/radialmenu reload` reads `menus.json` and the icon images again, after editing them outside the game.
- `/radialmenu backup` backs up the menus now.
- `/radialmenu restore` lists the backups; click one to restore it.

## Settings

Click a swatch next to *Colors* in the menu manager to pick the ring's colors, or click *Reset* to go back to the
defaults.

Ring size, center size, colors, the open animation, labels, whether the last used menu is remembered, *Release to
Select* and *Inline Submenus* are also in the mod's config screen (Mods → Radial Menu → Config) or
`config/radialmenu.cfg`. Colors there are written like
`#3C78D8` (the `#` is optional, and `#38D` works too), with a separate opacity from 0 to 100%.

The editor screens use a dark ModularUI theme, `assets/radialmenu/themes/dark.json` in the jar. A resource pack can
replace that file to restyle them.

## Building

Run `./gradlew build` with JDK 17 or newer. Gradle runs the build itself on JDK 25, using an installed copy or
downloading one; the mod still targets Java 8. The build runs the unit tests too. The jar ends up in `build/libs`, and
its version comes from the git tag.

The symbol sheet in `src/main/resources/assets/radialmenu` is made by `tools/SymbolSheet.java` from Phosphor's icon
font; the file explains how to run it.

GitHub Actions builds every push. Pushing a version tag, such as `1.4.0`, builds that version and publishes it on the
Releases page with the jars attached.

## License

LGPL-3.0. See [LICENSE](LICENSE).

The symbols are [Phosphor Icons](https://phosphoricons.com), MIT licensed. Their license is in
`src/main/resources/assets/radialmenu/symbols/LICENSE-phosphor.txt` and in the jar.
