/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.forge.loader;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import java.util.function.Supplier;
import me.lucko.luckperms.forge.LPForgeBootstrap;
import me.lucko.luckperms.forge.LegacyLoaderBootstrap;

@Mod(modid = "luckperms", acceptableRemoteVersions = "*")
public class ForgeLoaderPlugin extends DummyModContainer implements Supplier<ModContainer> {

    private final ModContainer container;
    private LegacyLoaderBootstrap plugin;

    public ForgeLoaderPlugin() {
        super(getModMetadata());

        this.container = Loader.instance().getModList().stream()
                .filter(modContainer -> modContainer.getMod() == this).findFirst().orElse(null);
    }

    private static ModMetadata getModMetadata() {
        final ModMetadata meta = new ModMetadata();

        meta.modId = "luckperms";

        return meta;
    }

    @Override
    public boolean registerBus(final EventBus bus, final LoadController controller) {
        bus.register(this);

        return true;
    }

    @Override
    public ModContainer get() {
        return this.container;
    }

    @Subscribe
    public void onCommonSetup(final FMLInitializationEvent ignored) {
        this.plugin = new LPForgeBootstrap(this);
        this.plugin.onLoad();
    }

    @Subscribe
    public void onServerAboutToStart(final FMLServerAboutToStartEvent event) {
        this.plugin.onServerAboutToStart(event);
    }

    @Subscribe
    public void onServerStarted(final FMLServerStartedEvent event) {
        this.plugin.onServerStarted(event);
    }

    @Subscribe
    public void onServerStopping(final FMLServerStoppingEvent event) {
        this.plugin.onServerStopping(event);
    }
}
