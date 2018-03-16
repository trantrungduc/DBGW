package org.d.sps;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import groovy.lang.Binding;

public class GroovyJob implements Job {
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        String jobName = context.getJobDetail().getJobDataMap().getString("job");
        
        if (DBGW.isJobRunning(context, jobName)){
        	Logger.getLogger("jobs").info("\nJob is running!");
        }else{
        	
        	Binding bind = new Binding();
			bind.setVariable("props", DBGW.props);
			bind.setVariable("utility",DBGW.utility);
			bind.setVariable("global",DBGW.global);
			Iterator<?> k = DBGW.props.getKeys(jobName);
			while (k.hasNext()){
				String key = (String)k.next();
				bind.setVariable(key.replaceAll(jobName+".",""),DBGW.props.getString(key));
			}
			try {
				Logger.getLogger("jobs").info("\nJob "+jobName+" return: "+DBGW.shell(bind,"/conf/job/"+DBGW.props.getString(jobName+".script")));
			} catch (CompilationFailedException e) {
				e.printStackTrace();
			}
        }
    }
    public static void main(String[] args){
    	System.out.println(Math.round(999999/10000));
    }
}
