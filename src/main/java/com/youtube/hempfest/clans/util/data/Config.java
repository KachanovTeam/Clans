package com.youtube.hempfest.clans.util.data;

import com.youtube.hempfest.clans.HempfestClans;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {
    private final String n;
    private final String d;
    private FileConfiguration fc;
    private File file;
    private final HempfestClans plugin;
    private static List<Config> configs;

    static {
        Config.configs = new ArrayList<Config>();
    }

    public Config(final String n, final String d) {
        this.plugin = HempfestClans.getInstance();
        this.n = n;
        this.d = d;
        Config.configs.add(this);
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return (n == null) ? "" : n;
    }


    public static Config get(final String n, final String d) {
        return Config.configs.stream().filter(c -> c.getName().equals(n)).findFirst().orElse(new Config(n, d));
    }

    public boolean delete() {
        return this.getFile().delete();
    }

    public boolean exists() {
        if(this.fc == null || this.file == null) {
            final File temp = new File(this.getDataFolder(), this.getName() + ".yml");
            if(!temp.exists()) {
                return false;
            }
            this.file = temp;
        }
        return true;
    }

    public File getFile() {
        if(this.file == null) {
            this.file = new File(this.getDataFolder(), this.getName() + ".yml"); //create method get data folder
            if(!this.file.exists()) {
                try {
                    this.file.createNewFile();
                }catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.file;
    }

    public FileConfiguration getConfig() {
        if(this.fc == null) {
            this.fc = YamlConfiguration.loadConfiguration(this.getFile());
        }
        return this.fc;
    }

    public File getDataFolder() {
        final File dir = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " "));
        File d;
        if (this.d != null) {
            d = new File(dir.getParentFile().getPath(), HempfestClans.getInstance().getName() + "/" + this.d + "/");
        } else {
            d = new File(dir.getParentFile().getPath(), HempfestClans.getInstance().getName());
        }
        if(!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    public void reload() {
        this.fc = YamlConfiguration.loadConfiguration(getFile());
        final File defConfigStream = new File(plugin.getDataFolder(), this.getName() + ".yml");
        final YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        this.fc.setDefaults(defConfig);
    }

    public void saveConfig() {
        try {
            this.getConfig().save(this.getFile());
        }catch(final IOException e) {
            e.printStackTrace();
        }
    }






}

