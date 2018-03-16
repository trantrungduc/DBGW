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
import org.d.sps.DBGW.RETURN

List<Map<String,String>> m1 = DBGW.utility.qry("select to_char(to_date(to_char(sysdate,'mmyyyy'),'mmyyyy'),'yyyy-mm-dd') d1,to_char(add_months(to_date(to_char(sysdate,'mmyyyy'),'mmyyyy'),1),'yyyy-mm-dd') d2 from dual",[],"ccbs");
String xml = DBGW.get("http://10.149.248.11:8081/shrMA.pub?msisdn="+so_tb+"&startDate="+m1.get(0).get("D1")+"&endDate="+m1.get(0).get("D2"),["Authorization":"Basic bWluc2F0Om1pbnNhdA==","Host":"10.212.3.211:8080","User-Agent":"MyVinaphone/4.0/4.0"]);
List<Node> cdr = DBGW.getNodes(xml,"//*[local-name()='cDRMA']");
Map<String,String> hist = new HashMap<String,String>();
DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
List<String> list = Arrays.asList(b_numbers.split(","));
int c1=0;
for (Node c:cdr){
	String tsc=Main.getXmlText(c.asXML(),"//*[local-name()='tSC']");
	if (tsc.equals("0")||tsc.equals("4")){
		String b_sub = Main.getXmlText(c.asXML(),"//*[local-name()='otherPartyNumber']");
		if (list.contains(b_sub)){
			hist.put(b_sub,"");
		}
	}
}
return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",String.valueOf(hist.size()),"tpl","tpl_val","body",body);