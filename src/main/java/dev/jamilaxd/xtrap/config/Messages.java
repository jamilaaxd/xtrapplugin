package dev.jamilaxd.xtrap.config;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Messages {

    private final XTrapPlugin plugin;
    private YamlConfiguration yaml;

    public Messages(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String path) {
        return yaml.getString(path, path);
    }

    public String format(String path, Map<String, String> vars) {
        String text = raw(path);
        if (vars != null) {
            for (var entry : vars.entrySet()) text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> vars) {
        Text.send(sender, yaml.getString("prefix", ""), format(path, vars));
    }

    public void sendList(CommandSender sender, String path) {
        List<String> lines = yaml.getStringList(path);
        for (String line : lines) sender.sendMessage(Text.comp(line));
    }

    public void broadcast(String path, Map<String, String> vars) {
        String text = yaml.getString("prefix", "") + format(path, vars);
        Bukkit.broadcast(Text.comp(text));
    }
}
