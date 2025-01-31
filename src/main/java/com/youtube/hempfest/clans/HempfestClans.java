package com.youtube.hempfest.clans;

import com.github.sanctum.labyrinth.command.CommandRegistration;
import com.github.sanctum.labyrinth.data.Registry;
import com.github.sanctum.labyrinth.task.Schedule;
import com.google.gson.JsonObject;
import com.youtube.hempfest.clans.bank.api.BankAPI;
import com.youtube.hempfest.clans.bank.api.ClanBank;
import com.youtube.hempfest.clans.bank.model.BankMeta;
import com.youtube.hempfest.clans.metadata.PersistentClan;
import com.youtube.hempfest.clans.util.Metrics;
import com.youtube.hempfest.clans.util.Placeholders;
import com.youtube.hempfest.clans.util.construct.Claim;
import com.youtube.hempfest.clans.util.construct.ClaimManager;
import com.youtube.hempfest.clans.util.construct.Clan;
import com.youtube.hempfest.clans.util.construct.Resident;
import com.youtube.hempfest.clans.util.data.Config;
import com.youtube.hempfest.clans.util.data.ConfigType;
import com.youtube.hempfest.clans.util.data.DataManager;
import com.youtube.hempfest.clans.util.events.RaidShieldEvent;
import com.youtube.hempfest.clans.util.misc.URLParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;


public class HempfestClans extends JavaPlugin {

	private static HempfestClans instance;

	private BankAPI bankApi = null;

	private final Logger log = Logger.getLogger("Minecraft");

	public DataManager dataManager = new DataManager();

	public final ClaimManager claimManager = new ClaimManager();

	public HashMap<UUID, String> playerClan = new HashMap<>();

	public static HashMap<UUID, Clan> clanManager = new HashMap<>();

	public static HashMap<Player, String> idMode = new HashMap<>();

	public static HashMap<Player, String> chatMode = new HashMap<>();

	public static HashMap<String, List<String>> clanEnemies = new HashMap<>();

	public static HashMap<String, List<String>> clanAllies = new HashMap<>();

	public static List<Resident> residents = new ArrayList<>();

	public static List<Player> wildernessInhabitants = new ArrayList<>();

	public void onEnable() {
		getLogger().info("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		getLogger().info("- Clans [Free]. Loading plugin information...");
		getLogger().info("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		for (String ch : logo()) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			getLogger().info("- " + ch);
		}
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		setInstance(this);
		dataManager.copyDefaults();
		new Registry<>(Listener.class).source(this).pick("com.youtube.hempfest.clans.util.listener").operate(l -> getServer().getPluginManager().registerEvents(l, this));
		new Registry<>(Command.class).source(this).pick("com.youtube.hempfest.clans.commands").operate(CommandRegistration::use);
		Clan.clanUtil.setRaidShield(true);
		refreshChat();
		runShieldTimer();
		log.info(String.format("[%s] - Beginning claim resident event", getDescription().getName()));
		claimManager.refresh();
		Clan.clanUtil.loadClans();
		for (Player p : Bukkit.getOnlinePlayers()) {
			DataManager data = new DataManager(p.getUniqueId().toString(), null);
			Config user = data.getFile(ConfigType.USER_FILE);
			if (user.getConfig().getString("Clan") != null) {
				playerClan.put(p.getUniqueId(), user.getConfig().getString("Clan"));
				getLogger().info("- Refilled user data. *RELOAD NOT SAFE*");
			}
		}
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new Placeholders(this).register();
			getLogger().info("- PlaceholderAPI found! Loading clans placeholders %clans_(name, rank, raidshield)%.");
		} else {
			getLogger().info("- PlaceholderAPI not found, placeholders will not work!");
		}
		registerMetrics(9234);
		getLogger().info("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		boolean success;
		try {
			getLogger().info("- Attempting automatic clan meta query process..");
			PersistentClan.querySaved();
			success = true;
		} catch (NullPointerException e) {
			getLogger().info("- Process failed. No directory found to process.");
			getLogger().info("- Store a new instance of data for query to take effect on enable.");
			success = false;
		}
		if (success) {
			getLogger().info("- Query success! All found meta cached. (" + PersistentClan.getMetaDataContainer().length + ")");
		} else {
			getLogger().info("- Query failed! (SEE ABOVE FOR INFO)");
		}
		Update updater = new Update(this);
		try {
			if (updater.hasUpdate())
				getLogger().info("- An update was found! New version: " + updater.getLatestVersion() + " download: " + updater.getResourceURL());
		} catch (Exception e) {
			getLogger().warning("- There was a problem while checking for updates.");
		}
	}

	private List<String> logo() {
		return new ArrayList<>(Arrays.asList("   ▄▄· ▄▄▌   ▄▄▄·  ▐ ▄ .▄▄ · ", "  ▐█ ▌▪██•  ▐█ ▀█ •█▌▐█▐█ ▀. ", "  ██ ▄▄██▪  ▄█▀▀█ ▐█▐▐▌▄▀▀▀█▄", "  ▐███▌▐█▌▐▌▐█ ▪▐▌██▐█▌▐█▄▪▐█", "  ·▀▀▀ .▀▀▀  ▀  ▀ ▀▀ █▪ ▀▀▀▀ "));
	}

	public void onDisable() {
		residents.clear();
		idMode.clear();
		playerClan.clear();
		chatMode.clear();
		clanAllies.clear();
		clanEnemies.clear();
		clanManager.clear();
		wildernessInhabitants.clear();
		Clan.clanUtil.getClans.clear();
	}

	private void refreshChat() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			chatMode.put(p, "GLOBAL");
		}
	}

	public static Clan clanManager(Player p) {
		return Clan.clanUtil.getClans.stream().filter(c -> Arrays.asList(c.getMembers()).contains(p.getName())).findFirst().orElse(null);
	}

	public static Config getMain() {
		DataManager dm = new DataManager("Config", "Configuration");
		Config main = dm.getFile(ConfigType.MISC_FILE);
		if (!main.exists()) {
			InputStream is = getInstance().getResource("Config.yml");
			Config.copy(is, main.getFile());
		}
		return main;
	}

	private void runShieldTimer() {
		boolean configAllow = getMain().getConfig().getBoolean("Clans.raid-shield.allow");
		if (configAllow) {
			Schedule.sync(() -> {
				if (Bukkit.getOnlinePlayers().size() > 0) {
					RaidShieldEvent e = new RaidShieldEvent();
					Bukkit.getPluginManager().callEvent(e);
					if (!e.isCancelled()) {
						e.handleUpdate();
					}
				}
			}).debug().repeat(10, 10);
			log.info(String.format("[%s] - Running allowance for RaidShield event", getDescription().getName()));
		} else {
			log.info(String.format("[%s] - RaidShield disabled. Denying runnable.", getDescription().getName()));
		}
	}

	public boolean isBankingEnabled() {
		return false; // TODO: config flag
	}

	public static @Nullable BankAPI getBankAPI() {
		if (instance.isBankingEnabled() && instance.bankApi == null) {
			return instance.bankApi = new BankAPI() {
				@Override
				public ClanBank getBank(Clan clan) {
					return BankMeta.get(clan).getBank().orElseThrow(NullPointerException::new);
				}

				@Override
				public boolean isBankingEnabled() {
					return true;
				}
			};
		}
		return null;
	}

	public static HempfestClans getInstance() {
		return instance;
	}

	private void setInstance(HempfestClans instance) {
		HempfestClans.instance = instance;
	}

	private void registerMetrics(int ID) {
		Metrics metrics = new Metrics(this, ID);
		metrics.addCustomChart(new Metrics.SimplePie("using_claiming", () -> {
			String result = "No";
			if (Claim.claimUtil.claimingAllowed()) {
				result = "Yes";
			}
			return result;
		}));
		boolean configAllow = getMain().getConfig().getBoolean("Clans.raid-shield.allow");
		metrics.addCustomChart(new Metrics.SimplePie("using_raidshield", () -> {
			String result = "No";
			if (configAllow) {
				result = "Yes";
			}
			return result;
		}));
		metrics.addCustomChart(new Metrics.SimplePie("used_prefix", () -> ChatColor.stripColor(getMain().getConfig().getString("Formatting.prefix"))));
		metrics.addCustomChart(new Metrics.SingleLineChart("total_logged_players", () -> Clan.clanUtil.getAllUsers().size()));
		metrics.addCustomChart(new Metrics.SingleLineChart("total_clans_made", () -> Clan.clanUtil.getAllClanIDs().size()));
		getLogger().info("- Converting bStats metrics tables.");
	}
}
