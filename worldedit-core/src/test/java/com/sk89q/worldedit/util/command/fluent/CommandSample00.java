/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sk89q.worldedit.util.command.fluent;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.WorldEdit;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
            commandMethod00 = CommandSample00.class.getMethod(COMMAND_NAME_00, (Class<?>) null);
        } catch (NoSuchMethodException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail("Refelection error obtaining commandMethod00 instance");
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, null, ex);
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
