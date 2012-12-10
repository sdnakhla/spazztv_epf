/**
 * 
 */
package com.spazztv.epf;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;

import com.spazztv.epf.dao.EPFDbConfig;

/**
 * Logger advice for the EPFImporter object.
 * <p>
 * This logger displays the configuration options under which the job will run.
 * The config file and command line options are logged.
 * 
 * @author Thomas Billingsley
 * 
 */
@Aspect
public class EPFImporterLogger {

	// afterRunImporterJob - job is completed
//	@Before("execution(* myMotherOnThePhone(..)")
//	public void beforeSomeProcedure(JoinPoint joinPoint) {
//
//	}
	
	@Before("call(* com.spazztv.epf.EPFImporter.parseCommandLine(..))")
	public void beforeParseCommandLine(JoinPoint joinPoint) {
		String[] args = (String[])joinPoint.getArgs()[0];
		String msg = "";
		for (int i  = 0 ; i < args.length; i++)  {
			msg += args[i];
			if (i+1 < args.length) {
				msg += " ";
			}
		}
		Logger log = EPFImporter.getLogger();
		log.info("EPFImporter Launched");
		log.info("EPFImporter command line args: {}", msg);
	}

	@Before("call(* com.spazztv.epf.EPFImporter.runImporterJob(..))")
	public void beforeRunningImporterJob(JoinPoint joinPoint) {
		EPFImporter epfImporter = (EPFImporter)joinPoint.getTarget();
		EPFConfig config = epfImporter.getConfig();
		EPFDbConfig dbConfig = epfImporter.getDbConfig();
		
		Logger log = EPFImporter.getLogger();
		log.info("EPF Config Properties:");
		log.info("  Whitelist");
		for (String itm : config.getWhiteList()) {
			log.info("    {}",itm);
		}
		log.info("  Blacklist");
		for (String itm : config.getBlackList()) {
			log.info("    {}",itm);
		}
		log.info("  Directory Paths");
		for (String itm : config.getDirectoryPaths()) {
			log.info("    {}",itm);
		}
		log.info("  Max Threads: {}",config.getMaxThreads());
		log.info("  Allow Extensions: {}",config.isAllowExtensions());
		log.info("  Skip Key Violators: {}",config.isSkipKeyViolators());
		log.info("  Table Prefix: {}",config.getTablePrefix());
		log.info("  Record Separator: {}",config.getRecordSeparator());
		log.info("  Field Separator: {}",config.getFieldSeparator());
		log.info("  Snapshot File: {}",config.getSnapShotFile());
		
		log.info("EPF DB Config Properties:");
		log.info("  DB Writer Class: {}",dbConfig.getDbWriterClass());
		log.info("  DB Data Source Class: {}",dbConfig.getDbDataSourceClass());
		log.info("  DB Default Catalog: {}",dbConfig.getDefaultCatalog());
		log.info("  DB URL: {}",dbConfig.getDbUrl());
		log.info("  DB Username: {}",dbConfig.getUsername());
		log.info("  DB Password: {}",dbConfig.getPassword().replaceAll(".", "*"));
		log.info("  Min Connections: {}",dbConfig.getMinConnections());
		log.info("  Max Connections: {}",dbConfig.getMaxConnections());
	}
	
	@After("call(* com.spazztv.epf.EPFImporter.runImporterJob(..))")
	public void afterRunImporterJob(JoinPoint joinPoint) {
		Logger log = EPFImporter.getLogger();
		log.info("EPFImporter Job Completed");
	}
	
	@AfterThrowing(pointcut = "execution(* com.spazztv.epf.EPFImporter.main(..))", throwing = "error")
	public void afterThrowingMainException(Throwable error) {
		Logger log = EPFImporter.getLogger();
		log.error("EPFImporter Error", error);
	}
	
	@After("call(* com.spazztv.epf.EPFImportManager.loadImportFileList(..)) && this(importManager)")
	public void afterLoadImportFileList(EPFImportManager importManager) {
		Logger log = EPFImporter.getLogger();
		log.info("EPFImporter Files To Be Imported...");
		for (String file : importManager.getFileList()) {
			log.info("  {}", file);
		}
	}
}
