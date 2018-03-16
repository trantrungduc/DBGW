import org.d.sps.DBGW.RETURN
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.sanselan.ImageInfo
import org.apache.sanselan.ImageReadException
import org.apache.sanselan.Sanselan
import org.d.sps.*

byte[] imgB1 = Base64.decodeBase64(img1.getBytes());
byte[] imgB2 = Base64.decodeBase64(img2.getBytes());
byte[] imgB3 = null;
byte[] imgB4 = Base64.decodeBase64(img4.getBytes());
if (!img3.equals("")){
	imgB3=Base64.decodeBase64(img3.getBytes());
}
ImageInfo imgI1 = Sanselan.getImageInfo(imgB1);
ImageInfo imgI2 = Sanselan.getImageInfo(imgB2);
ImageInfo imgI3 = imgB3==null?null:Sanselan.getImageInfo(imgB3);
ImageInfo imgI4 = Sanselan.getImageInfo(imgB4);
if (imgI1.getHeight()<200 || imgI2.getHeight()<200 || imgI1.getWidth()<300 || imgI2.getWidth()<300){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}
if (imgI1.getHeight()>2000 || imgI2.getHeight()>2000 || imgI1.getWidth()>3000 || imgI2.getWidth()>3000){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}

if (imgI4.getHeight()<200 || imgI4.getWidth()<300){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}
if (imgI4.getHeight()>2000 || imgI4.getWidth()>3000){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}

String strFolder = DBGW.fdate("MMyyyy/dd");
String f1 = so_tb+"_1_"+DBGW.fdate("yyyyMMdd_HHmmss") + ".png";
String f2 = so_tb+"_2_"+DBGW.fdate("yyyyMMdd_HHmmss") + ".png";
String f3 = imgB3==null?"":so_tb+"_3_"+DBGW.fdate("yyyyMMdd_HHmmss") + ".png";
String f4 = so_tb+"_4_"+DBGW.fdate("yyyyMMdd_HHmmss") + ".png";

FTPClient ftpClient = new FTPClient();

ftpClient.connect("10.156.3.30",21);
ftpClient.login("ftp_vnp", "ftp\$123");

ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

boolean done1 = ftpClient.storeFile(strFolder+"/"+f1,new ByteArrayInputStream(imgB1));
boolean done2 = ftpClient.storeFile(strFolder+"/"+f2,new ByteArrayInputStream(imgB2));
boolean done3 = imgB3==null?true:ftpClient.storeFile(strFolder+"/"+f3,new ByteArrayInputStream(imgB3));
boolean done4 = ftpClient.storeFile(strFolder+"/"+f4,new ByteArrayInputStream(imgB4));
if (!done1 || !done2 || !done3 || !done4){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Upload khong thanh cong","tpl","exception","body",body);
}
ftpClient.logout();
ftpClient.disconnect();

String iu1 = "0";
create_date1=(create_date1==null || create_date1.equals("null"))?"":create_date1;
create_date2=(create_date2==null || create_date2.equals("null"))?"":create_date2;
create_date4=(create_date4==null || create_date4.equals("null"))?"":create_date4;
if (imgB3==null){
	iu1=utility.val("begin ? := prepaid.merge_image_sub(?,'',?,'1',?,?); end;",[so_tb,f1+"|"+create_date1+","+f2+"|"+create_date2+","+f4+"|"+create_date4,STK.f84(msisdn),strFolder],"vnp1");
}else{
	iu1=utility.val("begin ? := prepaid.merge_image_sub(?,'',?,'1',?,?); end;",[so_tb,f1+"|"+create_date1+","+f2+"|"+create_date2+","+f3+"|"+create_date3+","+f4+"|"+create_date4,STK.f84(msisdn),strFolder],"vnp1");
}
if ( !(iu1.startsWith("0")) ){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Cap nhat DB khong thanh cong","tpl","exception","body",body);
}

return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Upload anh thanh cong","tpl","tpl_val","body",body);