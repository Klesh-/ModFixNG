/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package modfixng.fixes;

import modfixng.main.Config;
import modfixng.main.ModFixNG;
import modfixng.utils.ModFixNGUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class FixBag implements Listener {
	private ModFixNG main;
	private Config config;

	public FixBag(ModFixNG main, Config config) {
		this.main = main;
		this.config = config;
		initBag19ButtonInventoryClickListener();
		initDropButtonInventoryClickListener();
		initDropButtonPlayClickListener();
	}

	// close inventory on death and also fix dropped cropanalyzer
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!config.fixBagEnabled) {
			return;
		}

		Player p = (Player) event.getEntity();
		if (config.fixBagCropanalyzerFixEnabled) {
			if (ModFixNGUtils.isCropanalyzerOpen(p)) {
				try {
					ModFixNGUtils.findAndFixOpenCropanalyzer(p, event.getDrops());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		p.closeInventory();
	}

	// restrict shift-click on block with some bags
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerShitClickBlock(PlayerInteractEvent event) {
		if (!config.fixBagEnabled) {
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		if (player.isSneaking() && config.fixBagShiftBlockRestrictBagIDs.contains(player.getItemInHand().getTypeId())) {
			event.setCancelled(true);
		}
	}

	// close inventory before opening another one
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerOpenedBlock(PlayerInteractEvent event) {
		if (!config.fixBagEnabled) {
			return;
		}
		if (!config.fixBagCloseInventryOnInteractIfAlreadyOpened) {
			return;
		}

		if (ModFixNGUtils.isInventoryOpen(event.getPlayer())) {
			if ((event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.isCancelled()) || (event.getAction() == Action.RIGHT_CLICK_AIR)) {
				event.getPlayer().closeInventory();
			}
		}
	}

	// close inventory on portal enter
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onMove(PlayerMoveEvent event) {
		if (!config.fixBagEnabled) {
			return;
		}

		if (event.getFrom().getBlock().equals(event.getTo().getBlock())) {
			return;
		}

		if (event.getTo().getBlock().getType() == Material.PORTAL) {
			event.getPlayer().closeInventory();
		}

	}

	// close inventory on quit
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerExit(PlayerQuitEvent event) {
		if (!config.fixBagEnabled) {
			return;
		}

		event.getPlayer().closeInventory();
	}

	// restrict using 1-9 buttons in bags inventories if it will move bag to another slot
	private void initBag19ButtonInventoryClickListener() {
		main.protocolManager.addPacketListener(new PacketAdapter(
				PacketAdapter
				.params(main, PacketType.Play.Client.WINDOW_CLICK)
				.clientSide()
				.listenerPriority(ListenerPriority.HIGHEST)
				.optionIntercept()
			) {
			@SuppressWarnings("deprecation")
			@Override
			public void onPacketReceiving(PacketEvent e) {
				if (!config.fixBagEnabled) {
					return;
				}
				
				if (!config.fixBag19ButtonClickEnabled) {
					return;
				}

				if (e.getPlayer() == null) {
					return;
				}

				final Player player = e.getPlayer();
				// if item in hand is one of the bad ids - check buttons
				if (config.fixBag19ButtonClickBagIDs.contains(player.getItemInHand().getTypeId())) {
					// check click type(checking for shift+button)
					if (e.getPacket().getIntegers().getValues().get(3) == 2) {
						// check to which slot we want to move item(checking if it is the holding bag slot)
						final int heldslot = player.getInventory().getHeldItemSlot();
						if (heldslot == e.getPacket().getIntegers().getValues().get(2)) {
							e.setCancelled(true);
							player.updateInventory();
						}
					}
				}
			}
		});
	}

	// restrict q and ctrl+q button in cropanalyzer and toobox inventory if trying to drop opened toolbox or cropnalyzer
	private void initDropButtonInventoryClickListener() {
		main.protocolManager.addPacketListener(new PacketAdapter(
				PacketAdapter
				.params(main, PacketType.Play.Client.WINDOW_CLICK)
				.clientSide()
				.listenerPriority(ListenerPriority.HIGHEST)
				.optionIntercept()
			) {
			@SuppressWarnings("deprecation")
			@Override
			public void onPacketReceiving(PacketEvent e) {
				if (!config.fixBagEnabled) {
					return;
				}

				if (e.getPlayer() == null) {
					return;
				}

				Player player = e.getPlayer();
				int clickedslot = e.getPacket().getIntegers().getValues().get(1);
				
				// check click type(checking for q or ctrl+q)
				if (e.getPacket().getIntegers().getValues().get(3) == 4 && clickedslot != -999) {
					// first check for cropnalyzer
					if (config.fixBagCropanalyzerFixEnabled) {
						if (ModFixNGUtils.isCropanalyzerOpen(player)) {
							try {
								if (ModFixNGUtils.isTryingToDropOpenCropanalyzer(player, clickedslot)) {
									e.setCancelled(true);
									player.updateInventory();
									return;
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
					// then check for toolbox
					if (config.fixBagToolboxFixEnabled) {
						if (ModFixNGUtils.isToolboxOpen(player)) {
							try {
								if (ModFixNGUtils.isTryingToDropOpenToolBox(player, clickedslot)) {
									e.setCancelled(true);
									player.updateInventory();
									return;
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
		});
	}
	
	//restrict dropping toolbox using q (while inventory is not yet open clientside) but open serverside.
	private void initDropButtonPlayClickListener() {
		main.protocolManager.addPacketListener(new PacketAdapter(
				PacketAdapter
				.params(main, PacketType.Play.Client.BLOCK_DIG)
				.clientSide()
				.listenerPriority(ListenerPriority.HIGHEST)
				.optionIntercept()
			) {
			@SuppressWarnings("deprecation")
			@Override
			public void onPacketReceiving(PacketEvent e) {
				if (!config.fixBagEnabled) {
					return;
				}
				
				if (!config.fixBagToolboxFixEnabled) {
					return;
				}

				if (e.getPlayer() == null) {
					return;
				}

				Player player = e.getPlayer();
				
				// check click type(checking for q)
				int actiontype = e.getPacket().getIntegers().getValues().get(4);
				if (actiontype == 3 || actiontype == 4) {
					// then check for toolbox
					if (ModFixNGUtils.isToolboxOpen(player)) {
						try {
							if (ModFixNGUtils.isTryingToDropOpenToolBox(player, player.getItemInHand())) {
								e.setCancelled(true);
								player.updateInventory();
								return;
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		});
	}
	
}
