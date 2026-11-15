package com.servidor.playerwarps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private final String MENU_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Warps dos Jogadores" + ChatColor.DARK_GRAY + " ✦";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("pw").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MeuPlayerWarps ativado com sucesso para a versão 1.26.3!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            abrirMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /pw set <nome>
        if (sub.equals("set") || sub.equals("criar")) {
            if (!player.hasPermission("meuplayerwarps.criar") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para criar warps.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Use: /pw set <nome>");
                return true;
            }

            String nome = args[1].toLowerCase().replaceAll("[^a-z0-9_]", "");
            if (nome.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Nome inválido! Use apenas letras e números.");
                return true;
            }

            if (getConfig().contains("warps." + nome)) {
                String dono = getConfig().getString("warps." + nome + ".dono-uuid");
                if (!player.getUniqueId().toString().equals(dono) && !player.hasPermission("meuplayerwarps.admin")) {
                    player.sendMessage(ChatColor.RED + "Já existe uma warp com esse nome!");
                    return true;
                }
            }

            Location loc = player.getLocation();
            getConfig().set("warps." + nome + ".dono-nome", player.getName());
            getConfig().set("warps." + nome + ".dono-uuid", player.getUniqueId().toString());
            getConfig().set("warps." + nome + ".mundo", loc.getWorld().getName());
            getConfig().set("warps." + nome + ".x", loc.getX());
            getConfig().set("warps." + nome + ".y", loc.getY());
            getConfig().set("warps." + nome + ".z", loc.getZ());
            getConfig().set("warps." + nome + ".yaw", loc.getYaw());
            getConfig().set("warps." + nome + ".pitch", loc.getPitch());
            getConfig().set("warps." + nome + ".visitas", 0);
            saveConfig();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "✔ Warp " + ChatColor.BOLD + nome + ChatColor.GREEN + " criada com sucesso!");
            return true;
        }

        // /pw del <nome>
        if (sub.equals("del") || sub.equals("deletar") || sub.equals("remover")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Use: /pw del <nome>");
                return true;
            }

            String nome = args[1].toLowerCase();
            if (!getConfig().contains("warps." + nome)) {
                player.sendMessage(ChatColor.RED + "Essa warp não existe.");
                return true;
            }

            String dono = getConfig().getString("warps." + nome + ".dono-uuid");
            if (!player.getUniqueId().toString().equals(dono) && !player.hasPermission("meuplayerwarps.admin")) {
                player.sendMessage(ChatColor.RED + "Você não é o dono desta warp!");
                return true;
            }

            getConfig().set("warps." + nome, null);
            saveConfig();
            player.sendMessage(ChatColor.RED + "✖ Warp " + ChatColor.BOLD + nome + ChatColor.RED + " removida.");
            return true;
        }

        // /pw ir <nome> ou /pw <nome>
        String alvo = (sub.equals("ir") && args.length >= 2) ? args[1].toLowerCase() : sub;
        teleportarWarp(player, alvo);
        return true;
    }

    private void teleportarWarp(Player player, String nome) {
        if (!getConfig().contains("warps." + nome)) {
            player.sendMessage(ChatColor.RED + "Warp '" + nome + "' não encontrada. Use /pw para ver a lista.");
            return;
        }

        String mundoNome = getConfig().getString("warps." + nome + ".mundo");
        if (Bukkit.getWorld(mundoNome) == null) {
            player.sendMessage(ChatColor.RED + "O mundo desta warp não está carregado.");
            return;
        }

        double x = getConfig().getDouble("warps." + nome + ".x");
        double y = getConfig().getDouble("warps." + nome + ".y");
        double z = getConfig().getDouble("warps." + nome + ".z");
        float yaw = (float) getConfig().getDouble("warps." + nome + ".yaw");
        float pitch = (float) getConfig().getDouble("warps." + nome + ".pitch");

        Location destino = new Location(Bukkit.getWorld(mundoNome), x, y, z, yaw, pitch);
        player.teleport(destino);
        player.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        int visitas = getConfig().getInt("warps." + nome + ".visitas", 0) + 1;
        getConfig().set("warps." + nome + ".visitas", visitas);
        saveConfig();

        String dono = getConfig().getString("warps." + nome + ".dono-nome");
        player.sendMessage(ChatColor.GREEN + "✦ Teleportado para a warp " + ChatColor.GOLD + nome + ChatColor.GREEN + " de " + ChatColor.WHITE + dono + ChatColor.GREEN + "!");
    }

    private void abrirMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MENU_TITLE);

        ItemStack vidroRoxo = criarItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack vidroPreto = criarItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, (i % 2 == 0) ? vidroRoxo : vidroPreto);
            }
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Como criar a sua Warp?");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Vá até o local desejado e digite:");
        infoLore.add(ChatColor.YELLOW + "/pw set <nome>");
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Para apagar a sua warp:");
        infoLore.add(ChatColor.RED + "/pw del <nome>");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        ConfigurationSection sec = getConfig().getConfigurationSection("warps");
        if (sec != null) {
            int slot = 10;
            for (String nome : sec.getKeys(false)) {
                while (slot < 44 && (slot % 9 == 0 || slot % 9 == 8)) {
                    slot++;
                }
                if (slot >= 44) break;

                String donoNome = sec.getString(nome + ".dono-nome", "Desconhecido");
                String donoUuid = sec.getString(nome + ".dono-uuid");
                String mundo = sec.getString(nome + ".mundo", "world");
                int visitas = sec.getInt(nome + ".visitas", 0);

                ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) cabeca.getItemMeta();
                if (donoUuid != null) {
                    try {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(donoUuid)));
                    } catch (Exception ignored) {}
                }

                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + nome.toUpperCase());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "------------------------");
                lore.add(ChatColor.GRAY + "Dono: " + ChatColor.WHITE + donoNome);
                lore.add(ChatColor.GRAY + "Mundo: " + ChatColor.AQUA + mundo);
                lore.add(ChatColor.GRAY + "Visitas: " + ChatColor.GREEN + visitas);
                lore.add(ChatColor.DARK_GRAY + "------------------------");
                lore.add(ChatColor.YELLOW + "▶ Clique para teleportar!");
                meta.setLore(lore);
                cabeca.setItemMeta(meta);

                inv.setItem(slot, cabeca);
                slot++;
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
    }

    private ItemStack criarItem(Material mat, String nome) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nome);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void aoClicarNoMenu(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicado = event.getCurrentItem();
        if (clicado == null || clicado.getType() != Material.PLAYER_HEAD || !clicado.hasItemMeta()) return;

        String nomeWarp = ChatColor.stripColor(clicado.getItemMeta().getDisplayName()).toLowerCase();
        player.closeInventory();
        teleportarWarp(player, nomeWarp);
    }
}
