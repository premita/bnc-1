package com.icsynergy.bnc.tasks;

import oracle.iam.scheduler.vo.TaskSupport;

import java.util.HashMap;
import java.util.logging.Logger;

public class CUCMReconTask extends TaskSupport {
	 private final Logger log = Logger.getLogger("com.icsynergy");
	@Override
	
	public void execute(HashMap hashMap) throws Exception {
		log.entering(getClass().getName(),"execute");
		String scriptPath;

		// check required parameters
        if(!hashMap.containsKey("External Script Path")) {
        	log.severe("Requred parameters are missing");
			throw new Exception("Required parameters are missing");
        }

        scriptPath = hashMap.get("External Script Path").toString();
        log.fine("External Script Path : "+scriptPath);

        ProcessBuilder pb = new ProcessBuilder(scriptPath);
        Process p = pb.start();     // Start the process.
        log.fine("External process started... Wait for the process to finish");

        p.waitFor();                // Wait for the process to finish.
        log.fine("Process executed successfully");
	}

	@Override
	public HashMap getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttributes() {
		// TODO Auto-generated method stub
	}
}
