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

package modfixng.nms.packets.v1_7_R4;

import modfixng.main.ModFixNG;
import modfixng.nms.packets.PacketHookInterface;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

public class PacketHook implements PacketHookInterface {

	@Override
	public void initInBlockDigListener() {
		ModFixNG.getProtocolManager().addPacketListener(
			new PacketAdapter(
				PacketAdapter
				.params(ModFixNG.getInstance(), PacketType.Play.Client.BLOCK_DIG)
				.listenerPriority(ListenerPriority.HIGHEST)
			) {
				@Override
				public void onPacketReceiving(PacketEvent e) {
					Player player = e.getPlayer();
					if (player == null) {
						return;
					}

					PacketContainer newpacket = PacketFactory.getBlockDigPacket(player, e.getPacket());
					e.setPacket(newpacket);
				}
			}
		);
	}

}