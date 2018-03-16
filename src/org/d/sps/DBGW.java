package org.d.sps;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.jasypt.util.text.BasicTextEncryptor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import com.google.gson.Gson;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class DBGW {

	public static Map<String,Object> global = Collections.synchronizedMap(new HashMap<String,Object>());
	public static PropertiesConfiguration props = null;
	public static Logger plog = Logger.getLogger("process"),elog = Logger.getLogger("error"),rlog = Logger.getLogger("req");
	public static Logger dequeueLog = Logger.getLogger("dequeue");
    public static SqlUtility utility = null;
    public static Scheduler scheduler;
    public static Map<String,String> runningJob = new ConcurrentHashMap<String,String>();
    static BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
    static Gson gson = new Gson();
    
    public static String fdate(String format){
    	DateFormat df = new SimpleDateFormat(format);
    	return df.format(new Date(System.currentTimeMillis()));
    }
    public static String fdate(String format, Date date){
    	DateFormat df = new SimpleDateFormat(format);
    	return df.format(date);
    }
    public static String get(String url,Map<String,String> params){
		try {

		   HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		   con.setRequestMethod("GET");
		   for (Map.Entry<String,String> e:params.entrySet()){
			   con.setRequestProperty(e.getKey(),e.getValue());
		   }
		   
		   BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		   String inputLine;
		   StringBuffer response = new StringBuffer();

		   while ((inputLine = in.readLine()) != null) {
			   response.append(inputLine);
		   }
		   in.close();
		   con.disconnect();
		   return response.toString();
		} catch (IOException e) {
		   return e.getMessage();
		}
	}
		
	public static void main(String[] args) throws ConfigurationException, IOException, SchedulerException{
		System.out.println("Load configuration api.properties!");
    	props = new PropertiesConfiguration();
    	props.setDelimiterParsingDisabled(true);
    	props.setEncoding("UTF-8");
        props.load("conf/api.properties");
        props.setReloadingStrategy(new FileChangedReloadingStrategy());
        utility = new SqlUtility(props);
        textEncryptor.setPassword("vnptnet_modular_encrypt");
        
        HttpServer httpServer = new HttpServer();
		NetworkListener networkListener = new NetworkListener("grizzly-listener","0.0.0.0",props.getInt("port") );
		ThreadPoolConfig threadPoolConfig = ThreadPoolConfig
		        .defaultConfig()
		        .setCorePoolSize(props.getInt("pool_size"))
		        .setMaxPoolSize(props.getInt("pool_size"));
		networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
		httpServer.addListener(networkListener);
		
		httpServer.getServerConfiguration().setDefaultQueryEncoding(Charset.forName("UTF-8"));
		httpServer.getServerConfiguration().addHttpHandler(new HttpHandler() {
		    final ExecutorService complexAppExecutorService =
			        GrizzlyExecutorService.createInstance(
			            ThreadPoolConfig.defaultConfig()
			            .copy()
			            .setCorePoolSize(props.getInt("pool_size"))
			            .setMaxPoolSize(props.getInt("pool_size")));

				@Override
				public void service(final Request req, final Response resp) throws Exception {
					// TODO Auto-generated method stub
					resp.suspend();
			        complexAppExecutorService.execute(new HttpProcessThread(req, resp));
				}
				
				@Override
			    public void destroy() {
			        super.destroy();
			    }
			}, "/*");
		httpServer.start();
		
        System.out.println("Load jobs!");
        scheduler = new StdSchedulerFactory().getScheduler();
    	scheduler.start();
    	
    	new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				String[] jobs = props.getStringArray("job.name");
				List<String> removes = new ArrayList<String>();
				for (String jobName: runningJob.keySet()){
					try {
						TriggerKey tk=TriggerKey.triggerKey(jobName,jobName);
						JobKey jk = JobKey.jobKey(jobName, jobName);
						String k = props.getString(jobName+".schedule")+"|"+props.getString(jobName+".script");
						
						if (!Arrays.asList(jobs).contains(jobName) || !k.equals(runningJob.get(jobName))){
							System.out.println("Stop job: "+jobName);
							scheduler.unscheduleJob(tk);
							scheduler.interrupt(jk);
							scheduler.deleteJob(jk);
							removes.add(jobName);
						}
					} catch (SchedulerException e) {
						System.out.println("Stop job: "+e.getMessage());
					}
				}
				for (String remove:removes){
					runningJob.remove(remove);
				}
				for (String jobName: jobs){
		    		if (!runningJob.containsKey(jobName)){
			        	System.out.println("Start job: "+jobName);
			        	JobDetail job = JobBuilder.newJob(GroovyJob.class).usingJobData("job", jobName).usingJobData("script", props.getString(jobName+".script")).usingJobData("scheduler", props.getString(jobName+".schedule")).withIdentity(jobName, jobName).build();
			        	Trigger trigger = TriggerBuilder.newTrigger()
			        			.withIdentity(jobName,jobName)
			        			.withSchedule(CronScheduleBuilder.cronSchedule(props.getString(jobName+".schedule")))
			        			.build();
			        	try {
							scheduler.scheduleJob(job, trigger);
							runningJob.put(jobName,props.getString(jobName+".schedule")+"|"+props.getString(jobName+".script"));
				        	
						} catch (SchedulerException e) {
							e.printStackTrace();
						}
		    		}
		        }
    		}
		},0,10000);
	}
	
	public static RESP post(Object... exts){
		Map<String,Object> m = new HashMap<String,Object>();
		for (int i = 0; i < exts.length; i += 2) {
			m.put((String)exts[i],exts[i + 1]); 
    	}
		return post(m);
	}
	public static RESP post(Map<String,Object> params){
		String data="";
		Map<String,String> task_log = new HashMap<String,String>();
		task_log.put("start_time",String.valueOf(System.currentTimeMillis()));
		try{
			URL u = new URL((String)params.get("url"));
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setReadTimeout(10000);
			conn.setDoOutput(true);
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty( "Content-Type", "text/xml;charset=UTF-8" );
	        if (params.containsKey("Content-Type")){
	        	conn.setRequestProperty( "Content-Type", (String)params.get("Content-Type") );
	        }
	        
	        if (params.containsKey("SOAPAction")){
	        	conn.setRequestProperty( "SOAPAction", (String)params.get("SOAPAction") );
	        }else{
	        	conn.setRequestProperty( "SOAPAction", "" );
	        }
	        
	        for (Entry<String, Object> i:params.entrySet()){
	        	if (i.getKey().startsWith("h:")){
	        		conn.setRequestProperty(i.getKey().substring(2),(String)i.getValue());
	        	}
	        }
	        
	        data = FileUtils.readFileToString(new File(System.getProperty("user.dir")+"/conf/req/"+params.get("service")+".xml"),"UTF-8");
	        data = eval1(data,params);
	        task_log.put("request",data);
	        task_log.put("service", (String) params.get("service"));
	        
	        conn.setRequestProperty( "Content-Length", String.valueOf(data.length()));
	        OutputStream os = conn.getOutputStream();
	        os.write(data.getBytes("UTF-8"));
	        os.flush();
	        os.close();
	        
	        StringBuffer out = new StringBuffer();
	        task_log.put("response_code", String.valueOf(conn.getResponseCode()));
	        if (conn.getResponseCode()==200){
		        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
		        String inputLine;
		        while ((inputLine = in.readLine()) != null) {
		        	out.append(inputLine+"\n");
		        }
		        in.close();
	        }else if (conn.getErrorStream()!=null){
	        	BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream(),"UTF-8"));
		        String inputLine;
		        while ((inputLine = in.readLine()) != null) {
		        	out.append(inputLine+"\n");
		        }
		        in.close();
	        }
	        String r_ = out.toString();
	        task_log.put("response", r_);
	        RESP re = new RESP(true,data,r_,String.valueOf(conn.getResponseCode()),task_log);
	        if (params.containsKey("xmltext")){
	        	re.xmltext=getXmlText(r_,(String) params.get("xmltext"));
	        	re.log.put("xmltext", re.xmltext);
	        }
	        if (params.containsKey("xmlattr")){
	        	re.xmlattr=getXmlText(r_,(String) params.get("xmlattr"));
	        	re.log.put("xmlattr", re.xmlattr);
	        }
	        conn.disconnect();
	        re.log.put("end_time", String.valueOf(System.currentTimeMillis()));
	        return re;
		}catch(Exception e){
			task_log.put("end_time", String.valueOf(System.currentTimeMillis()));
			task_log.put("code",e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			task_log.put("response",sw.toString());
			return new RESP(false,data,e.getMessage(),"-1",task_log);
		}
	}
	public static Object shell(Binding binding, String script){
		GroovyShell shell = new GroovyShell(binding);
		try{
			File s = new File(System.getProperty("user.dir")+script);
			if (s.exists()){
				return shell.evaluate(s);
			}else{
				Map<String,String> p = new HashMap<String,String>();
				for (Object e:binding.getVariables().keySet()){
					Object v = binding.getVariable((String)e);
					if (v!=null){
						p.put((String)e,v.toString());
					}
				}
				return props.getString("msg.005");
			}
		}catch(Exception e){
			e.printStackTrace();
			return e.getMessage();
		}
	}
	public static String eval(String input, Map<String, String> params){
		VelocityContext vcontext = new VelocityContext(params);
		vcontext.put("global",global);
		vcontext.put("props",props);
		vcontext.put("spgw",new DBGW());
		StringWriter sw = new StringWriter();
		Velocity.evaluate(vcontext, sw, "velocity",input);
		return sw.toString();
	}
	public static String eval1(String input, Map<String, Object> params){
		VelocityContext vcontext = new VelocityContext(params);
		vcontext.put("global",global);
		vcontext.put("props",props);
		vcontext.put("spgw",new DBGW());
		StringWriter sw = new StringWriter();
		Velocity.evaluate(vcontext, sw, "velocity",input);
		return sw.toString();
	}
	public static class RESP{
		boolean ok=false;
		String req=null,resp=null,code=null,xmltext=null,xmlattr=null;
		Map<String,String> log = new HashMap<String,String>();
		public RESP(boolean OK, String REQ, String RESP, String CODE,Map<String,String> LOG){
			ok=OK;
			req=REQ;
			resp=RESP;
			code=CODE;
			log = LOG;
		}
	}
	public static class RETURN{
		public Map<String,Object> params = new HashMap<String,Object>();
		public RETURN(Map<String,Object> m){
			params = m;
		}
		public RETURN(Object... exts){
			for (int i = 0; i < exts.length; i += 2) {
				params.put((String)exts[i],exts[i + 1]); 
	    	}
		}
		@Override
		public String toString(){
			try {
				String data = FileUtils.readFileToString(new File(System.getProperty("user.dir")+"/conf/resp/"+params.get("tpl")+".xml"),"UTF-8");
				data = eval1(data,params);
				return data;
			} catch (IOException e) {
				return e.getMessage();
			}
		}
	}
	public static String enc(String text){
		return (textEncryptor.encrypt(text));
	}
	public static String dec(String pass){
		return textEncryptor.decrypt(pass);
	}
	public static boolean isJobRunning(JobExecutionContext ctx, String jobName){
        List<JobExecutionContext> currentJobs;
		try {
			currentJobs = ctx.getScheduler().getCurrentlyExecutingJobs();
			for (JobExecutionContext jobCtx : currentJobs) {
				String thisJobName = jobCtx.getJobDetail().getKey().getName();
	            String thisGroupName = jobCtx.getJobDetail().getKey().getGroup();
	            if (jobName.equalsIgnoreCase(thisJobName) && jobName.equalsIgnoreCase(thisGroupName) && !jobCtx.getFireTime().equals(ctx.getFireTime())) {
	                return true;
	            }
	        }
	        return false;
		} catch (SchedulerException e) {
			return false;
		}
    }
	@SuppressWarnings("unchecked")
	public static List<Node> getNodes(String xml,String path){
		try{
			SAXReader reader = new SAXReader();
	        Document document = reader.read(new StringReader(xml));
	        return document.selectNodes(path);
		}catch(Exception e){
			return null;
		}
	}
	public static String getXmlAttr(String xml, String path) {
		try{
	        SAXReader reader = new SAXReader();
	        Document document = reader.read(new StringReader(xml));
	        return ((Attribute)document.selectSingleNode(path)).getText();
		}catch(Exception e){
			return null;
		}
    }
	public static String getXmlText(String xml, String path) {
    	try{
	        SAXReader reader = new SAXReader();
	        Document document = reader.read(new StringReader(xml));
	        return document.selectSingleNode(path).getText();
    	}catch(Exception e){
			return null;
		}
    }
	
	public static String getIP(Request req){
		String ip = req.getHeader("X-Forwarded-For");  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("Proxy-Client-IP");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("WL-Proxy-Client-IP");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_X_FORWARDED_FOR");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_X_FORWARDED");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_X_CLUSTER_CLIENT_IP");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_CLIENT_IP");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_FORWARDED_FOR");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_FORWARDED");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("HTTP_VIA");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getHeader("REMOTE_ADDR");  
	    }  
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = req.getRemoteAddr();  
	    }  
	    return ip;  
	}
	public static Map<String, String> reqToMap(Request req){
		Map<String,String> params = new HashMap<String,String>();
		
		Set<String> e = req.getAttributeNames();
		for (String k:e){
    		if (req.getAttribute(k)!=null){
    			params.put(k,req.getAttribute(k).toString());
    		}else{
    			params.put(k,"");
    		}
    	}
    	return params;
	}
}
