/*
 * Miracle Bringer - once in a while, a lethal blow is answered with a blessing.
 * Copyright (C) 2026 Rusty Shackleford and nfx
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nfx.miraclebringer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The player's miracle memory, as a persistent attachment. */
public final class ModAttachments {
    private ModAttachments() {}

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MiracleBringer.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MiracleMemory>> MEMORY =
            ATTACHMENTS.register("memory", () -> AttachmentType.builder(() -> MiracleMemory.NONE)
                    .serialize(MiracleMemory.CODEC)
                    .copyOnDeath()
                    .build());

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
