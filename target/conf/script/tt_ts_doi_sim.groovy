import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.d.sps.*;
import org.d.sps.DBGW.RETURN;
import org.d.sps.DBGW.RESP;

List<Map<String,String>> siminfo1 = utility.qry("select * from subadmin.sim_data where imsi=? and ac_state in ('0')",["45202"+sim.substring(9,19)],"vnp1");
List<Map<String,String>> so_msin = utility.qry("select so_msin from subadmin.thue_bao where so_tb=?",[so_tb],"vnp1");
DBGW.plog.info(tran_id+"|DOI_SIM|siminfo result : "+siminfo1);
if (siminfo1.size()==0){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Sim đã dùng hoặc không tồn tại","tpl","tpl_val","body",body);
}
if (so_msin.size()==0){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Thuê bao không tồn tại","tpl","tpl_val","body",body);
}
String imsi_old="45202"+so_msin.get(0).get("SO_MSIN");
Map<String,String> siminfo=siminfo1.get(0);
String imsi="45202"+sim.substring(9,19),secver=(siminfo.get("ALGORITHM")==null || siminfo.get("ALGORITHM").equals(""))?"0":"2",ki=siminfo.get("A4KI"),simtype=siminfo.get("SIMTYPE");
DBGW.plog.info(tran_id+"|DOI_SIM|call sps: "+props.getString("sps_soap_url"));
String keyid = "1";
if ("129".equals(simtype)) {secver = "3";keyid = "1";} else if ("130".equals(simtype)) {secver = "3";keyid = "2";} else if ("65".equals(simtype)) {secver = "3";keyid = "2";}

RESP xml = DBGW.post(["url":props.getString("sps_soap_url"),"service":"sps_ac","tran_id":trace_id,"ki":ki,"secver":secver,"imsi":imsi,"note":"Đổi SIM DHSXKD vung HPG ["+trace_id+"]","loai_tb":product,"msisdn":so_tb,"province":ma_tinh,"keyid":keyid,"xmltext":"//*[name() = 'StatusMessage']"]);
if (!xml.xmltext.startsWith("00000|")){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Lỗi Khởi tạo AC"+xml.xmltext,"tpl","tpl_val","body",body);
}
RESP xml = DBGW.post(["url":props.getString("sps_soap_url"),"service":"doi_sim","tran_id":trace_id,"ki":ki,"secver":secver,"imsi_old":imsi_old,"imsi":imsi,"note":"Đổi SIM DHSXKD vung HPG ["+trace_id+"]","loai_tb":product,"msisdn":so_tb,"province":ma_tinh,"keyid":keyid,"xmltext":"//*[name() = 'StatusMessage']"]);
 return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",xml.resp,"tpl","tpl_val","body",body);