/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.commands;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

public class SpongeRootCommand implements Command.Raw, RootCommand {

    private final SpongeCommandManager manager;
    private final String name;
    private BaseCommand defCommand;
    private SetMultimap<String, RegisteredCommand> subCommands = HashMultimap.create();
    private List<BaseCommand> children = new ArrayList<>();
    boolean isRegistered = false;

    SpongeRootCommand(SpongeCommandManager manager, String name) {
        this.manager = manager;
        this.name = name;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    private CommandResult executeSponge(CommandIssuer sender, String commandLabel, String[] args) {
        BaseCommand cmd = execute(sender, commandLabel, args);
        SpongeCommandOperationContext lastContext = (SpongeCommandOperationContext) cmd.getLastCommandOperationContext();
        return lastContext != null ? lastContext.getResult() : CommandResult.success();
    }

    public void addChild(BaseCommand command) {
        if (this.defCommand == null || !command.subCommands.get(BaseCommand.DEFAULT).isEmpty()) {
            this.defCommand = command;
        }
        addChildShared(this.children, this.subCommands, command);
    }

    @Override
    public BaseCommand getDefCommand() {
        return defCommand;
    }

    @Override
    public CommandManager getManager() {
        return manager;
    }

    @Override
    public SetMultimap<String, RegisteredCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public List<BaseCommand> getChildren() {
        return children;
    }

    @Override
    public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
        String[] args = argToStrlist(arguments);
        return this.executeSponge(manager.getCommandIssuer(cause), this.name, args);
    }

    @Override
    public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
        String[] args = argToStrlist(arguments);
        return getTabCompletions(manager.getCommandIssuer(cause), this.name, args).stream().map(it -> new CommandCompletion() {
            @Override
            public String completion() {
                return it;
            }

            @Override
            public Optional<Component> tooltip() {
                return Optional.empty();
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getTabCompletions(CommandIssuer sender, String alias, String[] args, boolean commandsOnly, boolean isAsync) {
        Set<String> completions = new HashSet<>();
        getChildren().forEach(child -> {
            if (!commandsOnly) {
                completions.addAll(child.tabComplete(sender, this, args, isAsync));
            }
            completions.addAll(child.getCommandsForCompletion(sender, args));
        });

        return completions.stream()
                .filter(it -> Arrays
                        .stream(args)
                        .noneMatch(it::equals))
                .collect(Collectors.toList());
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        return this.hasAnyPermission(manager.getCommandIssuer(cause));
    }

    @Override
    public Optional<Component> shortDescription(CommandCause cause) {
        String description = getDescription();
        return description != null ? Optional.of(text(description)) : Optional.empty();
    }

    @Override
    public Optional<Component> extendedDescription(CommandCause cause) {
        return Optional.empty();
    }

    @Override
    public Component usage(CommandCause cause) {
        String usage = getUsage();
        return usage != null ? text(usage) : text("");
    }

    private String[] argToStrlist(ArgumentReader.Mutable arguments) {
        return Arrays.stream(arguments.input().split(" "))
                .filter(string -> (!string.isEmpty()))
                .toArray(String[]::new);
    }
}
