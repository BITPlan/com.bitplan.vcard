/**
 * Copyright (c) 2019 BITPlan GmbH
 *
 * http://www.bitplan.com
 *
 * This file is part of the Opensource project at:
 * https://github.com/BITPlan/com.bitplan.vcard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitplan.vcard;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Main class of vcard backup and synchronisation
 * @author wf
 *
 */
public class Main {
	@Option(aliases = { "--debug" }, name = "-d", usage = "set debug mode")
	boolean debug= false;
	
	@Option( aliases = { "--user" }, required = true, name = "-u", usage = "set user")
	String user;
	
  @Option( aliases = { "--backup" }, required = false, name = "-b", usage = "full backup")
	boolean doBackup;
  
  @Option( aliases = { "--sync" }, required = false, name = "-s", usage = "synchronize")
  boolean doSync;

  
	public static boolean testmode=false;
	
	/**
	 * parse Command line arguments
	 * 
	 * @param args
	 */
	public void parseArguments(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (Exception e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			if (!testmode)
				System.exit(1);
		} 
	}
	
	/**
	 * work 
	 * @throws Exception 
	 */
	public void work() throws Exception {
		
		CardDavStore cs = CardDavStore.getCardDavStore(user);
		if (doBackup) {
		  System.out.println("Backing up CardDavStore for user="+user);
		  cs.backup();
		}
		if (doSync) {
      System.out.println("Synchronizing CardDavStore for user="+user);
      cs.sync();		  
		}
		
	}
	
	/**
	 * entry point
	 * @param args
	 */
	public static void main(String []args) {
		Main main=new Main();
		main.parseArguments(args);
		try {
			main.work();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
