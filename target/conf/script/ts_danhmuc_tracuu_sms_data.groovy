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

RESP xml = DBGW.post(["h:Authorization":"Basic bmVvOm5lbyMx","h:SOAPAction":"/axis/services/qos/qosGetDataHighBWRemain","url":"http://10.149.34.91:8899/axis/services/qos","service":"bw","tran_id":trace_id,"so_tb":so_tb,"xmltext":"//*[name() = 'qosGetDataHighBWRemainReturn']"]);
return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result",xml.resp,"tpl","tpl_val","body",body);