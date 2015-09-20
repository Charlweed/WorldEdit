/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sk89q.worldedit.util.command.fluent;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author chymes
 */
public class LocalRegistrarTest {

    public LocalRegistrarTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of commandAliases method, of class LocalRegistrar.
     */
    @Test
    public void testCommandAliases() {
        System.out.println("commandAliases");
        Method commandMethod = CommandSample00.commandMethod00;
        List<String> expResult = CommandSample00.test00_ALIASES_LIST;
        List<String> result = LocalRegistrar.commandAliases(commandMethod);
        assertEquals(expResult, result);
    }

    /**
     * Test of hasWorldEditConstructor method, of class LocalRegistrar.
     */
    @Test
    public void testHasWorldEditConstructor() {
        System.out.println("hasWorldEditConstructor");
        Class<CommandSample00> clazz = CommandSample00.class;
        boolean expResult = true;
        boolean result = LocalRegistrar.hasWorldEditConstructor(clazz);
        assertEquals(expResult, result);
    }

    /**
     * Test of commandMethods method, of class LocalRegistrar.
     */
    @Test
    public void testCommandMethods() {
        System.out.println("commandMethods");
        Class<CommandSample00> someClass = CommandSample00.class;
        List<Method> expResult = CommandSample00.COMMAND_METHODS;
        List<Method> result = LocalRegistrar.commandMethods(someClass);
        assertEquals(expResult, result);
    }

//    /**
//     * Test of registerJaredCommands method, of class LocalRegistrar.
//     */
//    @Test
//    public void testRegisterJaredCommands() {
//        System.out.println("registerJaredCommands");
//        PlatformManager platformManager = null;
//        DispatcherNode dispatcherNode = null;
//        WorldEdit worldEdit = null;
//        DispatcherNode expResult = null;
//        DispatcherNode result = LocalRegistrar.registerJaredCommands(platformManager, dispatcherNode, worldEdit);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }


}
