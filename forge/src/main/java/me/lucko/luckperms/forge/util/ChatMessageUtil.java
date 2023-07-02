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

package me.lucko.luckperms.forge.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.jetbrains.annotations.NotNull;

public class ChatMessageUtil {

    public static @NotNull List<IChatComponent> fixLineJumps(
            @NotNull final IChatComponent chatComponent) {
        final NodeTreeInfo nodeTreeInfo = new NodeTreeInfo(-1);
        final List<IChatComponent> chatComponents =
                fixLineJumps(chatComponent, chatComponent, new ArrayList<>(), nodeTreeInfo,
                        nodeTreeInfo);

        if (nodeTreeInfo.isNotDone()) {
            chatComponents.add(getComponent(chatComponent, nodeTreeInfo));
        }

        return chatComponents;
    }

    private static @NotNull List<IChatComponent> fixLineJumps(
            final @NotNull IChatComponent rootChatComponent,
            final @NotNull IChatComponent currentChatComponent,
            @NotNull final List<IChatComponent> chatComponents,
            @NotNull final NodeTreeInfo rootNode, @NotNull final NodeTreeInfo currentNode) {
        if (currentChatComponent instanceof ChatComponentText
                && currentChatComponent.getUnformattedTextForChat().contains("\n")) {
            currentNode.added();

            if (rootNode.isNotDone()) {
                chatComponents.add(getComponent(rootChatComponent, rootNode));
            }

            final String[] split = currentChatComponent.getUnformattedTextForChat().split("\n");

            for (final String s : split) {
                if (s.isEmpty()) {
                    continue;
                }

                chatComponents.add(
                        new ChatComponentText(s).setChatStyle(currentChatComponent.getChatStyle()));
            }
        }

        for (int i = 0; i < currentChatComponent.getSiblings().size(); i++) {
            final IChatComponent c = (IChatComponent) currentChatComponent.getSiblings().get(i);
            final NodeTreeInfo n = new NodeTreeInfo(i);

            currentNode.addSibling(n);

            fixLineJumps(rootChatComponent, c, chatComponents, rootNode, n);
        }

        return chatComponents;
    }

    private static IChatComponent getComponent(@NotNull final IChatComponent chatComponent,
            @NotNull final NodeTreeInfo nodeTreeInfo) {
        final IChatComponent component;

        if (nodeTreeInfo.getStatus() == NodeTreeInfo.NodeStatus.ADDED) {
            component = new ChatComponentText("");
            component.setChatStyle(chatComponent.getChatStyle());
        } else if (chatComponent instanceof ChatComponentText) {
            component = new ChatComponentText(
                    ((ChatComponentText) chatComponent).getChatComponentText_TextValue());
            component.setChatStyle(chatComponent.getChatStyle());
        } else {
            component = chatComponent.createCopy();
            component.getSiblings().clear();
        }

        for (final NodeTreeInfo n : nodeTreeInfo.getSiblings()) {
            component.appendSibling(
                    getComponent((IChatComponent) chatComponent.getSiblings().get(n.getIndex()),
                            n));
        }

        nodeTreeInfo.added();

        return component;
    }

    private static class NodeTreeInfo {

        private final int index;
        private final List<NodeTreeInfo> siblings;
        private NodeStatus status;

        public NodeTreeInfo(final int index) {
            this(index, NodeStatus.TO_BE_ADDED, new ArrayList<>());
        }

        public NodeTreeInfo(final int index, final NodeStatus status,
                final List<NodeTreeInfo> siblings) {
            this.index = index;
            this.status = status;
            this.siblings = siblings;
        }

        public int getIndex() {
            return this.index;
        }

        public List<NodeTreeInfo> getSiblings() {
            return this.siblings;
        }

        public void addSibling(final NodeTreeInfo sibling) {
            this.siblings.add(sibling);
        }

        public NodeStatus getStatus() {
            return this.status;
        }

        public boolean isNotDone() {
            return this.status != NodeStatus.ADDED || this.siblings.stream()
                    .anyMatch(NodeTreeInfo::isNotDone);
        }

        public void added() {
            this.status = NodeStatus.ADDED;
        }

        enum NodeStatus {
            ADDED, TO_BE_ADDED
        }
    }
}
