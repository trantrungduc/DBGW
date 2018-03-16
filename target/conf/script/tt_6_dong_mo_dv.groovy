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

RESP xml = DBGW.post(["url":DBGW.props.getString("sps_soap_url"),"service":"dong_mo_dich_vu","tran_id":trace_id,"product":product,"msisdn":so_tb,"xmltext":"//*[name() = 'StatusMessage']"]);
return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",xml.resp,"tpl","tpl_val","body",body);