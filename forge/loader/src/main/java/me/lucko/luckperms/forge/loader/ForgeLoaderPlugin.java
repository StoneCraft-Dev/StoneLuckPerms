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

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import java.util.function.Supplier;
import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import me.lucko.luckperms.forge.LegacyLoaderBootstrap;

@Mod(modid = "luckperms", acceptableRemoteVersions = "*")
public class ForgeLoaderPlugin implements Supplier<ModContainer> {

    private static final String JAR_NAME = "luckperms-forge.jarinjar";
    private static final String BOOTSTRAP_CLASS = "me.lucko.luckperms.forge.LPForgeBootstrap";

    private final ModContainer container;

    private final JarInJarClassLoader loader;
    private LegacyLoaderBootstrap plugin;

    public ForgeLoaderPlugin() {
        this.container = Loader.instance().getModList().stream()
                .filter(modContainer -> modContainer.getMod() == this).findFirst().orElse(null);

        this.loader = new JarInJarClassLoader(this.getClass().getClassLoader(), JAR_NAME);
        FMLCommonHandler.instance().bus().register(this);
    }

    @Override
    public ModContainer get() {
        return this.container;
    }

    // TODO: Check if right event
    @Mod.EventHandler
    public void onCommonSetup(final FMLInitializationEvent ignored) {
        this.plugin =
                (LegacyLoaderBootstrap) this.loader.instantiatePlugin(BOOTSTRAP_CLASS, Supplier.class, this);
        this.plugin.onLoad();
    }

    @Mod.EventHandler
    public void onServerAboutToStart(final FMLServerAboutToStartEvent event) {
        this.plugin.onServerAboutToStart(event);
    }

    @Mod.EventHandler
    public void onServerStarted(final FMLServerStartedEvent event) {
        this.plugin.onServerStarted(event);
    }

    @Mod.EventHandler
    public void onServerStopping(final FMLServerStoppingEvent event) {
        this.plugin.onServerStopping(event);
    }
}
