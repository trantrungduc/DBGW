package org.d.sps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.app.Velocity;
import org.d.sps.DBGW.RETURN;
import org.dom4j.Element;
import org.dom4j.Node;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import groovy.lang.Binding;

public class HttpProcessThread implements Runnable {
	private Request req;
	private Response resp;
	public HttpProcessThread(Request Req, Response Resp){
		this.req = Req;
		this.resp = Resp;
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		if (req.getRequestURI().equals("/status") && req.getMethod().equals(Method.GET)){
			try {
				String data = FileUtils.readFileToString(new File(System.getProperty("user.dir")+"/conf/html/"+req.getParameter("tpl")+".html"),"UTF-8");
				Map<String,Object> params = new HashMap<String,Object>();
				Velocity.setProperty("input.encoding", "UTF-8");
				Velocity.setProperty("output.encoding", "UTF-8");
				params.put("req",req);
				params.put("resp",resp);
				params.put("global",DBGW.global);
				params.put("props",DBGW.props);
				
				resp.setContentType("text/html;charset=utf-8");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write(DBGW.eval1(data,params));
			} catch (Throwable e) {
				try {
					resp.getWriter().write(e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}finally{
				resp.resume();
			}
		}else if ( req.getRequestURI().equals("/") && req.getQueryString().equals("wsdl") && req.getMethod().equals(Method.GET)){
			try {
				String data = FileUtils.readFileToString(new File(System.getProperty("user.dir")+"/conf/html/PPWebSvService.wsdl"),"UTF-8");
				Map<String,Object> params = new HashMap<String,Object>();
				Velocity.setProperty("input.encoding", "UTF-8");
				Velocity.setProperty("output.encoding", "UTF-8");
				params.put("req",req);
				params.put("resp",resp);
				params.put("global",DBGW.global);
				params.put("props",DBGW.props);
				params.put("utility",DBGW.utility);
				
				resp.setContentType("text/xml;charset=utf-8");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write(DBGW.eval1(data,params));
			} catch (Throwable e) {
				try {
					resp.getWriter().write(e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}finally{
				resp.resume();
			}
		}else if (req.getMethod().equals(Method.POST)){
			String trace_id=String.valueOf(System.nanoTime());
			String ip=DBGW.getIP(req);
			RETURN ret = null;
			try {
				Return: {
					resp.setContentType("application/soap+xml;charset=UTF-8");
		        	String body = IOUtils.toString(req.getInputStream());

		        	
		        	List<Node> Header = DBGW.getNodes(body,"//*[local-name()='PPHeader']");
		        	List<Node> Body = DBGW.getNodes(body,"//*[local-name()='PPBody']");
		        	if (Header.size()==0 || Body.size()!=1){
		        		ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_req"),"tpl","invalid_req","body",body,"ip",ip);
		        		break Return;
		        	}
		        	String user = DBGW.getXmlText(body,"//*[local-name()='PPHeader']/UserID");
		        	String pass = DBGW.getXmlText(body,"//*[local-name()='PPHeader']/Password");
		        	
		        	if (user==null || pass==null){
		        		ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_req"),"tpl","invalid_req","body",body,"ip",ip);
		        		break Return;
		        	}
		        	Element eBody = (Element)Body.get(0);
		        	if (eBody.selectNodes("*").size()!=1){
		        		ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_req"),"tpl","invalid_req","body",body,"ip",ip);
		        		break Return;
		        	}
		        	if (DBGW.props.getString("user."+user)==null || !DBGW.dec(DBGW.props.getString("user."+user)).equals(pass)){
		        		ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_auth"),"tpl","invalid_auth","body",body,"ip",ip);
		        		break Return;
		        	}
		        	if (DBGW.props.getString("user."+user+".accept_ip")!=null && !DBGW.props.getString("user."+user+".accept_ip").contains(ip)){
		        		ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_ip"),"tpl","invalid_auth","body",body,"ip",ip);
		        		break Return;
		        	}
		        	eBody = (Element)eBody.selectNodes("*").get(0);
		        	String service = eBody.getName();
		        	String serviceType = DBGW.props.getString("sql."+service+".type");
		        	if (serviceType==null){
		        		serviceType="COMPLEX";
		        	}
		        	if (DBGW.props.getString("user."+user+".validate_script")!=null){
		        		Binding bind = new Binding();
		        		bind.setVariable("service", service);
		        		bind.setVariable("serviceType", serviceType);
		        		bind.setVariable("global",DBGW.global);
		        		bind.setVariable("props",ConfigurationConverter.getMap(DBGW.props));
		        		bind.setVariable("request", req);
		        		bind.setVariable("trace_id",trace_id);
		        		bind.setVariable("body",body);
		        		Map<String,String> params = new HashMap<String,String>();
		        		for (Object n:eBody.selectNodes("*")){
		        			Element e = (Element)n;
		        			params.put(e.getName(),e.getText());
		        			bind.setVariable(e.getName(),e.getText());
		        		}
		        		bind.setVariable("ip",ip);
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|complex|"+System.currentTimeMillis()+"|"+params);
		        		
		            	Object call = DBGW.shell(bind, "/conf/script/"+DBGW.props.getString("user."+user+".validate_script")+".groovy");
		        		if (call!=null && call instanceof Map){
		        			Map<String,Object> call1 =(Map<String,Object>)call;
		        			if (!(Boolean)call1.get("validate")){
		        				ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_check"),"tpl","invalid_auth","body",body,"ip",ip,"valid",call);
		        				break Return;
		        			}
		        		}else{
		        			ret = new RETURN("trace_id",trace_id,"result",DBGW.props.getString("msg.invalid_check"),"tpl","invalid_auth","body",body,"ip",ip,"valid",call);
	        				break Return;
		        		}
		        	}
		        	
		        	if (serviceType!=null && serviceType.equals("VALUE")){
		        		String sql = DBGW.props.getString("sql."+service);
		        		Map<String,String> params = new HashMap<String,String>();
		        		for (Object n:eBody.selectNodes("*")){
		        			Element e = (Element)n;
		        			params.put(e.getName(),e.getText());
		        		}
		        		params.put("ip",ip);
		        		sql = DBGW.eval(sql,params);
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|value|"+System.currentTimeMillis()+"|"+sql);
		        		String val = DBGW.utility.val(sql,null,DBGW.props.getString("sql."+service+".src"));
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|value|result|"+System.currentTimeMillis()+"|"+val);
		        		ret = new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",StringEscapeUtils.escapeXml(val),"tpl","tpl_val","body",body,"ip",ip);
		        	}else if (serviceType!=null && serviceType.equals("UPDATE")){
		        		String sql = DBGW.props.getString("sql."+service);
		        		Map<String,String> params = new HashMap<String,String>();
		        		for (Object n:eBody.selectNodes("*")){
		        			Element e = (Element)n;
		        			params.put(e.getName(),e.getText());
		        		}
		        		params.put("ip",ip);
		        		sql = DBGW.eval(sql,params);
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|update|"+System.currentTimeMillis()+"|"+sql);
		        		String val = DBGW.utility.update(sql,null,DBGW.props.getString("sql."+service+".src"));
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|update|result|"+System.currentTimeMillis()+"|"+val);
		        		ret = new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",val,"tpl","tpl_val","body",body,"ip",ip);
		        	}else if (serviceType!=null && (serviceType.equals("QUERY")||serviceType.equals("CURSOR"))){
		        		String sql = DBGW.props.getString("sql."+service);
		        		Map<String,String> params = new HashMap<String,String>();
		        		for (Object n:eBody.selectNodes("*")){
		        			Element e = (Element)n;
		        			params.put(e.getName(),e.getText());
		        		}
		        		params.put("ip",ip);
		        		sql = DBGW.eval(sql,params);
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|rowset|"+System.currentTimeMillis()+"|"+sql);
		        		List<Map<String,String>> res = null;
		        		if (serviceType.equals("CURSOR")){
		        			res = DBGW.utility.rf(sql,null,DBGW.props.getString("sql."+service+".src"));
		        		}else{
		        			res = DBGW.utility.qry(sql,null,DBGW.props.getString("sql."+service+".src"));
		        		}
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|rowset|result|"+System.currentTimeMillis()+"|"+res.toString().replace("\n","").replace("\r",""));
		        		ret = new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",res,"tpl","tpl_rowset","body",body,"ip",ip);
		        	}else{
		        		Binding bind = new Binding();
		        		bind.setVariable("service", service);
		        		bind.setVariable("serviceType", serviceType);
		        		bind.setVariable("global",DBGW.global);
		        		bind.setVariable("props",ConfigurationConverter.getMap(DBGW.props));
		        		bind.setVariable("request", req);
		        		bind.setVariable("trace_id",trace_id);
		        		bind.setVariable("body",body);
		        		Map<String,String> params = new HashMap<String,String>();
		        		for (Object n:eBody.selectNodes("*")){
		        			Element e = (Element)n;
		        			params.put(e.getName(),e.getText());
		        			bind.setVariable(e.getName(),e.getText());
		        		}
		        		bind.setVariable("ip",ip);
		        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|complex|"+System.currentTimeMillis()+"|"+params);
		        		
		            	Object call = DBGW.shell(bind, "/conf/script/"+service+".groovy");
		            	Logger.getLogger("sql").info(trace_id+"|"+ip+"|complex|result|"+System.currentTimeMillis()+"|"+call);
						
						if (call instanceof RETURN){
							ret = (RETURN)call;
		        		}else{
		        			ret = new RETURN("serviceType","COMPLEX","trace_id",trace_id,"result",call,"body",body,"tpl","exception");
		        		}
		        	}
				}
	        	resp.getWriter().write(ret.toString());
	        } catch (Exception e) {
	        	try {
	        		Logger.getLogger("sql").info(trace_id+"|"+ip+"|exception|"+System.currentTimeMillis()+"|"+e.getMessage());
	        		ret = new RETURN("serviceType","COMPLEX","trace_id",trace_id,"result",e.getMessage(),"body",e.getMessage(),"tpl","exception");
		        	resp.getWriter().write(ret.toString());
				} catch (IOException e1) {}
	        } finally {
	        	resp.resume();
	        }
		}else{
			resp.resume();
		}
    }
}
