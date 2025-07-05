# HardcoreWorldReset

A Spigot plugin for Minecraft that resets the world on every player death in hardcore mode and starts a new, unique timer. Ideal for "hardcore challenge" servers with friends.

---
## ⚠️ Disclaimer

> **This is pre-release software!**
>
> While it has been tested, it may still contain bugs or undergo significant changes. It is **strongly recommended** to make a full backup of your server folder before installing.
>
> Please report any issues you find on the [GitHub Issues](https://github.com/<your-github-name>/hardcore-world-reset/issues) page.

---

## Features

- **Automatic World Reset:** Upon player death, the current hardcore world is deleted and replaced with a new, freshly generated one.
- **Seamless Transition:** All players are instantly teleported to the new world.
- **Configurable Reset Method:** Choose between a seamless teleport (`SEAMLESS`) or disconnecting all players (`DISCONNECT`) until the new world is ready.
- **Integrated Timer:** A precise timer (`HH:MM:SS.ms`) is displayed in the player list.
- **Defined Goal:** The timer reliably stops as soon as the Ender Dragon is defeated.
- **Advancement Reset:** All advancements are reset for all players with every world reset.
- **Full Dimension Support:** Nether and End dimensions are correctly created and linked for each hardcore world set, thanks to the Multiverse integration.

---

## Installation & Setup

1.  **Download Dependencies:** You need the following plugins on your server:
    * [Multiverse-Core](https://dev.bukkit.org/projects/multiverse-core) (v4.3.x)
    * [Multiverse-NetherPortals](https://dev.bukkit.org/projects/multiverse-netherportals) (v4.2.x)
2.  **Download HardcoreWorldReset:** Download the latest `.jar` file from the **[Releases Page](https://github.com/kdltmhl/hardcore-world-reset/releases)**.
3.  **Place Files:** Put all three `.jar` files into the `plugins` folder of your Spigot server.
4.  Important: Ensure that `hardcore=true` is set in your `server.properties` file.
5.  Start your server.

---

## Configuration

The configuration can be found in `plugins/HardcoreWorldReset/config.yml`.

```yaml
# enables or disables the entire plugin.
plugin-enabled: true

# the prefix for all generated hardcore worlds.
# WARNING: Changing this value will create a completely new series of worlds!
world-prefix: "hardcore_"

# the method for swapping to the new world after a death.
# SEAMLESS: Instantly teleports all players to the new world.
# WARNING: Causes a significant lag spike at every reset from creating the new standby world
# DISCONNECT: Kicks all players from the server and allows them to reconnect once the new world is ready.
swap-method: "SEAMLESS"

# end goal of the run (currently only supports the ender dragon)
end-goal: "ENDER_DRAGON"

# splash texts after certain events
messages:
  kick-reason: "&6A player has died! The world is resetting."
  title-main: "&cA player died!"
  title-subtitle: "Welcome to the new world."

# the %time% placeholder will be replaced with the final time.
  dragon-defeat: "&aThe Ender Dragon has been defeated! &fFinal Time: &e%time%"

  redirect: "&aMoving you to the active hardcore world."

# --- DO NOT EDIT BELOW THIS LINE ---
# This section saves the state of the plugin.
state:
  active-world: "hardcore_1"
  standby-world: "hardcore_2"
  world-counter: 2