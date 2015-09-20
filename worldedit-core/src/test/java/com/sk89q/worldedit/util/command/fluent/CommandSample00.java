/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * LocalRegistrar
 * Copyright (C) 2011 Charles Hymes <http://www.hymerfania.com>
 */
package com.sk89q.worldedit.util.command.fluent;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.WorldEdit;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.fail;

/**
 *
 * @author chymes
 */
public class CommandSample00 {

    private static final Logger LOG = Logger.getLogger(CommandSample00.class.getName());
    public static final String test00_ALIASES[] = {"/commandTest00", "/cmdTst00"};
    public static final List<String> test00_ALIASES_LIST = Arrays.asList(test00_ALIASES);
    public static final String COMMAND_NAME_00 = "commandTest00";
    public static final List<Method> COMMAND_METHODS;
    public static Method commandMethod00;
    private final WorldEdit worldEdit;

    static {
        try {
            commandMethod00 = CommandSample00.class.getMethod(COMMAND_NAME_00, (Class<?>[]) null);
        } catch (NoSuchMethodException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            fail("Refelection error obtaining commandMethod00 instance");            
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            fail("Refelection error obtaining commandMethod00 instance");            
        }
        COMMAND_METHODS = Arrays.asList(new Method[]{commandMethod00});
    }

    public CommandSample00() {
        this(null);
    }

    public CommandSample00(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    @Command(aliases = {"/commandTest00", "/cmdTst00"},
            usage = "/commandTest00 [length] l || length l [width w] [slope HORIZONTAL|INCLINE|DECLINE]",
            desc = "Dummy command to test LocalRegistration API 00",
            help = "Does nothing 00",
            min = 1,
            max = 4)
    @CommandPermissions(value = {"worldedit"})
    @Logging(value = Logging.LogMode.ALL)
    public void commandTest00() {
        System.err.println("commandTest00");
    }
}
